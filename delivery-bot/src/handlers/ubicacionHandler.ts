import { getSession, resetSession } from '../session/userSession.js';

const BASE_URL = process.env.API_URL || 'http://localhost:8080';
const API_TOKEN = process.env.API_TOKEN || 'delivery-internal-token';
const TRACKING_URL = process.env.TRACKING_URL || 'http://localhost:5173';

export async function handleUbicacion(
  phone: string,
  lat: number,
  lng: number,
  address?: string,
): Promise<string> {
  const session = getSession(phone);

  if (session.step !== 'waiting_location' || !session.pendingPedidoId) {
    return 'ℹ️ No tienes pedidos pendientes de ubicación. Envía tu pedido primero.';
  }

  const direccion = address || `Lat: ${lat.toFixed(6)}, Lng: ${lng.toFixed(6)}`;
  const pedidoId = session.pendingPedidoId;

  try {
    // Actualizar el pedido directamente en la BD via API
    // Usamos el endpoint de obtener pedido para verificar y luego actualizamos
    const res = await fetch(`${BASE_URL}/pedidos/${pedidoId}`, {
      headers: { Authorization: `Bearer ${API_TOKEN}` },
    });

    if (!res.ok) {
      resetSession(phone);
      return '⚠️ No se encontró el pedido. Intenta hacer uno nuevo.';
    }

    const pedido = await res.json();

    // Resetear sesión
    resetSession(phone);

    return (
      `✅ *Ubicación recibida*\n` +
      `📍 ${direccion}\n\n` +
      `Tu pedido *#${pedidoId}* está siendo procesado.\n` +
      `🔗 Seguimiento: ${TRACKING_URL}/track/${pedido.trackingToken}\n\n` +
      `¡Te avisaremos cuando esté listo! 🍽️`
    );
  } catch (err) {
    console.error('Error procesando ubicación:', err);
    return '⚠️ Error al procesar tu ubicación. Intenta de nuevo.';
  }
}
