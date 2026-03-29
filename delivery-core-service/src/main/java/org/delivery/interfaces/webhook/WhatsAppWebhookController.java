package org.delivery.interfaces.webhook;

import java.util.List;
import java.util.Map;

import org.delivery.application.dto.WhatsAppMessage;
import org.delivery.application.service.WhatsAppMessageService;
import org.delivery.infrastructure.external.whatsapp.WhatsAppPayloadParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Webhook de WhatsApp Business.
 * Solo recibe HTTP y delega. Cero lógica de negocio.
 */
@RestController
@RequestMapping("/webhook")
public class WhatsAppWebhookController {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppWebhookController.class);

    private final String verifyToken;
    private final WhatsAppMessageService messageService;

    public WhatsAppWebhookController(
            @Value("${whatsapp.verify.token}") String verifyToken,
            WhatsAppMessageService messageService) {
        this.verifyToken = verifyToken;
        this.messageService = messageService;
    }

    @GetMapping
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
