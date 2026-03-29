package org.delivery.service;

import org.delivery.domain.enums.EstadoPedido;

/**
 * Máquina de estados para validar transiciones de un pedido.
 */
public interface PedidoStateMachine {

    /**
     * Valida si la transición de estado es permitida.
     *
     * @throws IllegalStateException si la transición no es válida
     */
    void validarTransicion(EstadoPedido actual, EstadoPedido nuevo);
}
