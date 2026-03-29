package org.delivery.application.port;

import org.delivery.domain.enums.EstadoPedido;

/**
 * Puerto de salida para comunicación con WhatsApp Business API.
 * La implementación concreta vive en infrastructure.
 */
public interface IWhatsAppPort {

    void notificarCambioEstado(String telefono, Long pedidoId, EstadoPedido estado);

    void enviarMensaje(String telefono, String mensaje);
}
