package org.delivery.application.service;

import org.delivery.domain.entity.Pedido;
import org.delivery.domain.enums.EstadoPedido;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
        Pedido p = new Pedido();
        p.setId(1L);
        p.setEstado(estado);
        return p;
    }

    // --- Transiciones válidas (happy path) ---

    @ParameterizedTest(name = "{0} → {1}")
    @DisplayName("Transiciones válidas del flujo principal")
    @CsvSource({
            "NUEVO, CONFIRMADO",
            "CONFIRMADO, PREPARANDO",
            "PREPARANDO, LISTO",
            "LISTO, EN_CAMINO",
            "EN_CAMINO, ENTREGADO"
    })
    void shouldAllowValidTransitions(String from, String to) {
        EstadoPedido estadoFrom = EstadoPedido.valueOf(from);
        EstadoPedido estadoTo = EstadoPedido.valueOf(to);
        Pedido p = pedido(estadoFrom);

        EstadoPedido result = stateMachine.cambiarEstado(p, estadoTo);

        assertEquals(estadoTo, p.getEstado());
        assertEquals(estadoTo, result);
    }

    @ParameterizedTest(name = "{0} → CANCELADO")
    @DisplayName("Cancelación desde cualquier estado no terminal")
    @CsvSource({"NUEVO", "CONFIRMADO", "PREPARANDO", "LISTO", "EN_CAMINO"})
    void shouldAllowCancellationFromNonTerminalStates(String from) {
        EstadoPedido estadoFrom = EstadoPedido.valueOf(from);
        Pedido p = pedido(estadoFrom);

        EstadoPedido result = stateMachine.cambiarEstado(p, EstadoPedido.CANCELADO);

        assertEquals(EstadoPedido.CANCELADO, p.getEstado());
        assertEquals(EstadoPedido.CANCELADO, result);
    }

    // --- Transiciones inválidas ---

    @Test
    @DisplayName("Rechaza transición inválida con IllegalStateException y mensaje descriptivo")
    void shouldRejectInvalidTransitionWithDescriptiveMessage() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> stateMachine.validarTransicion(EstadoPedido.NUEVO, EstadoPedido.ENTREGADO));
        assertTrue(ex.getMessage().contains("NUEVO"));
        assertTrue(ex.getMessage().contains("ENTREGADO"));
    }

    @Test
    @DisplayName("Rechaza transiciones desde estados terminales ENTREGADO y CANCELADO")
    void shouldRejectFromTerminalStates() {
        for (EstadoPedido target : EstadoPedido.values()) {
            if (target == EstadoPedido.ENTREGADO) continue;
            assertThrows(IllegalStateException.class,
                    () -> stateMachine.validarTransicion(EstadoPedido.ENTREGADO, target));
        }
        for (EstadoPedido target : EstadoPedido.values()) {
            if (target == EstadoPedido.CANCELADO) continue;
            assertThrows(IllegalStateException.class,
                    () -> stateMachine.validarTransicion(EstadoPedido.CANCELADO, target));
        }
    }

    @Test
    @DisplayName("Rechaza ENTREGADO → CANCELADO y CANCELADO → CANCELADO")
    void shouldRejectTerminalToCancelado() {
        assertThrows(IllegalStateException.class,
                () -> stateMachine.validarTransicion(EstadoPedido.ENTREGADO, EstadoPedido.CANCELADO));
        assertThrows(IllegalStateException.class,
                () -> stateMachine.validarTransicion(EstadoPedido.CANCELADO, EstadoPedido.CANCELADO));
    }

    // --- Acciones automáticas ---

    @Test
    @DisplayName("LISTO dispara asignarDomiciliario")
    void shouldAssignOnListo() {
        Pedido p = pedido(EstadoPedido.PREPARANDO);
        stateMachine.cambiarEstado(p, EstadoPedido.LISTO);
        verify(asignacionService).asignarDomiciliario(1L);
        verify(asignacionService, never()).liberarDomiciliario(anyLong());
    }

    @Test
    @DisplayName("ENTREGADO dispara liberarDomiciliario")
    void shouldReleaseOnEntregado() {
        Pedido p = pedido(EstadoPedido.EN_CAMINO);
        stateMachine.cambiarEstado(p, EstadoPedido.ENTREGADO);
        verify(asignacionService).liberarDomiciliario(1L);
        verify(asignacionService, never()).asignarDomiciliario(anyLong());
    }

    @Test
    @DisplayName("CANCELADO dispara liberarDomiciliario")
    void shouldReleaseOnCancelado() {
        Pedido p = pedido(EstadoPedido.CONFIRMADO);
        stateMachine.cambiarEstado(p, EstadoPedido.CANCELADO);
        verify(asignacionService).liberarDomiciliario(1L);
        verify(asignacionService, never()).asignarDomiciliario(anyLong());
    }

    @Test
    @DisplayName("CONFIRMADO no dispara ninguna acción automática")
    void shouldNotActOnConfirmado() {
        Pedido p = pedido(EstadoPedido.NUEVO);
        stateMachine.cambiarEstado(p, EstadoPedido.CONFIRMADO);
        verify(asignacionService, never()).asignarDomiciliario(anyLong());
        verify(asignacionService, never()).liberarDomiciliario(anyLong());
    }

    @Test
    @DisplayName("PREPARANDO no dispara ninguna acción automática")
    void shouldNotActOnPreparando() {
        Pedido p = pedido(EstadoPedido.CONFIRMADO);
        stateMachine.cambiarEstado(p, EstadoPedido.PREPARANDO);
        verify(asignacionService, never()).asignarDomiciliario(anyLong());
        verify(asignacionService, never()).liberarDomiciliario(anyLong());
    }
}
