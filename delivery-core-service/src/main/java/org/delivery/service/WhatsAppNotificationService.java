package org.delivery.service;

import org.delivery.domain.enums.EstadoPedido;

/**
 * Servicio para enviar notificaciones al cliente vía WhatsApp Business API.
 */
public interface WhatsAppNotificationService {

    void notificarCambioEstado(String telefono, Long pedidoId, EstadoPedido estado);

    void enviarMensaje(String telefono, String mensaje);
}
