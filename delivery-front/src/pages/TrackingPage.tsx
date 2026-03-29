import { useParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { MapContainer, TileLayer, Marker, Popup } from 'react-leaflet';
import L from 'leaflet';
import { trackingService } from '../services/tracking';
import { EstadoBadge } from '../components/EstadoBadge';

import markerIcon2x from 'leaflet/dist/images/marker-icon-2x.png';
import markerIcon from 'leaflet/dist/images/marker-icon.png';
import markerShadow from 'leaflet/dist/images/marker-shadow.png';

delete (L.Icon.Default.prototype as Record<string, unknown>)._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: markerIcon2x,
  iconUrl: markerIcon,
  shadowUrl: markerShadow,
});

export function TrackingPage() {
  const { token } = useParams<{ token: string }>();

  const { data: tracking, isLoading, error } = useQuery({
    queryKey: ['tracking', token],
    queryFn: () => trackingService.obtener(token!),
    refetchInterval: 5000,
    enabled: !!token,
  });

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50">
        <p className="text-gray-500">Cargando tracking…</p>
      </div>
    );
  }

  if (error || !tracking) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50">
        <div className="text-center">
          <p className="text-4xl mb-2">🔍</p>
          <p className="text-gray-600">No se encontró el pedido</p>
        </div>
      </div>
    );
  }

  const center: [number, number] = tracking.pedidoLat && tracking.pedidoLng
    ? [tracking.pedidoLat, tracking.pedidoLng]
    : [4.711, -74.0721];

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-2xl mx-auto p-4">
        <div className="bg-white rounded-xl shadow p-6 mb-4">
          <h1 className="text-xl font-bold mb-2">📦 Tracking Pedido #{tracking.pedidoId}</h1>
          <div className="flex items-center gap-3 mb-3">
            <EstadoBadge estado={tracking.estado} />
            {tracking.domiciliarioNombre && (
              <span className="text-sm text-gray-500">🛵 {tracking.domiciliarioNombre}</span>
            )}
          </div>
          <p className="text-sm text-gray-600">📍 {tracking.direccion}</p>
        </div>

        <div className="bg-white rounded-xl shadow overflow-hidden" style={{ height: 350 }}>
          <MapContainer center={center} zoom={14} style={{ height: '100%', width: '100%' }}>
            <TileLayer
              attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
              url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
            />
            {tracking.pedidoLat && tracking.pedidoLng && (
              <Marker position={[tracking.pedidoLat, tracking.pedidoLng]}>
                <Popup>📍 Destino: {tracking.direccion}</Popup>
              </Marker>
            )}
            {tracking.domiciliarioLat && tracking.domiciliarioLng && (
              <Marker position={[tracking.domiciliarioLat, tracking.domiciliarioLng]}>
                <Popup>🛵 {tracking.domiciliarioNombre}</Popup>
              </Marker>
            )}
          </MapContainer>
        </div>
      </div>
    </div>
  );
}
