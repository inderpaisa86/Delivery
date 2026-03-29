package org.delivery.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.delivery.domain.Producto;
import org.delivery.dto.DetallePedidoRequest;
import org.delivery.dto.PedidoRequest;
import org.delivery.dto.WhatsAppMessage;
import org.delivery.repository.ProductoRepository;
import org.delivery.service.PedidoService;
import org.delivery.service.WhatsAppMessageService;
import org.delivery.service.WhatsAppNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Procesa mensajes entrantes de WhatsApp.
 * Detecta pedidos básicos (ej: "2 hamburguesas, 1 pizza") y los crea.
 */
@Service
public class WhatsAppMessageServiceImpl implements WhatsAppMessageService {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppMessageServiceImpl.class);

    // Patrón: "2 hamburguesas" o "1 pizza"
    private static final Pattern PEDIDO_PATTERN = Pattern.compile("(\\d+)\\s+([a-záéíóúñ]+)", Pattern.CASE_INSENSITIVE);

    private final ProductoRepository productoRepository;
    private final PedidoService pedidoService;
    private final WhatsAppNotificationService notificationService;

    public WhatsAppMessageServiceImpl(ProductoRepository productoRepository,
                                      PedidoService pedidoService,
                                      WhatsAppNotificationService notificationService) {
        this.productoRepository = productoRepository;
        this.pedidoService = pedidoService;
        this.notificationService = notificationService;
    }

    @Override
    public void processMessage(WhatsAppMessage message) {
        log.info("Mensaje recibido de [{}]: {}", message.from(), message.body());

        List<DetallePedidoRequest> detalles = parsearPedido(message.body());

        if (detalles.isEmpty()) {
            notificationService.enviarMensaje(message.from(),
                    "👋 Hola! Envía tu pedido así: \"2 hamburguesas, 1 pizza\"\n" +
                    "Escribe \"menu\" para ver los productos disponibles.");
            return;
        }

        try {
            PedidoRequest request = new PedidoRequest(
                    message.from(), null, "Pendiente por confirmar",
                    null, null, detalles);

            var response = pedidoService.crearPedido(request);
            log.info("Pedido #{} creado desde WhatsApp para {}", response.id(), message.from());
        } catch (Exception e) {
            log.error("Error creando pedido desde WhatsApp: {}", e.getMessage());
            notificationService.enviarMensaje(message.from(),
                    "⚠️ No pudimos procesar tu pedido. Intenta de nuevo.");
        }
    }

    private List<DetallePedidoRequest> parsearPedido(String texto) {
        List<DetallePedidoRequest> detalles = new ArrayList<>();
        Matcher matcher = PEDIDO_PATTERN.matcher(texto);

        while (matcher.find()) {
            int cantidad = Integer.parseInt(matcher.group(1));
            String nombreProducto = matcher.group(2);

            Optional<Producto> producto = productoRepository.findByNombreIgnoreCase(nombreProducto);
            producto.ifPresent(p -> detalles.add(new DetallePedidoRequest(p.getId(), cantidad)));
        }

        return detalles;
    }
}
