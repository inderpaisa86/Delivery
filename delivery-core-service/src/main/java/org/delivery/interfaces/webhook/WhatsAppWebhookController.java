package org.delivery.interfaces.webhook;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.delivery.application.dto.WhatsAppMessage;
import org.delivery.application.service.WhatsAppMessageService;
import org.delivery.infrastructure.external.whatsapp.WhatsAppPayloadParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/webhook")
@Tag(name = "Webhook", description = "Webhook de WhatsApp Business API")
@Slf4j
public class WhatsAppWebhookController {

    private final String verifyToken;
    private final WhatsAppMessageService messageService;

    public WhatsAppWebhookController(
            @Value("${whatsapp.verify.token}") String verifyToken,
            WhatsAppMessageService messageService) {
        this.verifyToken = verifyToken;
        this.messageService = messageService;
    }

    @GetMapping
    @Operation(summary = "Verificación de Meta")
    public ResponseEntity<String> verify(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.verify_token") String token,
            @RequestParam("hub.challenge") String challenge) {

        if ("subscribe".equals(mode) && verifyToken.equals(token)) {
            log.info("Webhook verificado correctamente");
            return ResponseEntity.ok(challenge);
        }

        log.warn("Verificación fallida - token inválido");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Token inválido");
    }

    @PostMapping
    @Operation(summary = "Recibir mensajes de WhatsApp")
    public ResponseEntity<Void> receiveMessage(@RequestBody Map<String, Object> payload) {
        try {
            List<WhatsAppMessage> messages = WhatsAppPayloadParser.extractMessages(payload);
            messages.forEach(messageService::procesarMensaje);
        } catch (Exception e) {
            log.error("Error procesando mensaje: {}", e.getMessage());
        }

        return ResponseEntity.ok().build();
    }
}
