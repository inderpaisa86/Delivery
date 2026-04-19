import pkg from 'whatsapp-web.js';
const { Client, LocalAuth } = pkg;
import qrcode from 'qrcode-terminal';
// @ts-ignore
import { handleMenu } from './handlers/menuHandler.js';
import { handlePedido, handleNombre, handleAddressConfirm } from './handlers/pedidoHandler.js';
import { handleUbicacion, handleDetallesEntrega, handleDireccionManual, handleContactPhone } from './handlers/ubicacionHandler.js';
import { getSession, resetSession, updateSession } from './session/userSession.js';
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

  iniciarPolling(async (phone, message) => {
    for (let attempt = 1; attempt <= 3; attempt++) {
      try {
        const numberId = await client.getNumberId(phone);
        if (numberId) {
          await client.sendMessage(numberId._serialized, message);
          return; // Éxito
        } else {
          console.warn(`⚠️ Número no encontrado en WhatsApp: ${phone}`);
          return;
        }
      } catch (err: any) {
        const isRetryable = err?.message?.includes('detached Frame') || err?.message?.includes('No LID');
        if (isRetryable && attempt < 3) {
          console.warn(`⚠️ Reintentando envío a ${phone} (intento ${attempt}/3)...`);
          await new Promise((r) => setTimeout(r, 2000 * attempt));
        } else {
          console.error(`❌ Error enviando a ${phone} (intento ${attempt}):`, err?.message || err);
          return;
        }
      }
    }
  }).catch((err: unknown) => console.error('Error iniciando polling:', err));
});

client.on('authenticated', () => console.log('🔐 Sesión autenticada'));
client.on('auth_failure', (msg: string) => console.error('❌ Error de autenticación:', msg));
client.on('disconnected', (reason: string) => console.log('🔌 Desconectado:', reason));

client.on('message', async (msg: any) => {
  try {
    if (msg.from.includes('@g.us')) return;
    if (msg.fromMe) return;

    let phone = msg.from.replace(/@.*$/, '');
    const originalFrom = phone;
    try {
      const contact = await msg.getContact();
      if (contact.number) phone = contact.number;
    } catch {}

    // Log para debug
    if (originalFrom !== phone) {
      console.log(`📱 [${originalFrom}] → número real: ${phone}`);
    }

    const session = getSession(phone);
    console.log(`📩 [${phone}] step=${session.step} tipo=${msg.type} body="${msg.body || ''}"`);

    // ── Ubicación GPS (funciona en waiting_location, waiting_location_choice, waiting_typed_address) ──
    if (msg.location) {
      const loc = msg.location;
      const address = loc.description || (loc as Record<string, unknown>).address as string || undefined;
      const reply = await handleUbicacion(phone, loc.latitude, loc.longitude, address);
      await msg.reply(reply);
      return;
    }

    if (msg.type !== 'chat' || !msg.body) {
      if (['image', 'sticker', 'video', 'audio', 'ptt', 'document'].includes(msg.type)) {
        await msg.reply(
          '📎 Solo puedo procesar *texto* y *ubicaciones*.\n\n' +
          'Escribe *menu* para ver productos o envía tu pedido:\n' +
          '_"2 hamburguesas, 1 gaseosa"_'
        );
      }
      return;
    }
    const texto = msg.body.trim();
    const textoLower = texto.toLowerCase();

    // ── Cancelar ──
    if (['cancelar', 'cancel', 'anular'].includes(textoLower)) {
      resetSession(phone);
      await msg.reply('❌ Pedido cancelado. Escribe *menu* para empezar de nuevo.');
      return;
    }

    // ── Esperando nombre ──
    if (session.step === 'waiting_name') {
      const reply = await handleNombre(phone, texto);
      await msg.reply(reply);
      return;
    }

    // ── Esperando confirmación de dirección ──
    if (session.step === 'waiting_address_confirm') {
      const reply = await handleAddressConfirm(phone, texto);
      await msg.reply(reply);
      return;
    }

    // ── Esperando elección: GPS o manual ──
    if (session.step === 'waiting_location_choice') {
      if (textoLower === '1' || textoLower === 'gps' || textoLower === 'ubicacion' || textoLower === 'ubicación') {
        updateSession(phone, { step: 'waiting_location' });
        await msg.reply('📍 Envía tu *ubicación GPS*:\n\nToca 📎 → *Ubicación* → Enviar ubicación actual.');
        return;
      }
      if (textoLower === '2' || textoLower === 'manual' || textoLower === 'escribir' || textoLower === 'direccion' || textoLower === 'dirección') {
        updateSession(phone, { step: 'waiting_typed_address' });
        await msg.reply('📝 Escribe tu *dirección completa*:\n\n_Incluye barrio, edificio/casa, torre, apto, ciudad y referencias._');
        return;
      }
      await msg.reply('Escribe *1* para enviar ubicación GPS o *2* para escribir la dirección.');
      return;
    }

    // ── Esperando ubicación GPS ──
    if (session.step === 'waiting_location') {
      await msg.reply('📍 Necesitamos tu *ubicación GPS*.\n\nToca 📎 → *Ubicación* → Enviar ubicación actual.\n\n_O escribe "2" para escribir la dirección manualmente._');
      // Permitir cambiar a manual
      if (textoLower === '2' || textoLower === 'manual') {
        updateSession(phone, { step: 'waiting_typed_address' });
        await msg.reply('📝 Escribe tu *dirección completa*:');
      }
      return;
    }

    // ── Esperando dirección escrita ──
    if (session.step === 'waiting_typed_address') {
      const reply = await handleDireccionManual(phone, texto);
      await msg.reply(reply);
      return;
    }

    // ── Esperando datos adicionales (después de GPS) ──
    if (session.step === 'waiting_details') {
      const reply = await handleDetallesEntrega(phone, texto);
      await msg.reply(reply);
      return;
    }

    // ── Esperando teléfono de contacto ──
    if (session.step === 'waiting_contact_phone') {
      const reply = await handleContactPhone(phone, texto);
      await msg.reply(reply);
      return;
    }

    // ── Comandos generales ──
    if (['hola', 'hi', 'hello', 'inicio', 'empezar', 'ayuda', 'help'].includes(textoLower)) {
      await msg.reply(
        '👋 *¡Hola! Bienvenido a Delivery App* 🛵\n\n' +
        '📋 Escribe *menu* para ver productos\n' +
        '🛒 Envía tu pedido: _"2 hamburguesas, 1 gaseosa"_\n' +
        '❌ Escribe *cancelar* en cualquier momento',
      );
      return;
    }

    if (['menu', 'menú', 'carta', 'productos', 'ver menu', 'ver menú'].includes(textoLower)) {
      await msg.reply(await handleMenu(phone));
      return;
    }

    // ── Intentar parsear como pedido ──
    const pedidoReply = await handlePedido(phone, texto);
    if (pedidoReply) {
      await msg.reply(pedidoReply);
      return;
    }

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
