import pkg from 'whatsapp-web.js';
const { Client, LocalAuth } = pkg;
import qrcode from 'qrcode-terminal';
import { handleMenu } from './handlers/menuHandler.js';
import { handlePedido } from './handlers/pedidoHandler.js';
import { handleUbicacion, handleDetallesEntrega } from './handlers/ubicacionHandler.js';
import { getSession, resetSession } from './session/userSession.js';
import { iniciarPolling } from './services/notificaciones.js';

const client = new Client({
  authStrategy: new LocalAuth(),
  puppeteer: {
    headless: true,
    args: ['--no-sandbox', '--disable-setuid-sandbox'],
  },
});

console.log('🛵 Delivery Bot iniciando...');

client.on('qr', (qr: string) => {
  console.log('\n📱 Escanea este QR con WhatsApp:\n');
  qrcode.generate(qr, { small: true });
});

client.on('ready', () => {
  console.log('\n✅ Bot conectado y listo!');
  console.log('📞 Esperando mensajes...\n');

  // Iniciar polling de notificaciones
  iniciarPolling(async (phone, message) => {
    try {
      await client.sendMessage(phone, message);
    } catch (err) {
      console.error(`Error enviando mensaje a ${phone}:`, err);
    }
  });
});

client.on('authenticated', () => console.log('🔐 Sesión autenticada'));
client.on('auth_failure', (msg: string) => console.error('❌ Error de autenticación:', msg));
client.on('disconnected', (reason: string) => console.log('🔌 Desconectado:', reason));

client.on('message', async (msg: InstanceType<typeof pkg.Message>) => {
  try {
    if (msg.from.includes('@g.us')) return;
    if (msg.fromMe) return;

    const phone = msg.from.replace('@c.us', '');
    const session = getSession(phone);

    console.log(`📩 [${phone}] tipo=${msg.type} step=${session.step} body="${msg.body || ''}" hasLocation=${!!msg.location}`);

    // ── Mensaje de ubicación ──
    if (msg.location) {
      const loc = msg.location;
      const address = loc.description || (loc as Record<string, unknown>).address as string || undefined;
      console.log(`📍 [${phone}] lat=${loc.latitude} lng=${loc.longitude} address="${address || 'N/A'}"`);
      const reply = await handleUbicacion(phone, loc.latitude, loc.longitude, address);
      await msg.reply(reply);
      return;
    }

    // Solo texto
    if (msg.type !== 'chat' || !msg.body) return;

    const texto = msg.body.trim();
    const textoLower = texto.toLowerCase();

    // ── Cancelar (funciona en cualquier paso) ──
    if (['cancelar', 'cancel', 'anular'].includes(textoLower)) {
      resetSession(phone);
      await msg.reply('❌ Pedido cancelado. Escribe *menu* para empezar de nuevo.');
      return;
    }

    // ── Esperando datos de entrega ──
    if (session.step === 'waiting_details') {
      const reply = await handleDetallesEntrega(phone, texto);
      await msg.reply(reply);
      return;
    }

    // ── Esperando ubicación ──
    if (session.step === 'waiting_location') {
      await msg.reply(
        '📍 Necesitamos tu *ubicación GPS* para entregar el pedido.\n\n' +
        'Toca 📎 → *Ubicación* → Enviar ubicación actual.\n\n' +
        '_Escribe "cancelar" para anular el pedido._',
      );
      return;
    }

    // ── Comandos generales (solo en estado idle) ──

    if (['hola', 'hi', 'hello', 'inicio', 'empezar', 'ayuda', 'help'].includes(textoLower)) {
      await msg.reply(
        '👋 *¡Hola! Bienvenido a Delivery App* 🛵\n\n' +
        '¿Qué deseas hacer?\n\n' +
        '📋 Escribe *menu* para ver productos\n' +
        '🛒 Envía tu pedido: _"2 hamburguesas, 1 gaseosa"_\n' +
        '📍 Después de pedir, comparte tu *ubicación*\n' +
        '❌ Escribe *cancelar* en cualquier momento',
      );
      return;
    }

    if (['menu', 'menú', 'carta', 'productos', 'ver menu', 'ver menú'].includes(textoLower)) {
      const reply = await handleMenu(phone);
      await msg.reply(reply);
      return;
    }

    if (['estado', 'seguimiento', 'mi pedido'].includes(textoLower)) {
      if (session.pendingPedidoId) {
        await msg.reply(`📦 Tu pedido *#${session.pendingPedidoId}* está pendiente.\n\n📍 Envía tu ubicación para continuar.`);
      } else {
        await msg.reply('ℹ️ No tienes pedidos activos. Escribe *menu* para hacer uno.');
      }
      return;
    }

    // Intentar parsear como pedido
    const pedidoReply = await handlePedido(phone, texto);
    if (pedidoReply) {
      await msg.reply(pedidoReply);
      return;
    }

    // No se entendió
    await msg.reply(
      '🤔 No entendí tu mensaje.\n\n' +
      'Escribe *menu* para ver productos.\n' +
      'O envía tu pedido: _"2 hamburguesas, 1 gaseosa"_',
    );
  } catch (err) {
    console.error('Error procesando mensaje:', err);
  }
});

client.initialize();
