import { useEffect, useState, useRef } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { MapContainer, TileLayer, Marker, Popup, useMap, Polyline } from 'react-leaflet';
import L from 'leaflet';
import { pedidoService } from '../../services/pedidos';
import { domiciliarioService } from '../../services/domiciliarios';
import { trackingService } from '../../services/tracking';
import { useAuth } from '../../context/AuthContext';
import { useRestaurante } from '../../context/RestauranteContext';
import { useToast } from '../../components/Toast';
import { EstadoBadge } from '../../components/EstadoBadge';
import { EstadoPedido, type PedidoResponse } from '../../types';

import markerIcon2x from 'leaflet/dist/images/marker-icon-2x.png';
import markerIcon from 'leaflet/dist/images/marker-icon.png';
import markerShadow from 'leaflet/dist/images/marker-shadow.png';

delete (L.Icon.Default.prototype as Record<string, unknown>)._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: markerIcon2x,
  iconUrl: markerIcon,
  shadowUrl: markerShadow,
});

// Icono azul para el domiciliario
const myIcon = new L.DivIcon({
  html: '<div style="font-size:28px;text-align:center">🛵</div>',
  iconSize: [32, 32],
  iconAnchor: [16, 16],
  className: '',
});

// Icono rojo para destino de entrega
const destinoIcon = new L.DivIcon({
  html: '<div style="font-size:24px;text-align:center">📍</div>',
  iconSize: [28, 28],
  iconAnchor: [14, 28],
  className: '',
});

// Icono verde para pedidos listos
const listoIcon = new L.DivIcon({
  html: '<div style="font-size:24px;text-align:center">📦</div>',
  iconSize: [28, 28],
  iconAnchor: [14, 28],
  className: '',
});

function FlyToPosition({ position }: { position: [number, number] }) {
  const map = useMap();
  const prevPos = useRef(position);
  useEffect(() => {
    if (prevPos.current[0] !== position[0] || prevPos.current[1] !== position[1]) {
      map.flyTo(position, map.getZoom(), { duration: 1 });
      prevPos.current = position;
    }
  }, [position, map]);
  return null;
}

/** Ajusta el mapa para mostrar todos los markers */
function FitBounds({ positions }: { positions: [number, number][] }) {
  const map = useMap();
  useEffect(() => {
    if (positions.length > 1) {
      const bounds = L.latLngBounds(positions.map(p => L.latLng(p[0], p[1])));
      map.fitBounds(bounds, { padding: [50, 50] });
    }
  }, [positions, map]);
  return null;
}

const DEFAULT_POS: [number, number] = [4.711, -74.0721];

export function DomiciliarioPanel() {
  const { auth } = useAuth();
  const { restaurante } = useRestaurante();
  const { toast } = useToast();
  const queryClient = useQueryClient();
  const domiciliarioId = auth?.domiciliarioId ?? 0;
  const [myPos, setMyPos] = useState<[number, number] | null>(null);
  const [geoError, setGeoError] = useState(false);
  const [updatingLocation, setUpdatingLocation] = useState(false);
  const [selectedPedido, setSelectedPedido] = useState<PedidoResponse | null>(null);
  const prevCount = useRef(0);

  const updateLocation = () => {
    if (!navigator.geolocation) {
      toast('Tu navegador no soporta geolocalización', 'error');
      return;
    }
    setUpdatingLocation(true);
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        const coords: [number, number] = [pos.coords.latitude, pos.coords.longitude];
        setMyPos(coords);
        domiciliarioService.actualizarUbicacion({ domiciliarioId, lat: coords[0], lng: coords[1] })
          .then(() => toast('📍 Ubicación actualizada', 'success'))
          .catch(() => toast('Error al enviar ubicación', 'error'))
          .finally(() => setUpdatingLocation(false));
      },
      () => { toast('No se pudo obtener la ubicación', 'error'); setUpdatingLocation(false); },
      { enableHighAccuracy: true, timeout: 10000 },
    );
  };

  useEffect(() => {
    if (!navigator.geolocation) { setGeoError(true); return; }
    const watchId = navigator.geolocation.watchPosition(
      (pos) => {
        const coords: [number, number] = [pos.coords.latitude, pos.coords.longitude];
        setMyPos(coords);
        domiciliarioService.actualizarUbicacion({ domiciliarioId, lat: coords[0], lng: coords[1] }).catch(() => {});
      },
      () => setGeoError(true),
      { enableHighAccuracy: true, maximumAge: 5000, timeout: 15000 },
    );
    return () => navigator.geolocation.clearWatch(watchId);
  }, [domiciliarioId]);

  const { data: ubicacion } = useQuery({
    queryKey: ['ubicacion', domiciliarioId],
    queryFn: () => trackingService.ubicacion(domiciliarioId),
    refetchInterval: 10000,
    enabled: domiciliarioId > 0 && !myPos,
  });

  // Pedidos LISTO y EN_CAMINO
  const { data: pedidosData } = useQuery({
    queryKey: ['pedidos-domiciliario', restaurante?.id],
    queryFn: async () => {
      const restId = restaurante?.id ?? 1;
      const res = await pedidoService.listarPorRestaurante(restId, 0, 50);
      return res.content.filter(
        (p) => p.estado === EstadoPedido.LISTO || p.estado === EstadoPedido.EN_CAMINO,
      );
    },
    refetchInterval: 5000,
  });

  const pedidos = pedidosData ?? [];
  const pedidosListo = pedidos.filter(p => p.estado === EstadoPedido.LISTO);
  const pedidosEnCamino = pedidos.filter(p => p.estado === EstadoPedido.EN_CAMINO);

  // Notificación de nuevos pedidos listos
  useEffect(() => {
    if (pedidosListo.length > prevCount.current && prevCount.current > 0) {
      toast('📦 Nuevo pedido listo para recoger', 'info');
    }
    prevCount.current = pedidosListo.length;
  }, [pedidosListo.length, toast]);

  const cambiarEstado = useMutation({
    mutationFn: ({ id, estado }: { id: number; estado: EstadoPedido }) =>
      pedidoService.cambiarEstado(id, estado),
    onSuccess: (_, { estado }) => {
      queryClient.invalidateQueries({ queryKey: ['pedidos-domiciliario'] });
      if (estado === EstadoPedido.EN_CAMINO) toast('🛵 Pedido recogido, en camino', 'success');
      else if (estado === EstadoPedido.ENTREGADO) toast('🏁 Pedido entregado', 'success');
      setSelectedPedido(null);
    },
    onError: (err: Error) => toast(err.message, 'error'),
  });

  const mapCenter: [number, number] = myPos
    ?? (ubicacion?.lat && ubicacion?.lng ? [ubicacion.lat, ubicacion.lng] : DEFAULT_POS);

  // Posiciones para ajustar bounds del mapa
  const allPositions: [number, number][] = [mapCenter];
  pedidos.forEach(p => {
    if (p.lat && p.lng) allPositions.push([p.lat, p.lng]);
  });

  return (
    <div className="p-4 md:p-6 max-w-7xl mx-auto">
      <h1 className="text-xl font-bold text-gray-800 mb-4">🛵 Panel Domiciliario</h1>

      {/* Barra de ubicación */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 mb-4 bg-white rounded-xl shadow p-4">
        <div className="text-sm">
          {myPos ? (
            <span className="text-green-600">📍 Ubicación activa: {myPos[0].toFixed(5)}, {myPos[1].toFixed(5)}</span>
          ) : (
            <span className="text-gray-400">📍 Sin ubicación</span>
          )}
          <span className="ml-4 text-gray-400">
            📦 {pedidosListo.length} listos · 🛵 {pedidosEnCamino.length} en camino
          </span>
        </div>
        <button
          onClick={updateLocation}
          disabled={updatingLocation}
          className="bg-blue-600 text-white px-4 py-2 rounded-lg text-sm hover:bg-blue-700 disabled:opacity-50 transition-all"
        >
          {updatingLocation ? '⏳ Actualizando…' : '📍 Actualizar ubicación'}
        </button>
      </div>

      {geoError && !myPos && (
        <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-3 mb-4 text-sm text-yellow-800">
          ⚠️ No se pudo obtener tu ubicación. Activa la geolocalización en tu navegador.
        </div>
      )}

      <div className="grid md:grid-cols-2 gap-4">
        {/* Mapa con destinos de entrega */}
        <div className="bg-white rounded-xl shadow overflow-hidden" style={{ height: 500 }}>
          <MapContainer center={mapCenter} zoom={14} style={{ height: '100%', width: '100%' }}>
            <TileLayer
              attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
              url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
            />
            {allPositions.length > 1 ? (
              <FitBounds positions={allPositions} />
            ) : (
              <FlyToPosition position={mapCenter} />
            )}

            {/* Mi ubicación */}
            {myPos && (
              <Marker position={myPos} icon={myIcon}>
                <Popup>🛵 Estoy aquí</Popup>
              </Marker>
            )}

            {/* Pedidos LISTO — donde debe ir a recoger */}
            {pedidosListo.map((p) => {
              if (!p.lat || !p.lng) return null;
              return (
                <Marker key={`listo-${p.id}`} position={[p.lat, p.lng]} icon={listoIcon}>
                  <Popup>
                    <strong>📦 Pedido #{p.id} — LISTO</strong><br />
                    {p.direccion}<br />
                    {p.clienteNombre || p.clienteTelefono}<br />
                    <strong>${Number(p.total).toLocaleString()}</strong>
                  </Popup>
                </Marker>
              );
            })}

            {/* Pedidos EN_CAMINO — destino de entrega */}
            {pedidosEnCamino.map((p) => {
              if (!p.lat || !p.lng) return null;
              return (
                <Marker key={`camino-${p.id}`} position={[p.lat, p.lng]} icon={destinoIcon}>
                  <Popup>
                    <strong>🛵 Pedido #{p.id} — EN CAMINO</strong><br />
                    {p.direccion}<br />
                    {p.clienteNombre || p.clienteTelefono}<br />
                    <strong>${Number(p.total).toLocaleString()}</strong>
                  </Popup>
                </Marker>
              );
            })}

            {/* Línea de ruta al pedido seleccionado */}
            {selectedPedido?.lat && selectedPedido?.lng && myPos && (
              <Polyline
                positions={[myPos, [selectedPedido.lat, selectedPedido.lng]]}
                color={selectedPedido.estado === EstadoPedido.EN_CAMINO ? '#f97316' : '#6366f1'}
                weight={3}
                dashArray="8 8"
              />
            )}
          </MapContainer>
        </div>

        {/* Lista de pedidos */}
        <div className="space-y-3 overflow-y-auto" style={{ maxHeight: 500 }}>
          {/* Pedidos LISTO */}
          <h2 className="font-semibold text-gray-700">📦 Listos para recoger ({pedidosListo.length})</h2>
          {pedidosListo.length === 0 ? (
            <p className="text-gray-400 text-sm py-2">No hay pedidos listos</p>
          ) : (
            pedidosListo.map((p) => (
              <div
                key={p.id}
                className={`bg-white rounded-xl shadow p-4 cursor-pointer transition border-l-4 border-purple-500 hover:ring-2 hover:ring-purple-300 ${
                  selectedPedido?.id === p.id ? 'ring-2 ring-purple-500' : ''
                }`}
                onClick={() => setSelectedPedido(selectedPedido?.id === p.id ? null : p)}
              >
                <div className="flex items-center justify-between mb-2">
                  <span className="font-mono text-sm font-bold">#{p.id}</span>
                  <EstadoBadge estado={p.estado} />
                </div>
                <p className="text-sm text-gray-600 font-medium">📍 {p.direccion}</p>
                <p className="text-sm text-gray-500">{p.clienteNombre || p.clienteTelefono}</p>
                <p className="text-xs text-green-600 font-medium">📞 Recibe: {p.telefonoContacto || p.clienteTelefono}</p>
                <p className="text-lg font-bold text-indigo-600 mt-1">${Number(p.total).toLocaleString()}</p>

                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    cambiarEstado.mutate({ id: p.id, estado: EstadoPedido.EN_CAMINO });
                  }}
                  disabled={cambiarEstado.isPending}
                  className="w-full mt-3 bg-orange-500 text-white py-2 rounded-lg text-sm font-medium hover:bg-orange-600 disabled:opacity-50"
                >
                  🛵 Recoger pedido
                </button>

                {selectedPedido?.id === p.id && (
                  <div className="mt-3 pt-3 border-t text-sm space-y-1">
                    {p.detalles.map((d, i) => (
                      <div key={i} className="flex justify-between text-gray-600">
                        <span>{d.cantidad}x {d.producto}</span>
                        <span>${Number(d.precio).toLocaleString()}</span>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            ))
          )}

          {/* Pedidos EN_CAMINO */}
          <h2 className="font-semibold text-gray-700 mt-4">🛵 En camino ({pedidosEnCamino.length})</h2>
          {pedidosEnCamino.length === 0 ? (
            <p className="text-gray-400 text-sm py-2">No hay pedidos en camino</p>
          ) : (
            pedidosEnCamino.map((p) => (
              <div
                key={p.id}
                className={`bg-white rounded-xl shadow p-4 cursor-pointer transition border-l-4 border-orange-500 hover:ring-2 hover:ring-orange-300 ${
                  selectedPedido?.id === p.id ? 'ring-2 ring-orange-500' : ''
                }`}
                onClick={() => setSelectedPedido(selectedPedido?.id === p.id ? null : p)}
              >
                <div className="flex items-center justify-between mb-2">
                  <span className="font-mono text-sm font-bold">#{p.id}</span>
                  <EstadoBadge estado={p.estado} />
                </div>
                <p className="text-sm text-gray-600 font-medium">📍 {p.direccion}</p>
                <p className="text-sm text-gray-500">{p.clienteNombre || p.clienteTelefono}</p>
                <p className="text-xs text-green-600 font-medium">📞 Recibe: {p.telefonoContacto || p.clienteTelefono}</p>
                <p className="text-lg font-bold text-indigo-600 mt-1">${Number(p.total).toLocaleString()}</p>

                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    cambiarEstado.mutate({ id: p.id, estado: EstadoPedido.ENTREGADO });
                  }}
                  disabled={cambiarEstado.isPending}
                  className="w-full mt-3 bg-green-500 text-white py-2 rounded-lg text-sm font-medium hover:bg-green-600 disabled:opacity-50"
                >
                  🏁 Marcar como entregado
                </button>

                {selectedPedido?.id === p.id && (
                  <div className="mt-3 pt-3 border-t text-sm space-y-1">
                    {p.detalles.map((d, i) => (
                      <div key={i} className="flex justify-between text-gray-600">
                        <span>{d.cantidad}x {d.producto}</span>
                        <span>${Number(d.precio).toLocaleString()}</span>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
}
