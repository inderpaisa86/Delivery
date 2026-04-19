import { api, type PedidoResponse } from './apiClient.js';

const RESTAURANTE_ID = Number(process.env.RESTAURANTE_ID || '1');
const ENV = process.env.NODE_ENV || 'development';
const POLL_INTERVAL = ENV === 'production'
  ? Number(process.env.POLL_INTERVAL_MS || '300000')
  : Number(process.env.POLL_INTERVAL_MS || '10000');

const TRACKING_URL = process.env.TRACKING_URL || 'http://localhost:5173';

const estadoCache = new Map<number, string>();
let cacheReady = false;

const MENSAJES: Record<string, (p: PedidoResponse) => string> = {
  CONFIRMADO: (p) =>
    `✅ *Pedido #${p.numeroDiario ?? p.id} confirmado*\n\nEl restaurante confirmó tu pedido. Están preparándolo pronto.`,
  PREPARANDO: (p) =>
    `👨‍🍳 *Pedido #${p.numeroDiario ?? p.id} en preparación*\n\nTu pedido se está preparando. ¡Ya falta poco!`,
  LISTO: (p) =>
    `📦 *Pedido #${p.numeroDiario ?? p.id} listo*\n\nTu pedido está listo. Un domiciliario lo recogerá pronto.`,
  EN_CAMINO: (p) =>
    `🛵 *Pedido #${p.numeroDiario ?? p.id} en camino*\n\nTu pedido va en camino a tu dirección.\n\n🔗 Seguimiento: ${TRACKING_URL}/track/${p.trackingToken}`,
  ENTREGADO: (p) =>
    `🏁 *Pedido #${p.numeroDiario ?? p.id} entregado*\n\n¡Tu pedido fue entregado! Gracias por tu compra. 🙌\n\nEscribe *menu* para pedir de nuevo.`,
  CANCELADO: (p) =>
    `❌ *Pedido #${p.numeroDiario ?? p.id} cancelado*\n\nTu pedido fue cancelado. Si fue un error, escribe *menu* para hacer uno nuevo.`,
};

type SendMessageFn = (phone: string, message: string) => Promise<void>;

let intervalId: ReturnType<typeof setInterval> | null = null;

/** Normaliza el teléfono: quita sufijos de WhatsApp y caracteres no numéricos */
function toWhatsAppId(telefono: string): string {
  const clean = telefono.replace(/@.*$/, '').replace(/\D/g, '');
  return `${clean}@c.us`;
}

export async function iniciarPolling(sendMessage: SendMessageFn) {
  console.log(`🔔 Notificaciones activas — polling cada ${POLL_INTERVAL / 1000}s (${ENV})`);

  // Cargar cache inicial y esperar a que termine
  await cargarEstadosIniciales();

  intervalId = setInterval(async () => {
    if (!cacheReady) return;

    try {
      const pedidos = await api.listarPedidosHoy(RESTAURANTE_ID);

      for (const pedido of pedidos) {
        const estadoAnterior = estadoCache.get(pedido.id);
        const estadoActual = pedido.estado;

        if (estadoAnterior === undefined) {
          estadoCache.set(pedido.id, estadoActual);
          continue;
        }

        if (estadoAnterior !== estadoActual) {
          estadoCache.set(pedido.id, estadoActual);

          const generarMensaje = MENSAJES[estadoActual];
          if (generarMensaje && pedido.clienteTelefono) {
            const mensaje = generarMensaje(pedido);
            const whatsappId = toWhatsAppId(pedido.clienteTelefono);

            console.log(`🔔 Notificando [${pedido.clienteTelefono}] → ${whatsappId} | pedido #${pedido.id}: ${estadoAnterior} → ${estadoActual}`);

            try {
              await sendMessage(whatsappId, mensaje);
              console.log(`✅ Notificación enviada a ${pedido.clienteTelefono}`);
            } catch (err) {
              console.error(`❌ Error enviando notificación a ${pedido.clienteTelefono}:`, err);
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
    cacheReady = true;
    console.log(`📋 Cache inicial: ${estadoCache.size} pedidos cargados`);
  } catch (err) {
    console.error('Error cargando estados iniciales:', err);
    cacheReady = true; // Permitir que siga aunque falle
  }
}

export function detenerPolling() {
  if (intervalId) {
    clearInterval(intervalId);
    intervalId = null;
    console.log('🔔 Polling detenido');
  }
}
