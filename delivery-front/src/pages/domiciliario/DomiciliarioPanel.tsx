import { useEffect, useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { MapContainer, TileLayer, Marker, Popup, useMap } from 'react-leaflet';
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

const myIcon = new L.Icon({
  iconUrl: markerIcon,
  iconRetinaUrl: markerIcon2x,
  shadowUrl: markerShadow,
  iconSize: [25, 41],
  iconAnchor: [12, 41],
  className: 'hue-rotate-[200deg] saturate-150',
});

/** Componente que mueve el mapa cuando cambia la posición */
function FlyToPosition({ position }: { position: [number, number] }) {
  const map = useMap();
  useEffect(() => {
    map.flyTo(position, map.getZoom(), { duration: 1 });
  }, [position, map]);
  return null;
}

const DEFAULT_POS: [number, number] = [4.711, -74.0721]; // Bogotá

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

  // Función para actualizar ubicación manualmente
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
        domiciliarioService.actualizarUbicacion({
          domiciliarioId,
          lat: coords[0],
          lng: coords[1],
        })
          .then(() => toast('📍 Ubicación actualizada', 'success'))
          .catch(() => toast('Error al enviar ubicación', 'error'))
          .finally(() => setUpdatingLocation(false));
      },
      () => {
        toast('No se pudo obtener la ubicación', 'error');
        setUpdatingLocation(false);
      },
      { enableHighAccuracy: true, timeout: 10000 },
    );
  };

  // Geolocalización real del navegador
  useEffect(() => {
    if (!navigator.geolocation) {
      setGeoError(true);
      return;
    }
    const watchId = navigator.geolocation.watchPosition(
      (pos) => {
        const coords: [number, number] = [pos.coords.latitude, pos.coords.longitude];
        setMyPos(coords);
        domiciliarioService.actualizarUbicacion({
          domiciliarioId,
          lat: coords[0],
          lng: coords[1],
        }).catch(() => {});
      },
      () => setGeoError(true),
      { enableHighAccuracy: true, maximumAge: 5000, timeout: 15000 },
    );
    return () => navigator.geolocation.clearWatch(watchId);
  }, [domiciliarioId]);

  // Ubicación desde el backend como fallback
  const { data: ubicacion } = useQuery({
    queryKey: ['ubicacion', domiciliarioId],
    queryFn: () => trackingService.ubicacion(domiciliarioId),
    refetchInterval: 10000,
    enabled: domiciliarioId > 0 && !myPos,
  });

  // Pedidos asignados
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

  const cambiarEstado = useMutation({
    mutationFn: ({ id, estado }: { id: number; estado: EstadoPedido }) =>
      pedidoService.cambiarEstado(id, estado),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['pedidos-domiciliario'] });
      toast('Estado actualizado', 'success');
    },
    onError: (err: Error) => toast(err.message, 'error'),
  });

  // Centro del mapa: mi posición > ubicación backend > default
  const mapCenter: [number, number] = myPos
    ?? (ubicacion?.lat && ubicacion?.lng ? [ubicacion.lat, ubicacion.lng] : DEFAULT_POS);

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
        {/* Mapa */}
        <div className="bg-white rounded-xl shadow overflow-hidden" style={{ height: 450 }}>
          <MapContainer center={mapCenter} zoom={16} style={{ height: '100%', width: '100%' }}>
            <TileLayer
              attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
              url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
            />
            <FlyToPosition position={mapCenter} />

            {/* Mi ubicación */}
            {myPos && (
              <Marker position={myPos} icon={myIcon}>
                <Popup>📍 Estoy aquí</Popup>
              </Marker>
            )}

            {/* Marcadores de pedidos con coordenadas reales */}
            {pedidos.map((p) => {
              const lat = (p as unknown as Record<string, number>).lat;
              const lng = (p as unknown as Record<string, number>).lng;
              if (!lat || !lng) return null;
              return (
                <Marker key={p.id} position={[lat, lng]}>
                  <Popup>
                    <strong>Pedido #{p.id}</strong><br />
                    {p.direccion}<br />
                    ${Number(p.total).toLocaleString()}
                  </Popup>
                </Marker>
              );
            })}
          </MapContainer>
        </div>

        {/* Lista de pedidos */}
        <div className="space-y-3 overflow-y-auto" style={{ maxHeight: 450 }}>
          <h2 className="font-semibold text-gray-700">Pedidos asignados</h2>
          {pedidos.length === 0 ? (
            <p className="text-gray-400 text-center py-8">No hay pedidos asignados</p>
          ) : (
            pedidos.map((p) => (
              <div
                key={p.id}
                className={`bg-white rounded-xl shadow p-4 cursor-pointer transition hover:ring-2 hover:ring-indigo-300 ${
                  selectedPedido?.id === p.id ? 'ring-2 ring-indigo-500' : ''
                }`}
                onClick={() => setSelectedPedido(p)}
              >
                <div className="flex items-center justify-between mb-2">
                  <span className="font-mono text-sm font-bold">#{p.id}</span>
                  <EstadoBadge estado={p.estado} />
                </div>
                <p className="text-sm text-gray-600">{p.direccion}</p>
                <p className="text-sm text-gray-500">{p.clienteNombre || p.clienteTelefono}</p>
                <p className="text-lg font-bold text-indigo-600 mt-1">${Number(p.total).toLocaleString()}</p>

                <div className="flex gap-2 mt-3">
                  {p.estado === EstadoPedido.LISTO && (
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        cambiarEstado.mutate({ id: p.id, estado: EstadoPedido.EN_CAMINO });
                      }}
                      className="flex-1 bg-orange-500 text-white py-1.5 rounded-lg text-sm hover:bg-orange-600"
                    >
                      🛵 Recoger
                    </button>
                  )}
                  {p.estado === EstadoPedido.EN_CAMINO && (
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        cambiarEstado.mutate({ id: p.id, estado: EstadoPedido.ENTREGADO });
                      }}
                      className="flex-1 bg-green-500 text-white py-1.5 rounded-lg text-sm hover:bg-green-600"
                    >
                      🏁 Entregar
                    </button>
                  )}
                </div>

                {selectedPedido?.id === p.id && (
                  <div className="mt-3 pt-3 border-t text-sm space-y-1">
                    <h4 className="font-medium text-gray-700">Productos:</h4>
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
