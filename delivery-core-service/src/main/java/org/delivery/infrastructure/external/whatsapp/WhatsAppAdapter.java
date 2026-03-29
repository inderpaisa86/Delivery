package org.delivery.infrastructure.external.whatsapp;

import lombok.extern.slf4j.Slf4j;
import org.delivery.application.port.WhatsAppPort;
import org.delivery.domain.enums.EstadoPedido;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Component
@Slf4j
public class WhatsAppAdapter implements WhatsAppPort {

    private final WebClient webClient;
    private final String phoneNumberId;

    public WhatsAppAdapter(
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
        enviarMensaje(telefono, generarMensaje(pedidoId, estado));
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
                    "text", Map.of("body", mensaje));

            webClient.post()
                    .uri("/{phoneNumberId}/messages", phoneNumberId)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .doOnSuccess(r -> log.info("Notificación enviada a {}", telefono))
                    .doOnError(e -> log.error("Error enviando a {}: {}", telefono, e.getMessage()))
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
