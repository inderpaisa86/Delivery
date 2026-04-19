import { api, type Producto } from '../services/apiClient.js';
import { getSession, updateSession } from '../session/userSession.js';

interface PendingOrder {
  detalles: { productoId: number; cantidad: number }[];
  resumenTexto: string;
}

const pendingOrders = new Map<string, PendingOrder>();

/**
 * Parsea pedido → busca cliente → decide flujo:
 * - Cliente conocido con dirección → pregunta si va a la misma
 * - Cliente conocido sin dirección → pide ubicación
 * - Cliente nuevo → pide nombre
 */
export async function handlePedido(phone: string, texto: string): Promise<string | null> {
  const session = getSession(phone);

  let productos: Producto[];
  try {
    productos = await api.listarProductos(session.restauranteId);
  } catch {
    return '⚠️ Error conectando con el servidor. Intenta de nuevo.';
  }

  if (productos.length === 0) return null;

  const detalles = parsearPedido(texto, productos);
  if (detalles.length === 0) return null;

  const resumen = detalles.map((d) => {
    const prod = productos.find((p) => p.id === d.productoId);
    const nombre = prod?.nombre ?? '?';
    const subtotal = (prod?.precio ?? 0) * d.cantidad;
    return `• ${d.cantidad}x ${nombre} - $${subtotal.toLocaleString('es-CO')}`;
  }).join('\n');

  pendingOrders.set(phone, { detalles, resumenTexto: resumen });

  // Buscar si el cliente ya existe
  const cliente = await api.buscarCliente(phone, session.restauranteId);

  if (cliente?.nombre && cliente?.direccion && !cliente.direccion.toLowerCase().includes('pendiente')) {
    updateSession(phone, {
      step: 'waiting_address_confirm',
      clienteNombre: cliente.nombre,
      clienteDireccion: cliente.direccion,
    });
    return (
      `🛒 *Tu pedido:*\n\n${resumen}\n\n` +
      `👤 Hola *${cliente.nombre}*!\n` +
      `📍 Tu última dirección fue:\n_${cliente.direccion}_\n\n` +
      `¿Enviamos a la misma dirección?\n` +
      `Escribe *si* para confirmar o *no* para enviar una nueva ubicación.`
    );
  }

  if (cliente?.nombre) {
    // Tiene nombre pero no dirección
    return await crearYPedirUbicacion(phone, cliente.nombre, resumen);
  }

  // Cliente nuevo
  updateSession(phone, { step: 'waiting_name' });
  return `🛒 *Tu pedido:*\n\n${resumen}\n\n📝 Para continuar, escribe tu *nombre completo*:`;
}

/**
 * Cliente confirmó dirección anterior con "si".
 */
export async function handleAddressConfirm(phone: string, respuesta: string): Promise<string> {
  const session = getSession(phone);
  const pending = pendingOrders.get(phone);

  if (!pending) {
    updateSession(phone, { step: 'idle' });
    return '⚠️ No hay pedido pendiente. Envía tu pedido de nuevo.';
  }

  const esSi = ['si', 'sí', 'yes', 'ok', 'dale', 'confirmar'].includes(respuesta.toLowerCase());

  if (esSi && session.clienteDireccion) {
    // Crear pedido con la dirección anterior
    try {
      const response = await api.crearPedido({
        restauranteId: session.restauranteId,
        telefono: phone,
        nombre: session.clienteNombre,
        direccion: session.clienteDireccion,
        detalles: pending.detalles,
      });

      pendingOrders.delete(phone);
      updateSession(phone, { step: 'idle' });

      const resumen = response.detalles
        .map((d) => `• ${d.cantidad}x ${d.producto} - $${(d.cantidad * d.precio).toLocaleString('es-CO')}`)
        .join('\n');

      const TRACKING_URL = process.env.TRACKING_URL || 'http://localhost:5173';

      return (
        `✅ *¡Pedido #${response.numeroDiario ?? response.id} confirmado!*\n` +
        `👤 *Cliente:* ${session.clienteNombre}\n` +
        `📍 *Dirección:* ${session.clienteDireccion}\n\n` +
        `${resumen}\n\n` +
        `💰 *Subtotal: $${response.total.toLocaleString('es-CO')}*\n` +
        `🛵 _El valor del domicilio es adicional al pedido_\n\n` +
        `🔗 *Seguimiento:* ${TRACKING_URL}/track/${response.trackingToken}\n\n` +
        `¡Tu pedido está siendo procesado! 🍽️`
      );
    } catch (err) {
      console.error('Error creando pedido:', err);
      pendingOrders.delete(phone);
      updateSession(phone, { step: 'idle' });
      return '⚠️ Error al crear el pedido. Intenta de nuevo.';
    }
  }

  // Dijo "no" — pedir nueva ubicación
  return await crearYPedirUbicacion(phone, session.clienteNombre || '', pending.resumenTexto);
}

/** Recibe el nombre y crea el pedido */
export async function handleNombre(phone: string, nombre: string): Promise<string> {
  const pending = pendingOrders.get(phone);
  if (!pending) {
    updateSession(phone, { step: 'idle' });
    return '⚠️ No hay pedido pendiente. Envía tu pedido de nuevo.';
  }
  return await crearYPedirUbicacion(phone, nombre.trim(), pending.resumenTexto);
}

/** Crea el pedido en el backend y ofrece opciones de dirección */
async function crearYPedirUbicacion(phone: string, nombre: string, _resumen: string): Promise<string> {
  const session = getSession(phone);
  const pending = pendingOrders.get(phone);

  if (!pending) {
    updateSession(phone, { step: 'idle' });
    return '⚠️ No hay pedido pendiente.';
  }

  try {
    const response = await api.crearPedido({
      restauranteId: session.restauranteId,
      telefono: phone,
      nombre,
      direccion: 'Pendiente ubicación',
      detalles: pending.detalles,
    });

    pendingOrders.delete(phone);
    updateSession(phone, {
      step: 'waiting_location_choice',
      pendingPedidoId: response.id,
      clienteNombre: nombre,
    });

    const resumen = response.detalles
      .map((d) => `• ${d.cantidad}x ${d.producto} - $${(d.cantidad * d.precio).toLocaleString('es-CO')}`)
      .join('\n');

    return (
      `✅ *Pedido #${response.numeroDiario ?? response.id} recibido*\n` +
      `👤 *Cliente:* ${nombre}\n\n` +
      `${resumen}\n\n` +
      `💰 *Subtotal: $${response.total.toLocaleString('es-CO')}*\n` +
      `🛵 _El valor del domicilio es adicional al pedido_\n\n` +
      `📍 *¿Cómo quieres enviar tu dirección?*\n\n` +
      `*1️⃣* Enviar *ubicación GPS* (📎 → Ubicación)\n` +
      `*2️⃣* Escribir *dirección manualmente*\n\n` +
      `Escribe *1* o *2*, o envía tu ubicación directamente.`
    );
  } catch (err) {
    console.error('Error creando pedido:', err);
    pendingOrders.delete(phone);
    updateSession(phone, { step: 'idle' });
    return '⚠️ No pudimos procesar tu pedido. Intenta de nuevo.';
  }
}

/** Parsea pedidos separados por coma, "y", punto y coma, salto de línea, o número después de texto */
function parsearPedido(texto: string, productos: Producto[]): { productoId: number; cantidad: number }[] {
  const detalles: { productoId: number; cantidad: number }[] = [];
  const normalizado = texto.replace(/([a-záéíóúñ])\s+(\d)/gi, '$1, $2');
  const segmentos = normalizado.split(/[,;\n]|\by\b/i);

  for (const seg of segmentos) {
    const trimmed = seg.trim();
    const spaceIdx = trimmed.indexOf(' ');
    if (spaceIdx <= 0) continue;
    const cantStr = trimmed.substring(0, spaceIdx);
    const nombre = trimmed.substring(spaceIdx + 1).trim().toLowerCase();
    if (!/^\d+$/.test(cantStr)) continue;
    const cantidad = parseInt(cantStr);
    if (cantidad <= 0 || cantidad > 100) continue;
    const producto = buscarProducto(nombre, productos);
    if (producto) detalles.push({ productoId: producto.id, cantidad });
  }
  return detalles;
}

function buscarProducto(nombre: string, productos: Producto[]): Producto | undefined {
  let found = productos.find((p) => p.nombre.toLowerCase() === nombre);
  if (found) return found;
  const singular = normalizarPlural(nombre);
  if (singular !== nombre) {
    found = productos.find((p) => p.nombre.toLowerCase() === singular);
    if (found) return found;
  }
  found = productos.find((p) => p.nombre.toLowerCase().includes(singular));
  if (found) return found;
  return productos.find((p) => p.nombre.toLowerCase().includes(nombre));
}

function normalizarPlural(palabra: string): string {
  if (palabra.endsWith('ces')) return palabra.slice(0, -3) + 'z';
  if (palabra.endsWith('es') && palabra.length > 3) {
    const prev = palabra[palabra.length - 3];
    if (!'aeiouáéíóú'.includes(prev)) return palabra.slice(0, -2);
  }
  if (palabra.endsWith('s') && palabra.length > 2) return palabra.slice(0, -1);
  return palabra;
}
