package org.delivery.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.delivery.domain.entity.Pedido;
import org.delivery.domain.enums.EstadoPedido;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
@Slf4j
@RequiredArgsConstructor
public class PedidoStateMachine {

    private static final Map<EstadoPedido, Set<EstadoPedido>> TRANSICIONES = Map.of(
            EstadoPedido.NUEVO, Set.of(EstadoPedido.CONFIRMADO, EstadoPedido.CANCELADO),
            EstadoPedido.CONFIRMADO, Set.of(EstadoPedido.PREPARANDO, EstadoPedido.CANCELADO),
            EstadoPedido.PREPARANDO, Set.of(EstadoPedido.LISTO, EstadoPedido.CANCELADO),
            EstadoPedido.LISTO, Set.of(EstadoPedido.EN_CAMINO, EstadoPedido.CANCELADO),
            EstadoPedido.EN_CAMINO, Set.of(EstadoPedido.ENTREGADO, EstadoPedido.CANCELADO),
            EstadoPedido.ENTREGADO, Set.of(),
            EstadoPedido.CANCELADO, Set.of()
    );

    private final AsignacionService asignacionService;

    public EstadoPedido cambiarEstado(Pedido pedido, EstadoPedido nuevoEstado) {
        EstadoPedido actual = pedido.getEstado();
        validarTransicion(actual, nuevoEstado);

        pedido.setEstado(nuevoEstado);
        log.info("Pedido #{}: {} → {}", pedido.getId(), actual, nuevoEstado);

        ejecutarAccionesAutomaticas(pedido, nuevoEstado);
        return nuevoEstado;
    }

    public void validarTransicion(EstadoPedido actual, EstadoPedido nuevo) {
        Set<EstadoPedido> permitidos = TRANSICIONES.getOrDefault(actual, Set.of());
        if (!permitidos.contains(nuevo)) {
            throw new IllegalStateException(
                    String.format("Transición no permitida: %s → %s", actual, nuevo));
        }
    }

    private void ejecutarAccionesAutomaticas(Pedido pedido, EstadoPedido nuevoEstado) {
        switch (nuevoEstado) {
            case LISTO -> {
                log.info("Pedido #{} LISTO → asignación automática de domiciliario", pedido.getId());
                asignacionService.asignarDomiciliario(pedido.getId());
            }
            case ENTREGADO -> {
                log.info("Pedido #{} ENTREGADO → liberando domiciliario", pedido.getId());
                asignacionService.liberarDomiciliario(pedido.getId());
            }
            case CANCELADO -> {
                log.info("Pedido #{} CANCELADO → liberando domiciliario si aplica", pedido.getId());
                asignacionService.liberarDomiciliario(pedido.getId());
            }
            default -> { /* Sin acción automática */ }
        }
    }
}
