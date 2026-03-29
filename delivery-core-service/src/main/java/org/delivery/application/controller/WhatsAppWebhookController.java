package org.delivery.application.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.delivery.application.service.WhatsAppMessageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/webhook")
@Tag(name = "Webhook", description = "Webhook de WhatsApp Business API")
@RequiredArgsConstructor
public class WhatsAppWebhookController {

    private final WhatsAppMessageService messageService;

    @GetMapping
    @Operation(summary = "Verificación de Meta")
    public ResponseEntity<String> verify(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.verify_token") String token,
            @RequestParam("hub.challenge") String challenge) {
        return messageService.verificarWebhook(mode, token, challenge);
    }

    @PostMapping
    @Operation(summary = "Recibir mensajes de WhatsApp")
    public ResponseEntity<Void> receiveMessage(@RequestBody Map<String, Object> payload) {
        messageService.procesarPayload(payload);
        return ResponseEntity.ok().build();
    }
}
