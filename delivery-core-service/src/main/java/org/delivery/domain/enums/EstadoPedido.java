package org.delivery.domain.enums;

/**
 * Estados posibles de un pedido en su ciclo de vida.
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
