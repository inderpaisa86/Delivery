package org.delivery.dto;

/**
 * Representa un mensaje extraído del webhook de WhatsApp Business.
 *
 * @param from   número del remitente
 * @param body   contenido del mensaje de texto
 */
public record WhatsAppMessage(String from, String body) {
}
