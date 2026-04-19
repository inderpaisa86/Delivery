import { api } from '../services/apiClient.js';
import { getSession, updateSession, resetSession } from '../session/userSession.js';

const TRACKING_URL = process.env.TRACKING_URL || 'http://localhost:5173';

/**
 * Recibe la ubicación GPS y pide datos adicionales de entrega.
 */
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

  // Guardar coordenadas y pasar al siguiente paso
  updateSession(phone, {
    step: 'waiting_details',
    pendingLat: lat,
    pendingLng: lng,
    pendingAddress: address || undefined,
  });

  console.log(`📍 [${phone}] Ubicación recibida: ${lat}, ${lng} - "${address || 'sin dirección'}"`);

  return (
    `📍 *Ubicación recibida*\n\n` +
    `Ahora envíanos los datos de entrega para que el domiciliario te encuentre fácil:\n\n` +
    `📝 Escribe en un solo mensaje:\n` +
    `_Barrio, edificio/casa, torre, apto, referencias_\n\n` +
    `Ejemplo: _"Barrio Chicó, Edificio Torres del Parque, Torre 2, Apto 501, portería principal"_`
  );
}

/**
 * Recibe los datos adicionales de entrega y confirma el pedido.
 */
export async function handleDetallesEntrega(
  phone: string,
  detalles: string,
): Promise<string> {
  const session = getSession(phone);

  if (session.step !== 'waiting_details' || !session.pendingPedidoId || !session.pendingLat || !session.pendingLng) {
    return 'ℹ️ No tienes pedidos pendientes. Envía tu pedido primero.';
  }

  // Construir dirección completa: dirección GPS + datos adicionales
  const parts: string[] = [];
  if (session.pendingAddress) parts.push(session.pendingAddress);
  parts.push(detalles.trim());
  const direccionCompleta = parts.join(' — ');

  const pedidoId = session.pendingPedidoId;

  try {
    const pedido = await api.actualizarUbicacion(
      pedidoId,
      session.pendingLat,
      session.pendingLng,
      direccionCompleta,
    );

    console.log(`✅ [${phone}] Pedido #${pedidoId} ubicación confirmada: ${direccionCompleta}`);

    resetSession(phone);

    return (
      `✅ *¡Pedido #${pedidoId} confirmado!*\n\n` +
      `📍 *Dirección:* ${direccionCompleta}\n` +
      `💰 *Total:* $${Number(pedido.total).toLocaleString('es-CO')}\n\n` +
      `🔗 *Seguimiento:* ${TRACKING_URL}/track/${pedido.trackingToken}\n\n` +
      `El restaurante está procesando tu pedido. ¡Te avisaremos cuando esté en camino! 🍽️`
    );
  } catch (err) {
    console.error('Error confirmando pedido:', err);
    return '⚠️ Error al confirmar tu pedido. Intenta enviar los datos de nuevo.';
  }
}
