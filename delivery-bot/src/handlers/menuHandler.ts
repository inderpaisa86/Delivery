import { api } from '../services/apiClient.js';
import { getSession } from '../session/userSession.js';

export async function handleMenu(phone: string): Promise<string> {
  const session = getSession(phone);

  try {
    const productos = await api.listarProductos(session.restauranteId);

    if (productos.length === 0) {
      return '📋 No hay productos disponibles en este momento.';
    }

    const lista = productos
      .map((p) => `• ${p.nombre} - $${p.precio.toLocaleString('es-CO')}`)
      .join('\n');

    return (
      `📋 *Menú disponible:*\n\n${lista}\n\n` +
      `Envía tu pedido así:\n"2 hamburguesas, 1 gaseosa"`
    );
  } catch (err) {
    console.error('Error obteniendo menú:', err);
    return '⚠️ No pudimos cargar el menú. Intenta de nuevo.';
  }
}
