import { api, type Producto } from '../services/apiClient.js';
import { getSession, updateSession } from '../session/userSession.js';

/**
 * Intenta parsear un mensaje como pedido.
 * Formato esperado: "2 hamburguesas, 1 gaseosa"
 * Retorna null si no es un pedido válido.
 */
export async function handlePedido(phone: string, texto: string): Promise<string | null> {
  const session = getSession(phone);

  // Cargar productos del restaurante
  let productos: Producto[];
  try {
    productos = await api.listarProductos(session.restauranteId);
  } catch {
    return '⚠️ Error conectando con el servidor. Intenta de nuevo.';
  }

  if (productos.length === 0) {
    return null; // No hay productos, no es un pedido
  }

  // Parsear el mensaje
  const detalles = parsearPedido(texto, productos);
  if (detalles.length === 0) {
    return null; // No se reconoció como pedido
  }

  try {
    const response = await api.crearPedido({
      restauranteId: session.restauranteId,
      telefono: phone,
      direccion: 'Pendiente ubicación',
      detalles,
    });

    // Guardar pedido pendiente y esperar ubicación
    updateSession(phone, { step: 'waiting_location', pendingPedidoId: response.id });

    const resumen = response.detalles
      .map((d) => `• ${d.cantidad}x ${d.producto} - $${(d.cantidad * d.precio).toLocaleString('es-CO')}`)
      .join('\n');

    return (
      `✅ *Pedido #${response.id} recibido*\n\n` +
      `${resumen}\n\n` +
      `💰 *Subtotal: $${response.total.toLocaleString('es-CO')}*\n` +
      `🛵 _El valor del domicilio es adicional al pedido_\n\n` +
      `🔎 *Seguimiento:* #${response.id}\n\n` +
      `📍 Ahora envíanos tu *ubicación* para saber dónde entregar.\n` +
      `Toca 📎 → Ubicación → Enviar ubicación actual.`
    );
  } catch (err) {
    console.error('Error creando pedido:', err);
    return '⚠️ No pudimos procesar tu pedido. Intenta de nuevo.';
  }
}

/** Parsea "2 hamburguesas, 1 gaseosa" y matchea con productos */
function parsearPedido(
  texto: string,
  productos: Producto[],
): { productoId: number; cantidad: number }[] {
  const detalles: { productoId: number; cantidad: number }[] = [];
  const segmentos = texto.split(',');

  for (const seg of segmentos) {
    const trimmed = seg.trim();
    const spaceIdx = trimmed.indexOf(' ');
    if (spaceIdx <= 0) continue;

    const cantStr = trimmed.substring(0, spaceIdx);
    const nombre = trimmed.substring(spaceIdx + 1).trim().toLowerCase();

    if (!/^\d+$/.test(cantStr)) continue;
    const cantidad = parseInt(cantStr);
    if (cantidad <= 0 || cantidad > 100) continue;

    // Buscar producto: exacto → singular → parcial
    const producto = buscarProducto(nombre, productos);
    if (producto) {
      detalles.push({ productoId: producto.id, cantidad });
    }
  }

  return detalles;
}

function buscarProducto(nombre: string, productos: Producto[]): Producto | undefined {
  // Exacto
  let found = productos.find((p) => p.nombre.toLowerCase() === nombre);
  if (found) return found;

  // Singular (quitar s/es)
  const singular = normalizarPlural(nombre);
  if (singular !== nombre) {
    found = productos.find((p) => p.nombre.toLowerCase() === singular);
    if (found) return found;
  }

  // Parcial
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
