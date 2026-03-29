package org.delivery.service.impl;

import java.util.Map;
import java.util.Set;

import org.delivery.domain.enums.EstadoPedido;
import org.delivery.service.PedidoStateMachine;
import org.springframework.stereotype.Service;

/**
 * Máquina de estados que define las transiciones válidas de un pedido.
 */
@Service
public class PedidoStateMachineImpl implements PedidoStateMachine {

    private static final Map<EstadoPedido, Set<EstadoPedido>> TRANSICIONES = Map.of(
            EstadoPedido.NUEVO, Set.of(EstadoPedido.CONFIRMADO, EstadoPedido.CANCELADO),
            EstadoPedido.CONFIRMADO, Set.of(EstadoPedido.PREPARANDO, EstadoPedido.CANCELADO),
            EstadoPedido.PREPARANDO, Set.of(EstadoPedido.LISTO, EstadoPedido.CANCELADO),
            EstadoPedido.LISTO, Set.of(EstadoPedido.EN_CAMINO, EstadoPedido.CANCELADO),
            EstadoPedido.EN_CAMINO, Set.of(EstadoPedido.ENTREGADO, EstadoPedido.CANCELADO),
            EstadoPedido.ENTREGADO, Set.of(),
            EstadoPedido.CANCELADO, Set.of()
    );

    @Override
    public void validarTransicion(EstadoPedido actual, EstadoPedido nuevo) {
        Set<EstadoPedido> permitidos = TRANSICIONES.getOrDefault(actual, Set.of());
        if (!permitidos.contains(nuevo)) {
            throw new IllegalStateException(
                    String.format("Transición no permitida: %s -> %s", actual, nuevo));
        }
    }
}
