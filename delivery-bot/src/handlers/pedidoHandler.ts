import { api, type Producto } from '../services/apiClient.js';
import { getSession, updateSession } from '../session/userSession.js';

/** Datos del pedido pendiente mientras se pide el nombre */
interface PendingOrder {
  detalles: { productoId: number; cantidad: number }[];
  resumenTexto: string;
}

const pendingOrders = new Map<string, PendingOrder>();

/**
 * Intenta parsear un mensaje como pedido.
 * Si es válido, pide el nombre antes de crear.
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

  // Calcular resumen para mostrarlo
  const resumen = detalles.map((d) => {
    const prod = productos.find((p) => p.id === d.productoId);
    const nombre = prod?.nombre ?? '?';
    const subtotal = (prod?.precio ?? 0) * d.cantidad;
    return `• ${d.cantidad}x ${nombre} - $${subtotal.toLocaleString('es-CO')}`;
  }).join('\n');

  // Guardar pedido pendiente y pedir nombre
  pendingOrders.set(phone, { detalles, resumenTexto: resumen });
  updateSession(phone, { step: 'waiting_name' });

  return (
    `🛒 *Tu pedido:*\n\n${resumen}\n\n` +
    `📝 Para continuar, escribe tu *nombre completo*:`
  );
}

/**
 * Recibe el nombre y crea el pedido en el backend.
 */
export async function handleNombre(phone: string, nombre: string): Promise<string> {
  const session = getSession(phone);
  const pending = pendingOrders.get(phone);

  if (!pending) {
    updateSession(phone, { step: 'idle' });
    return '⚠️ No hay pedido pendiente. Envía tu pedido de nuevo.';
  }

  try {
    const response = await api.crearPedido({
      restauranteId: session.restauranteId,
      telefono: phone,
      nombre: nombre.trim(),
      direccion: 'Pendiente ubicación',
      detalles: pending.detalles,
    });

    pendingOrders.delete(phone);
    updateSession(phone, {
      step: 'waiting_location',
      pendingPedidoId: response.id,
      clienteNombre: nombre.trim(),
    });

    const resumen = response.detalles
      .map((d) => `• ${d.cantidad}x ${d.producto} - $${(d.cantidad * d.precio).toLocaleString('es-CO')}`)
      .join('\n');

    return (
      `✅ *Pedido #${response.numeroDiario ?? response.id} recibido*\n` +
      `👤 *Cliente:* ${nombre.trim()}\n\n` +
      `${resumen}\n\n` +
      `💰 *Subtotal: $${response.total.toLocaleString('es-CO')}*\n` +
      `🛵 _El valor del domicilio es adicional al pedido_\n\n` +
      `📍 Ahora envíanos tu *ubicación* para saber dónde entregar.\n` +
      `Toca 📎 → Ubicación → Enviar ubicación actual.`
    );
  } catch (err) {
    console.error('Error creando pedido:', err);
    pendingOrders.delete(phone);
    updateSession(phone, { step: 'idle' });
    return '⚠️ No pudimos procesar tu pedido. Intenta de nuevo.';
  }
}

/** Parsea pedidos separados por coma, "y", punto y coma, salto de línea, o número después de texto */
function parsearPedido(
  texto: string,
  productos: Producto[],
): { productoId: number; cantidad: number }[] {
  const detalles: { productoId: number; cantidad: number }[] = [];

  // Insertar separador antes de cada número que viene después de una letra (ej: "hamburguesas 1" → "hamburguesas, 1")
  const normalizado = texto.replace(/([a-záéíóúñ])\s+(\d)/gi, '$1, $2');

  // Separar por: coma, punto y coma, "y", salto de línea
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
    if (producto) {
      detalles.push({ productoId: producto.id, cantidad });
    }
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
