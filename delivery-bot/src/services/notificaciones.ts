import { api, type PedidoResponse } from './apiClient.js';

const RESTAURANTE_ID = Number(process.env.RESTAURANTE_ID || '1');
const ENV = process.env.NODE_ENV || 'development';
const POLL_INTERVAL = ENV === 'production'
  ? Number(process.env.POLL_INTERVAL_MS || '300000')   // prod: 5 minutos
  : Number(process.env.POLL_INTERVAL_MS || '10000');    // dev: 10 segundos

const TRACKING_URL = process.env.TRACKING_URL || 'http://localhost:5173';

// Mapa de pedidoId → último estado conocido
const estadoCache = new Map<number, string>();

const MENSAJES: Record<string, (p: PedidoResponse) => string> = {
  CONFIRMADO: (p) =>
    `✅ *Pedido #${p.id} confirmado*\n\nEl restaurante confirmó tu pedido. Están preparándolo pronto.`,
  PREPARANDO: (p) =>
    `👨‍🍳 *Pedido #${p.id} en preparación*\n\nTu pedido se está preparando. ¡Ya falta poco!`,
  LISTO: (p) =>
    `📦 *Pedido #${p.id} listo*\n\nTu pedido está listo. Un domiciliario lo recogerá pronto.`,
  EN_CAMINO: (p) =>
    `🛵 *Pedido #${p.id} en camino*\n\nTu pedido va en camino a tu dirección.\n\n🔗 Seguimiento: ${TRACKING_URL}/track/${p.trackingToken}`,
  ENTREGADO: (p) =>
    `🏁 *Pedido #${p.id} entregado*\n\n¡Tu pedido fue entregado! Gracias por tu compra. 🙌\n\nEscribe *menu* para pedir de nuevo.`,
  CANCELADO: (p) =>
    `❌ *Pedido #${p.id} cancelado*\n\nTu pedido fue cancelado. Si fue un error, escribe *menu* para hacer uno nuevo.`,
};

type SendMessageFn = (phone: string, message: string) => Promise<void>;

let intervalId: ReturnType<typeof setInterval> | null = null;

export function iniciarPolling(sendMessage: SendMessageFn) {
  console.log(`🔔 Notificaciones activas — polling cada ${POLL_INTERVAL / 1000}s (${ENV})`);

  // Primera carga: llenar cache sin notificar
  cargarEstadosIniciales();

  intervalId = setInterval(async () => {
    try {
      const pedidos = await api.listarPedidosHoy(RESTAURANTE_ID);

      for (const pedido of pedidos) {
        const estadoAnterior = estadoCache.get(pedido.id);
        const estadoActual = pedido.estado;

        // Si es nuevo en el cache, solo guardar
        if (estadoAnterior === undefined) {
          estadoCache.set(pedido.id, estadoActual);
          continue;
        }

        // Si cambió de estado, notificar
        if (estadoAnterior !== estadoActual) {
          estadoCache.set(pedido.id, estadoActual);

          const generarMensaje = MENSAJES[estadoActual];
          if (generarMensaje && pedido.clienteTelefono) {
            const mensaje = generarMensaje(pedido);
            const whatsappId = `${pedido.clienteTelefono}@c.us`;

            console.log(`🔔 Notificando [${pedido.clienteTelefono}] pedido #${pedido.id}: ${estadoAnterior} → ${estadoActual}`);

            try {
              await sendMessage(whatsappId, mensaje);
            } catch (err) {
              console.error(`Error enviando notificación a ${pedido.clienteTelefono}:`, err);
            }
          }
        }
      }
    } catch (err) {
      console.error('Error en polling de notificaciones:', err);
    }
  }, POLL_INTERVAL);
}

async function cargarEstadosIniciales() {
  try {
    const pedidos = await api.listarPedidosHoy(RESTAURANTE_ID);
    for (const p of pedidos) {
      estadoCache.set(p.id, p.estado);
    }
    console.log(`📋 Cache inicial: ${estadoCache.size} pedidos cargados`);
  } catch (err) {
    console.error('Error cargando estados iniciales:', err);
  }
}

export function detenerPolling() {
  if (intervalId) {
    clearInterval(intervalId);
    intervalId = null;
    console.log('🔔 Polling detenido');
  }
}
