package org.delivery.application.service;

// Feature: delivery-core-refactor, Property 1: Transiciones de estado válidas e inválidas

import net.jqwik.api.*;
import org.delivery.domain.entity.Pedido;
import org.delivery.domain.enums.EstadoPedido;
import org.mockito.Mockito;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Validates: Requirements 3.1, 3.2, 3.3
 *
 * Para cualquier par (estadoActual, estadoNuevo) de EstadoPedido,
 * la transición es válida iff está en el conjunto definido.
 */
class PedidoStateMachinePropertyTest {

    private static final Map<EstadoPedido, Set<EstadoPedido>> EXPECTED_TRANSITIONS = Map.of(
            EstadoPedido.NUEVO, Set.of(EstadoPedido.CONFIRMADO, EstadoPedido.CANCELADO),
            EstadoPedido.CONFIRMADO, Set.of(EstadoPedido.PREPARANDO, EstadoPedido.CANCELADO),
            EstadoPedido.PREPARANDO, Set.of(EstadoPedido.LISTO, EstadoPedido.CANCELADO),
            EstadoPedido.LISTO, Set.of(EstadoPedido.EN_CAMINO, EstadoPedido.CANCELADO),
            EstadoPedido.EN_CAMINO, Set.of(EstadoPedido.ENTREGADO, EstadoPedido.CANCELADO),
            EstadoPedido.ENTREGADO, Set.of(),
            EstadoPedido.CANCELADO, Set.of()
    );

    private final AsignacionService asignacionService = Mockito.mock(AsignacionService.class);
    private final PedidoStateMachine stateMachine = new PedidoStateMachine(asignacionService);

    @Provide
    Arbitrary<EstadoPedido> estadoPedido() {
        return Arbitraries.of(EstadoPedido.values());
    }

    @Property(tries = 100)
    void transicionValidaSiYSoloSiEstaEnConjuntoDefinido(
            @ForAll("estadoPedido") EstadoPedido actual,
            @ForAll("estadoPedido") EstadoPedido nuevo) {

        Set<EstadoPedido> permitidos = EXPECTED_TRANSITIONS.getOrDefault(actual, Set.of());
        boolean shouldBeValid = permitidos.contains(nuevo);

        if (shouldBeValid) {
            // Valid transition: should not throw and should update state
            Pedido p = new Pedido();
            p.setId(1L);
            p.setEstado(actual);

            EstadoPedido result = stateMachine.cambiarEstado(p, nuevo);
            assertEquals(nuevo, p.getEstado(),
                    String.format("Estado debería ser %s después de transición %s → %s", nuevo, actual, nuevo));
            assertEquals(nuevo, result);
        } else {
            // Invalid transition: should throw IllegalStateException
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> stateMachine.validarTransicion(actual, nuevo),
                    String.format("Debería lanzar IllegalStateException para %s → %s", actual, nuevo));
            assertTrue(ex.getMessage().contains(actual.name()),
                    "Mensaje debe incluir estado actual: " + actual);
            assertTrue(ex.getMessage().contains(nuevo.name()),
                    "Mensaje debe incluir estado solicitado: " + nuevo);
        }
    }
}
