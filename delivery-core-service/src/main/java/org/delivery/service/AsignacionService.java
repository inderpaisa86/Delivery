package org.delivery.service;

/**
 * Servicio de asignación automática de domiciliarios a pedidos.
 */
public interface AsignacionService {

    /**
     * Busca el domiciliario disponible más cercano y lo asigna al pedido.
     */
    void asignarDomiciliario(Long pedidoId);
}
