package org.delivery.application.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.delivery.application.dto.DetallePedidoRequest;
import org.delivery.application.dto.PedidoRequest;
import org.delivery.application.dto.WhatsAppMessage;
import org.delivery.application.port.WhatsAppPort;
import org.delivery.domain.entity.Producto;
import org.delivery.domain.entity.Restaurante;
import org.delivery.infrastructure.persistence.repository.ProductoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Caso de uso: procesar mensajes entrantes de WhatsApp.
 * Detecta pedidos básicos con regex y los crea vía PedidoService.
 * Resuelve el restaurante dinámicamente (SaaS ready).
 */
@Service
public class WhatsAppMessageService {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppMessageService.class);
    private static final Pattern PEDIDO_PATTERN =
            Pattern.compile("(\\d+)\\s+([a-záéíóúñ]+)", Pattern.CASE_INSENSITIVE);

    private final ProductoRepository productoRepository;
    private final PedidoService pedidoService;
    private final RestauranteService restauranteService;
    private final WhatsAppPort whatsAppPort;

    public WhatsAppMessageService(ProductoRepository productoRepository,
                                  PedidoService pedidoService,
                                  RestauranteService restauranteService,
                                  WhatsAppPort whatsAppPort) {
        this.productoRepository = productoRepository;
        this.pedidoService = pedidoService;
        this.restauranteService = restauranteService;
        this.whatsAppPort = whatsAppPort;
    }

    public void procesarMensaje(WhatsAppMessage message) {
        log.info("Mensaje de [{}]: {}", message.from(), message.body());

        // Resolver restaurante dinámicamente (SaaS: cada número de WhatsApp = 1 restaurante)
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
