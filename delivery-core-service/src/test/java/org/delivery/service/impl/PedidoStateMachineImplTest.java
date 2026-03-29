package org.delivery.service.impl;

import org.delivery.domain.enums.EstadoPedido;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PedidoStateMachineImpl")
class PedidoStateMachineImplTest {

    private final PedidoStateMachineImpl stateMachine = new PedidoStateMachineImpl();

    @Test
    @DisplayName("Permite transición NUEVO -> CONFIRMADO")
    void shouldAllowNuevoToConfirmado() {
        assertDoesNotThrow(() ->
                stateMachine.validarTransicion(EstadoPedido.NUEVO, EstadoPedido.CONFIRMADO));
    }

    @Test
    @DisplayName("Permite transición NUEVO -> CANCELADO")
    void shouldAllowNuevoToCancelado() {
        assertDoesNotThrow(() ->
                stateMachine.validarTransicion(EstadoPedido.NUEVO, EstadoPedido.CANCELADO));
    }

    @Test
    @DisplayName("Rechaza transición NUEVO -> ENTREGADO")
    void shouldRejectNuevoToEntregado() {
        assertThrows(IllegalStateException.class, () ->
                stateMachine.validarTransicion(EstadoPedido.NUEVO, EstadoPedido.ENTREGADO));
    }

    @Test
    @DisplayName("Rechaza transición ENTREGADO -> cualquier estado")
    void shouldRejectTransitionFromEntregado() {
        assertThrows(IllegalStateException.class, () ->
                stateMachine.validarTransicion(EstadoPedido.ENTREGADO, EstadoPedido.NUEVO));
    }

    @Test
    @DisplayName("Rechaza transición CANCELADO -> cualquier estado")
    void shouldRejectTransitionFromCancelado() {
        assertThrows(IllegalStateException.class, () ->
                stateMachine.validarTransicion(EstadoPedido.CANCELADO, EstadoPedido.NUEVO));
    }

    @Test
    @DisplayName("Flujo completo válido")
    void shouldAllowFullFlow() {
        assertDoesNotThrow(() -> {
            stateMachine.validarTransicion(EstadoPedido.NUEVO, EstadoPedido.CONFIRMADO);
            stateMachine.validarTransicion(EstadoPedido.CONFIRMADO, EstadoPedido.PREPARANDO);
            stateMachine.validarTransicion(EstadoPedido.PREPARANDO, EstadoPedido.LISTO);
            stateMachine.validarTransicion(EstadoPedido.LISTO, EstadoPedido.EN_CAMINO);
            stateMachine.validarTransicion(EstadoPedido.EN_CAMINO, EstadoPedido.ENTREGADO);
        });
    }
}
