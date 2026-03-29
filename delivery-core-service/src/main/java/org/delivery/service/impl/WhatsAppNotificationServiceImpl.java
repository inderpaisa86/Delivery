package org.delivery.service.impl;

import java.util.Map;

import org.delivery.domain.enums.EstadoPedido;
import org.delivery.service.WhatsAppNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Envía notificaciones al cliente vía WhatsApp Business API.
 */
@Service
public class WhatsAppNotificationServiceImpl implements WhatsAppNotificationService {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppNotificationServiceImpl.class);

    private final WebClient webClient;
    private final String phoneNumberId;

    public WhatsAppNotificationServiceImpl(
            @Value("${whatsapp.api.url}") String apiUrl,
            @Value("${whatsapp.api.token}") String apiToken,
            @Value("${whatsapp.api.phone-number-id}") String phoneNumberId) {
        this.phoneNumberId = phoneNumberId;
        this.webClient = WebClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader("Authorization", "Bearer " + apiToken)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Override
    public void notificarCambioEstado(String telefono, Long pedidoId, EstadoPedido estado) {
        String mensaje = generarMensaje(pedidoId, estado);
        enviarMensaje(telefono, mensaje);
    }

    @Override
    public void enviarMensaje(String telefono, String mensaje) {
        if (phoneNumberId == null || phoneNumberId.isBlank()) {
            log.warn("WhatsApp API no configurada. Mensaje no enviado a {}: {}", telefono, mensaje);
            return;
        }

        try {
            Map<String, Object> body = Map.of(
                    "messaging_product", "whatsapp",
                    "to", telefono,
                    "type", "text",
                    "text", Map.of("body", mensaje)
            );

            webClient.post()
                    .uri("/{phoneNumberId}/messages", phoneNumberId)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .doOnSuccess(r -> log.info("Notificación enviada a {}", telefono))
                    .doOnError(e -> log.error("Error enviando notificación a {}: {}", telefono, e.getMessage()))
                    .subscribe();
        } catch (Exception e) {
            log.error("Error preparando notificación a {}: {}", telefono, e.getMessage());
        }
    }

    private String generarMensaje(Long pedidoId, EstadoPedido estado) {
        return switch (estado) {
            case NUEVO -> String.format("🛒 Pedido #%d recibido. Estamos procesándolo.", pedidoId);
            case CONFIRMADO -> String.format("✅ Pedido #%d confirmado por el restaurante.", pedidoId);
            case PREPARANDO -> String.format("👨‍🍳 Tu pedido #%d se está preparando.", pedidoId);
            case LISTO -> String.format("📦 Tu pedido #%d está listo para envío.", pedidoId);
            case EN_CAMINO -> String.format("🏍️ Tu pedido #%d va en camino.", pedidoId);
            case ENTREGADO -> String.format("🎉 Tu pedido #%d fue entregado. ¡Buen provecho!", pedidoId);
            case CANCELADO -> String.format("❌ Tu pedido #%d fue cancelado.", pedidoId);
        };
    }
}
