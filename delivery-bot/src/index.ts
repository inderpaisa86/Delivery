import pkg from 'whatsapp-web.js';
const { Client, LocalAuth } = pkg;
import qrcode from 'qrcode-terminal';
import { handleMenu } from './handlers/menuHandler.js';
import { handlePedido } from './handlers/pedidoHandler.js';
import { handleUbicacion } from './handlers/ubicacionHandler.js';
import { getSession } from './session/userSession.js';

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
});

client.on('authenticated', () => {
  console.log('🔐 Sesión autenticada');
});

client.on('auth_failure', (msg: string) => {
  console.error('❌ Error de autenticación:', msg);
});

client.on('disconnected', (reason: string) => {
  console.log('🔌 Desconectado:', reason);
});

client.on('message', async (msg: InstanceType<typeof pkg.Message>) => {
  try {
    // Ignorar mensajes de grupos y propios
    if (msg.from.includes('@g.us')) return;
    if (msg.fromMe) return;

    const phone = msg.from.replace('@c.us', '');
    const session = getSession(phone);

    console.log(`📩 [${phone}] ${msg.type}: ${msg.body || '(ubicación)'}`);

    // ── Mensaje de ubicación ──
    if (msg.type === 'location' || msg.location) {
      const loc = msg.location;
      if (loc) {
        const reply = await handleUbicacion(
          phone,
          loc.latitude,
          loc.longitude,
          loc.description || undefined,
        );
        await msg.reply(reply);
      }
      return;
    }

    // ── Mensajes de texto ──
    if (msg.type !== 'chat') return;

    const texto = msg.body.trim().toLowerCase();

    // Comando: hola / inicio
    if (['hola', 'hi', 'inicio', 'empezar'].includes(texto)) {
      await msg.reply(
        '👋 *¡Hola! Bienvenido a Delivery App* 🛵\n\n' +
        'Escribe *menu* para ver los productos disponibles.\n' +
        'O envía tu pedido directamente:\n' +
        '_"2 hamburguesas, 1 gaseosa"_',
      );
      return;
    }

    // Comando: menu
    if (['menu', 'menú', 'carta', 'productos'].includes(texto)) {
      const reply = await handleMenu(phone);
      await msg.reply(reply);
      return;
    }

    // Si está esperando ubicación, recordarle
    if (session.step === 'waiting_location') {
      await msg.reply(
        '📍 Necesitamos tu ubicación para entregar el pedido.\n\n' +
        'Toca 📎 → Ubicación → Enviar ubicación actual.\n\n' +
        '_Si quieres cancelar, escribe "cancelar"._',
      );
      return;
    }

    // Comando: cancelar
    if (texto === 'cancelar') {
      const { resetSession } = await import('./session/userSession.js');
      resetSession(phone);
      await msg.reply('❌ Pedido cancelado. Escribe *menu* para empezar de nuevo.');
      return;
    }

    // Intentar parsear como pedido
    const pedidoReply = await handlePedido(phone, msg.body);
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
