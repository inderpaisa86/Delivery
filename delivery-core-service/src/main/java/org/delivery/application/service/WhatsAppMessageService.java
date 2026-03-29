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

    public WhatsAppMessageService(
            IProductoRepository productoRepository,
            PedidoService pedidoService,
            RestauranteService restauranteService,
            IWhatsAppPort whatsAppPort,
            @Value("${whatsapp.verify.token}") String verifyToken) {
        this.productoRepository = productoRepository;
        this.pedidoService = pedidoService;
        this.restauranteService = restauranteService;
        this.whatsAppPort = whatsAppPort;
        this.verifyToken = verifyToken;
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
        log.info("Mensaje de [{}]: {}", message.from(), message.body());

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
                    message.from(), null, "Pendiente por confirmar",
                    null, null, detalles);

            PedidoResponse response = pedidoService.crearPedido(request);

            // Enviar resumen del pedido al cliente
            String resumen = generarResumenPedido(response);
            whatsAppPort.enviarMensaje(message.from(), resumen);

            log.info("Pedido #{} creado desde WhatsApp para {} (restaurante: {})",
                    response.id(), message.from(), restaurante.getNombre());
        } catch (Exception e) {
            log.error("Error creando pedido desde WhatsApp: {}", e.getMessage());
            whatsAppPort.enviarMensaje(message.from(),
                    "⚠️ No pudimos procesar tu pedido. Intenta de nuevo.");
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
        sb.append("\n\nTe notificaremos cuando esté listo. 🍽️");
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

            productoRepository.findByNombreIgnoreCase(nombreProducto)
                    .ifPresent(p -> detalles.add(new DetallePedidoRequest(p.getId(), cantidad)));
        }

        return detalles;
    }
}
