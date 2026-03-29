package org.delivery.service;

import org.delivery.dto.WhatsAppMessage;

/**
 * Contrato para procesar mensajes entrantes de WhatsApp.
 */
public interface WhatsAppMessageService {

    void processMessage(WhatsAppMessage message);
}
