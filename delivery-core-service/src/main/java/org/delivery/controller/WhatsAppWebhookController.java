package org.delivery.controller;

import org.delivery.dto.WhatsAppMessage;
import org.delivery.parser.WhatsAppPayloadParser;
import org.delivery.service.WhatsAppMessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller del webhook de WhatsApp Business.
 *
 * Responsabilidad única: recibir HTTP y delegar.
 * No contiene lógica de negocio ni parseo complejo.
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
            messages.forEach(messageService::processMessage);
        } catch (Exception e) {
            log.error("Error procesando mensaje: {}", e.getMessage());
        }

        return ResponseEntity.ok().build();
    }
}
