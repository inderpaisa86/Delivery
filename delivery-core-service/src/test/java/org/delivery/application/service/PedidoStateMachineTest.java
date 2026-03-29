package org.delivery.application.service;

import org.delivery.domain.entity.Pedido;
import org.delivery.domain.enums.EstadoPedido;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PedidoStateMachine")
class PedidoStateMachineTest {

    @Mock AsignacionService asignacionService;
    @InjectMocks PedidoStateMachine stateMachine;

    private Pedido pedido(EstadoPedido estado) {
        Pedido p = new Pedido(); p.setId(1L); p.setEstado(estado); return p;
    }

    @Test
    @DisplayName("Cambia estado y ejecuta acción LISTO → asignar domiciliario")
    void shouldAssignOnListo() {
        Pedido p = pedido(EstadoPedido.PREPARANDO);
        stateMachine.cambiarEstado(p, EstadoPedido.LISTO);
        assertEquals(EstadoPedido.LISTO, p.getEstado());
        verify(asignacionService).asignarDomiciliario(1L);
    }

    @Test
    @DisplayName("Libera domiciliario en ENTREGADO")
    void shouldReleaseOnEntregado() {
        Pedido p = pedido(EstadoPedido.EN_CAMINO);
        stateMachine.cambiarEstado(p, EstadoPedido.ENTREGADO);
        verify(asignacionService).liberarDomiciliario(1L);
    }

    @Test
    @DisplayName("Libera domiciliario en CANCELADO")
    void shouldReleaseOnCancelado() {
        Pedido p = pedido(EstadoPedido.CONFIRMADO);
        stateMachine.cambiarEstado(p, EstadoPedido.CANCELADO);
        verify(asignacionService).liberarDomiciliario(1L);
    }

    @Test
    @DisplayName("No ejecuta acción en CONFIRMADO")
    void shouldNotActOnConfirmado() {
        Pedido p = pedido(EstadoPedido.NUEVO);
        stateMachine.cambiarEstado(p, EstadoPedido.CONFIRMADO);
        verify(asignacionService, never()).asignarDomiciliario(anyLong());
        verify(asignacionService, never()).liberarDomiciliario(anyLong());
    }

    @Test
    @DisplayName("Rechaza transición inválida")
    void shouldRejectInvalidTransition() {
        assertThrows(IllegalStateException.class,
                () -> stateMachine.validarTransicion(EstadoPedido.NUEVO, EstadoPedido.ENTREGADO));
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

    @Test
    @DisplayName("Rechaza desde ENTREGADO y CANCELADO")
    void shouldRejectFromTerminalStates() {
        assertThrows(IllegalStateException.class,
                () -> stateMachine.validarTransicion(EstadoPedido.ENTREGADO, EstadoPedido.NUEVO));
        assertThrows(IllegalStateException.class,
                () -> stateMachine.validarTransicion(EstadoPedido.CANCELADO, EstadoPedido.NUEVO));
    }
}
