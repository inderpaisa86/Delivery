package org.delivery.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.delivery.application.dto.DetallePedidoRequest;
import org.delivery.application.dto.PedidoRequest;
import org.delivery.application.dto.WhatsAppMessage;
import org.delivery.application.port.WhatsAppPort;
import org.delivery.domain.entity.Restaurante;
import org.delivery.infrastructure.persistence.repository.ProductoRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class WhatsAppMessageService {

    private static final int MAX_INPUT_LENGTH = 500;

    private final ProductoRepository productoRepository;
    private final PedidoService pedidoService;
    private final RestauranteService restauranteService;
    private final WhatsAppPort whatsAppPort;

    public void procesarMensaje(WhatsAppMessage message) {
        log.info("Mensaje de [{}]: {}", message.from(), message.body());

        Restaurante restaurante;
        try {
            restaurante = restauranteService.resolverPorTelefonoWhatsApp(message.from());
        } catch (Exception e) {
            log.error("No se pudo resolver restaurante para mensaje de {}", message.from());
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

            var response = pedidoService.crearPedido(request);
            log.info("Pedido #{} creado desde WhatsApp para {} (restaurante: {})",
                    response.id(), message.from(), restaurante.getNombre());
        } catch (Exception e) {
            log.error("Error creando pedido desde WhatsApp: {}", e.getMessage());
            whatsAppPort.enviarMensaje(message.from(),
                    "⚠️ No pudimos procesar tu pedido. Intenta de nuevo.");
        }
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
