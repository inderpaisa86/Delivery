package org.delivery.application.service;

import org.delivery.application.dto.DetallePedidoRequest;
import org.delivery.application.dto.PedidoRequest;
import org.delivery.application.dto.WhatsAppMessage;
import org.delivery.application.port.WhatsAppPort;
import org.delivery.domain.entity.Producto;
import org.delivery.infrastructure.persistence.repository.ProductoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Caso de uso: procesar mensajes entrantes de WhatsApp.
 * Detecta pedidos básicos con regex y los crea vía PedidoService.
 */
@Service
public class WhatsAppMessageService {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppMessageService.class);
    private static final Pattern PEDIDO_PATTERN =
            Pattern.compile("(\\d+)\\s+([a-záéíóúñ]+)", Pattern.CASE_INSENSITIVE);

    private final ProductoRepository productoRepository;
    private final PedidoService pedidoService;
    private final WhatsAppPort whatsAppPort;

    /**
     * ID del restaurante por defecto para pedidos vía WhatsApp.
     * En producción esto se resolvería por el número de teléfono del restaurante.
     */
    private static final Long DEFAULT_RESTAURANTE_ID = 1L;

    public WhatsAppMessageService(ProductoRepository productoRepository,
                                  PedidoService pedidoService,
                                  WhatsAppPort whatsAppPort) {
        this.productoRepository = productoRepository;
        this.pedidoService = pedidoService;
        this.whatsAppPort = whatsAppPort;
    }

    public void procesarMensaje(WhatsAppMessage message) {
        log.info("Mensaje de [{}]: {}", message.from(), message.body());

        List<DetallePedidoRequest> detalles = parsearPedido(message.body());

        if (detalles.isEmpty()) {
            whatsAppPort.enviarMensaje(message.from(),
                    "👋 Hola! Envía tu pedido así: \"2 hamburguesas, 1 pizza\"\n" +
                    "Escribe \"menu\" para ver productos disponibles.");
            return;
        }

        try {
            PedidoRequest request = new PedidoRequest(
                    DEFAULT_RESTAURANTE_ID,
                    message.from(), null, "Pendiente por confirmar",
                    null, null, detalles);

            var response = pedidoService.crearPedido(request);
            log.info("Pedido #{} creado desde WhatsApp para {}", response.id(), message.from());
        } catch (Exception e) {
            log.error("Error creando pedido desde WhatsApp: {}", e.getMessage());
            whatsAppPort.enviarMensaje(message.from(),
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
