import { api } from '../services/apiClient.js';
import { getSession, updateSession, resetSession } from '../session/userSession.js';

const TRACKING_URL = process.env.TRACKING_URL || 'http://localhost:5173';

/**
 * Recibe la ubicación GPS y pide datos adicionales de entrega.
 */
export async function handleUbicacion(
  phone: string, lat: number, lng: number, address?: string,
): Promise<string> {
  const session = getSession(phone);

  if (!['waiting_location', 'waiting_location_choice'].includes(session.step) || !session.pendingPedidoId) {
    return 'ℹ️ No tienes pedidos pendientes de ubicación. Envía tu pedido primero.';
  }

  updateSession(phone, {
    step: 'waiting_details',
    pendingLat: lat, pendingLng: lng,
    pendingAddress: address || undefined,
  });

  console.log(`📍 [${phone}] GPS recibido: ${lat}, ${lng} - "${address || ''}"`);

  return (
    `📍 *Ubicación recibida*\n\n` +
    `Envíanos los datos de entrega:\n` +
    `📝 _Barrio, edificio/casa, torre, apto, referencias_\n\n` +
    `Ejemplo: _"Barrio Chicó, Edificio Torres, Torre 2, Apto 501"_`
  );
}

/**
 * Recibe dirección escrita manualmente — pasa a preguntar teléfono.
 */
export async function handleDireccionManual(phone: string, direccion: string): Promise<string> {
  const session = getSession(phone);

  if (session.step !== 'waiting_typed_address' || !session.pendingPedidoId) {
    return 'ℹ️ No tienes pedidos pendientes.';
  }

  updateSession(phone, {
    step: 'waiting_contact_phone',
    pendingAddress: direccion.trim(),
    pendingLat: undefined,
    pendingLng: undefined,
  });

  return preguntarTelefono(phone);
}

/**
 * Recibe datos adicionales después de GPS — pasa a preguntar teléfono.
 */
export async function handleDetallesEntrega(phone: string, detalles: string): Promise<string> {
  const session = getSession(phone);

  if (session.step !== 'waiting_details' || !session.pendingPedidoId) {
    return 'ℹ️ No tienes pedidos pendientes.';
  }

  const parts: string[] = [];
  if (session.pendingAddress) parts.push(session.pendingAddress);
  parts.push(detalles.trim());
  const direccionCompleta = parts.join(' — ');

  updateSession(phone, {
    step: 'waiting_contact_phone',
    pendingAddress: direccionCompleta,
  });

  return preguntarTelefono(phone);
}

function preguntarTelefono(phone: string): string {
  return (
    `📞 *¿Quién recibe el pedido?*\n\n` +
    `Escribe *1* si lo recibes tú (${phone})\n` +
    `O escribe el *número de teléfono* de quien lo recibe.`
  );
}

/**
 * Recibe el teléfono de contacto y confirma el pedido.
 */
export async function handleContactPhone(phone: string, respuesta: string): Promise<string> {
  const session = getSession(phone);

  if (session.step !== 'waiting_contact_phone' || !session.pendingPedidoId) {
    return 'ℹ️ No tienes pedidos pendientes.';
  }

  // Determinar teléfono de contacto
  let contactPhone = phone;
  const clean = respuesta.replace(/\D/g, '');

  if (respuesta.trim() !== '1' && clean.length >= 7) {
    contactPhone = clean;
  }

  const pedidoId = session.pendingPedidoId;
  const direccion = session.pendingAddress || 'Sin dirección';
  const lat = session.pendingLat ?? 0;
  const lng = session.pendingLng ?? 0;

  try {
    const pedido = await api.actualizarUbicacion(pedidoId, lat, lng, direccion, contactPhone);

    console.log(`✅ [${phone}] Pedido #${pedidoId} confirmado | dir: ${direccion} | contacto: ${contactPhone}`);

    resetSession(phone);

    const contactoInfo = contactPhone === phone
      ? `📞 *Contacto:* ${phone} (tú)`
      : `📞 *Contacto entrega:* ${contactPhone}`;

    return (
      `✅ *¡Pedido #${pedido.numeroDiario ?? pedidoId} confirmado!*\n\n` +
      `📍 *Dirección:* ${direccion}\n` +
      `${contactoInfo}\n` +
      `💰 *Total:* $${Number(pedido.total).toLocaleString('es-CO')}\n` +
      `🛵 _El valor del domicilio es adicional al pedido_\n\n` +
      `🔗 *Seguimiento:* ${TRACKING_URL}/track/${pedido.trackingToken}\n\n` +
      `El restaurante está procesando tu pedido. ¡Te avisaremos cuando esté en camino! 🍽️`
    );
  } catch (err) {
    console.error('Error confirmando pedido:', err);
    return '⚠️ Error al confirmar tu pedido. Intenta de nuevo.';
  }
}
