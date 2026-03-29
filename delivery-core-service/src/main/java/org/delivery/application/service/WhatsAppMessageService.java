package org.delivery.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.delivery.application.dto.DetallePedidoRequest;
import org.delivery.application.dto.PedidoRequest;
import org.delivery.application.dto.PedidoResponse;
import org.delivery.application.dto.WhatsAppMessage;
import org.delivery.application.port.IWhatsAppPort;
import org.delivery.domain.entity.Producto;
import org.delivery.domain.entity.Restaurante;
import org.delivery.infrastructure.external.whatsapp.WhatsAppPayloadParser;
import org.delivery.infrastructure.persistence.repository.IProductoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class WhatsAppMessageService {

    private static final int MAX_INPUT_LENGTH = 500;

    private final IProductoRepository productoRepository;
    private final PedidoService pedidoService;
    private final RestauranteService restauranteService;
    private final IWhatsAppPort whatsAppPort;
    private final String verifyToken;
    private final String trackingBaseUrl;

    public WhatsAppMessageService(
            IProductoRepository productoRepository,
            PedidoService pedidoService,
            RestauranteService restauranteService,
            IWhatsAppPort whatsAppPort,
            @Value("${whatsapp.verify.token}") String verifyToken,
            @Value("${app.tracking.base-url:http://localhost:5173}") String trackingBaseUrl) {
        this.productoRepository = productoRepository;
        this.pedidoService = pedidoService;
        this.restauranteService = restauranteService;
        this.whatsAppPort = whatsAppPort;
        this.verifyToken = verifyToken;
        this.trackingBaseUrl = trackingBaseUrl;
    }

    public ResponseEntity<String> verificarWebhook(String mode, String token, String challenge) {
        if ("subscribe".equals(mode) && verifyToken.equals(token)) {
            log.info("Webhook verificado correctamente");
            return ResponseEntity.ok(challenge);
        }
        log.warn("Verificación fallida - token inválido");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Token inválido");
    }

    public void procesarPayload(Map<String, Object> payload) {
        try {
            List<WhatsAppMessage> messages = WhatsAppPayloadParser.extractMessages(payload);
            messages.forEach(this::procesarMensaje);
        } catch (Exception e) {
            log.error("Error procesando mensaje: {}", e.getMessage());
        }
    }

    public void procesarMensaje(WhatsAppMessage message) {
        log.info("Mensaje de [{}] tipo [{}]: {}", message.from(), message.type(),
                message.isText() ? message.body() : "ubicación");

        // Si es un mensaje de ubicación, actualizar pedido pendiente
        if (message.isLocation()) {
            procesarUbicacion(message);
            return;
        }

        Restaurante restaurante;
        try {
            restaurante = restauranteService.resolverPorTelefonoWhatsApp(message.from());
        } catch (Exception e) {
            log.error("No se pudo resolver restaurante para mensaje de {}", message.from());
            return;
        }

        String texto = message.body().trim().toLowerCase();

        // Comando "menu"
        if ("menu".equals(texto) || "menú".equals(texto)) {
            enviarMenu(message.from(), restaurante.getId());
            return;
        }

        List<DetallePedidoRequest> detalles = parsearPedido(message.body());

        if (detalles.isEmpty()) {
            whatsAppPort.enviarMensaje(message.from(),
                    """
                    👋 Hola! Envía tu pedido así: "2 hamburguesas, 1 pizza"
                    Escribe "menu" para ver productos disponibles.""");
            return;
        }

        try {
            PedidoRequest request = new PedidoRequest(
                    restaurante.getId(),
                    message.from(), null, "Pendiente ubicación",
                    null, null, detalles);

            PedidoResponse response = pedidoService.crearPedido(request);

            // Enviar resumen y pedir ubicación
            String resumen = generarResumenPedido(response);
            whatsAppPort.enviarMensaje(message.from(), resumen);
            whatsAppPort.enviarMensaje(message.from(),
                    "📍 Ahora envíanos tu *ubicación* para saber dónde entregar.\n\n" +
                    "Toca el ícono 📎 → Ubicación → Enviar tu ubicación actual.");

            log.info("Pedido #{} creado desde WhatsApp para {} (restaurante: {}). Esperando ubicación.",
                    response.id(), message.from(), restaurante.getNombre());
        } catch (Exception e) {
            log.error("Error creando pedido desde WhatsApp: {}", e.getMessage());
            whatsAppPort.enviarMensaje(message.from(),
                    "⚠️ No pudimos procesar tu pedido. Intenta de nuevo.");
        }
    }

    private void procesarUbicacion(WhatsAppMessage message) {
        // Usar la dirección de WhatsApp si viene, sino generar una con coordenadas
        String direccion = message.address() != null
                ? message.address()
                : String.format("Lat: %.6f, Lng: %.6f", message.latitude(), message.longitude());

        boolean updated = pedidoService.actualizarUbicacionPedido(
                message.from(), message.latitude(), message.longitude(), direccion);

        if (updated) {
            whatsAppPort.enviarMensaje(message.from(),
                    "✅ Ubicación recibida: " + direccion +
                    "\nTu pedido está siendo procesado. ¡Te avisaremos cuando esté listo! 🍽️");
            log.info("Ubicación recibida de {}: {} ({}, {})",
                    message.from(), direccion, message.latitude(), message.longitude());
        } else {
            whatsAppPort.enviarMensaje(message.from(),
                    "ℹ️ No tienes pedidos pendientes de ubicación. Envía tu pedido primero.");
        }
    }

    private void enviarMenu(String telefono, Long restauranteId) {
        List<Producto> productos = productoRepository.findByActivoTrueAndRestauranteId(restauranteId);

        if (productos.isEmpty()) {
            whatsAppPort.enviarMensaje(telefono, "📋 No hay productos disponibles en este momento.");
            return;
        }

        String menu = "📋 *Menú disponible:*\n\n" +
                productos.stream()
                        .map(p -> String.format("• %s - $%,.0f", p.getNombre(), p.getPrecio()))
                        .collect(Collectors.joining("\n")) +
                "\n\nEnvía tu pedido: \"2 hamburguesas, 1 gaseosa\"";

        whatsAppPort.enviarMensaje(telefono, menu);
    }

    private String generarResumenPedido(PedidoResponse response) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("✅ *Pedido #%d recibido*\n\n", response.id()));

        for (PedidoResponse.DetalleResponse d : response.detalles()) {
            sb.append(String.format("• %dx %s - $%,.0f\n", d.cantidad(), d.producto(), d.precio()));
        }

        sb.append(String.format("\n💰 *Total: $%,.0f*", response.total()));
        sb.append(String.format("\n\n🔎 *Número de seguimiento:* %d", response.id()));
        sb.append(String.format("\n🔗 *Tracking:* %s/track/%s", trackingBaseUrl, response.trackingToken()));
        return sb.toString();
    }

    private List<DetallePedidoRequest> parsearPedido(String texto) {
        if (texto == null || texto.length() > MAX_INPUT_LENGTH) {
            return List.of();
        }

        List<DetallePedidoRequest> detalles = new ArrayList<>();
        String[] segmentos = texto.split(",");

        for (String segmento : segmentos) {
            String trimmed = segmento.trim();
            int spaceIndex = trimmed.indexOf(' ');
            if (spaceIndex <= 0) continue;

            String cantidadStr = trimmed.substring(0, spaceIndex);
            String nombreProducto = trimmed.substring(spaceIndex + 1).trim();

            if (cantidadStr.isEmpty() || nombreProducto.isEmpty()) continue;
            if (!cantidadStr.chars().allMatch(Character::isDigit)) continue;

            int cantidad;
            try {
                cantidad = Integer.parseInt(cantidadStr);
            } catch (NumberFormatException e) {
                continue;
            }

            if (cantidad <= 0 || cantidad > 100) continue;

            buscarProducto(nombreProducto)
                    .ifPresent(p -> detalles.add(new DetallePedidoRequest(p.getId(), cantidad)));
        }

        return detalles;
    }

    /**
     * Busca un producto por nombre con tolerancia a plurales.
     * Intenta: nombre exacto → singular (sin s/es) → búsqueda parcial.
     */
    private java.util.Optional<Producto> buscarProducto(String nombre) {
        // 1. Búsqueda exacta
        var result = productoRepository.findByNombreIgnoreCase(nombre);
        if (result.isPresent()) return result;

        // 2. Normalizar plural → singular en español
        String singular = normalizarPlural(nombre);
        if (!singular.equals(nombre)) {
            result = productoRepository.findByNombreIgnoreCase(singular);
            if (result.isPresent()) return result;
        }

        // 3. Búsqueda parcial (LIKE %nombre%)
        result = productoRepository.findFirstByNombreContainingIgnoreCaseAndActivoTrue(singular);
        if (result.isPresent()) return result;

        // 4. Búsqueda parcial con el nombre original
        return productoRepository.findFirstByNombreContainingIgnoreCaseAndActivoTrue(nombre);
    }

    /**
     * Normaliza plurales comunes del español a singular.
     * hamburguesas → hamburguesa, pizzas → pizza, gaseosas → gaseosa
     * jugos → jugo, perros → perro
     */
    private String normalizarPlural(String palabra) {
        String lower = palabra.toLowerCase();

        // Palabras terminadas en "ces" → "z" (ej: arroces → arroz)
        if (lower.endsWith("ces")) {
            return palabra.substring(0, palabra.length() - 3) + "z";
        }
        // Palabras terminadas en "es" (pero no "ses") → quitar "es" (ej: hamburgueses no aplica)
        // Palabras terminadas en "es" después de consonante → quitar "es"
        if (lower.endsWith("es") && lower.length() > 3) {
            char previa = lower.charAt(lower.length() - 3);
            // Si antes de "es" hay consonante (no vocal), quitar "es"
            if (!esVocal(previa)) {
                return palabra.substring(0, palabra.length() - 2);
            }
        }
        // Palabras terminadas en "s" → quitar "s" (caso más común)
        if (lower.endsWith("s") && lower.length() > 2) {
            return palabra.substring(0, palabra.length() - 1);
        }

        return palabra;
    }

    private boolean esVocal(char c) {
        return "aeiouáéíóú".indexOf(Character.toLowerCase(c)) >= 0;
    }
}
