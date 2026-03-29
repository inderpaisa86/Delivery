package org.delivery.application.service;

import org.delivery.domain.enums.EstadoPedido;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("PedidoStateMachine")
class PedidoStateMachineTest {

    @Mock
    private AsignacionService asignacionService;

    @InjectMocks
    private PedidoStateMachine stateMachine;

    @Test
    @DisplayName("Permite NUEVO -> CONFIRMADO")
    void shouldAllowNuevoToConfirmado() {
        assertDoesNotThrow(() ->
                stateMachine.validarTransicion(EstadoPedido.NUEVO, EstadoPedido.CONFIRMADO));
    }

    @Test
    @DisplayName("Permite cancelar desde cualquier estado activo")
    void shouldAllowCancelFromActiveStates() {
        assertDoesNotThrow(() ->
                stateMachine.validarTransicion(EstadoPedido.NUEVO, EstadoPedido.CANCELADO));
        assertDoesNotThrow(() ->
                stateMachine.validarTransicion(EstadoPedido.CONFIRMADO, EstadoPedido.CANCELADO));
        assertDoesNotThrow(() ->
                stateMachine.validarTransicion(EstadoPedido.PREPARANDO, EstadoPedido.CANCELADO));
    }

    @Test
    @DisplayName("Rechaza NUEVO -> ENTREGADO")
    void shouldRejectNuevoToEntregado() {
        assertThrows(IllegalStateException.class, () ->
                stateMachine.validarTransicion(EstadoPedido.NUEVO, EstadoPedido.ENTREGADO));
    }

    @Test
    @DisplayName("Rechaza transición desde ENTREGADO")
    void shouldRejectFromEntregado() {
        assertThrows(IllegalStateException.class, () ->
                stateMachine.validarTransicion(EstadoPedido.ENTREGADO, EstadoPedido.NUEVO));
    }

    @Test
    @DisplayName("Rechaza transición desde CANCELADO")
    void shouldRejectFromCancelado() {
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
