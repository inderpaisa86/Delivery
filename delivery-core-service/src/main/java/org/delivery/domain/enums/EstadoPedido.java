package org.delivery.domain.enums;

/**
 * Ciclo de vida de un pedido.
 */
public enum EstadoPedido {
    NUEVO,
    CONFIRMADO,
    PREPARANDO,
    LISTO,
    EN_CAMINO,
    ENTREGADO,
    CANCELADO
}
