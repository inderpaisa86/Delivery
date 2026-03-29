package org.delivery.service;

import org.delivery.domain.enums.EstadoPedido;
import org.delivery.dto.PedidoRequest;
import org.delivery.dto.PedidoResponse;

/**
 * Servicio de gestión de pedidos.
 */
public interface PedidoService {

    PedidoResponse crearPedido(PedidoRequest request);

    PedidoResponse obtenerPedido(Long id);

    PedidoResponse cambiarEstado(Long id, EstadoPedido nuevoEstado);
}
