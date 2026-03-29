package org.delivery.application.service;

import org.delivery.application.dto.*;
import org.delivery.application.port.WhatsAppPort;
import org.delivery.domain.entity.*;
import org.delivery.domain.enums.EstadoPedido;
import org.delivery.infrastructure.persistence.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Caso de uso principal: gestión del ciclo de vida de pedidos.
 */
@Service
public class PedidoService {

    private static final Logger log = LoggerFactory.getLogger(PedidoService.class);

    private final PedidoRepository pedidoRepository;
    private final ClienteRepository clienteRepository;
    private final ProductoRepository productoRepository;
    private final RestauranteRepository restauranteRepository;
    private final PedidoStateMachine stateMachine;
    private final WhatsAppPort whatsAppPort;

    public PedidoService(PedidoRepository pedidoRepository,
                         ClienteRepository clienteRepository,
                         ProductoRepository productoRepository,
                         RestauranteRepository restauranteRepository,
                         PedidoStateMachine stateMachine,
                         WhatsAppPort whatsAppPort) {
        this.pedidoRepository = pedidoRepository;
        this.clienteRepository = clienteRepository;
        this.productoRepository = productoRepository;
        this.restauranteRepository = restauranteRepository;
        this.stateMachine = stateMachine;
        this.whatsAppPort = whatsAppPort;
    }

    @Transactional
    public PedidoResponse crearPedido(PedidoRequest request) {
        Restaurante restaurante = restauranteRepository.findById(request.restauranteId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Restaurante no encontrado: " + request.restauranteId()));

        Cliente cliente = clienteRepository
                .findByTelefonoAndRestauranteId(request.telefono(), restaurante.getId())
                .orElseGet(() -> {
                    Cliente nuevo = new Cliente(
                            request.telefono(), request.nombre(),
                            request.direccion(), restaurante);
                    return clienteRepository.save(nuevo);
                });

        Pedido pedido = new Pedido();
        pedido.setCliente(cliente);
        pedido.setRestaurante(restaurante);
        pedido.setDireccion(request.direccion());
        pedido.setLat(request.lat());
        pedido.setLng(request.lng());
        pedido.setTrackingToken(UUID.randomUUID().toString());

        for (DetallePedidoRequest detReq : request.detalles()) {
            Producto producto = productoRepository.findById(detReq.productoId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Producto no encontrado: " + detReq.productoId()));
            pedido.addDetalle(new DetallePedido(producto, detReq.cantidad()));
        }

        pedido.calcularTotal();
        pedido = pedidoRepository.save(pedido);

        log.info("Pedido #{} creado - restaurante: {}, cliente: {}",
                pedido.getId(), restaurante.getNombre(), cliente.getTelefono());

        whatsAppPort.notificarCambioEstado(
                cliente.getTelefono(), pedido.getId(), EstadoPedido.NUEVO);

        return toResponse(pedido);
    }

    @Transactional(readOnly = true)
    public PedidoResponse obtenerPedido(Long id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado: " + id));
        return toResponse(pedido);
    }

    @Transactional
    public PedidoResponse cambiarEstado(Long id, EstadoPedido nuevoEstado) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado: " + id));

        stateMachine.cambiarEstado(pedido, nuevoEstado);
        pedido = pedidoRepository.save(pedido);

        whatsAppPort.notificarCambioEstado(
                pedido.getCliente().getTelefono(), id, nuevoEstado);

        return toResponse(pedido);
    }

    private PedidoResponse toResponse(Pedido pedido) {
        var detalles = pedido.getDetalles().stream()
                .map(d -> new PedidoResponse.DetalleResponse(
                        d.getProducto().getNombre(), d.getCantidad(), d.getPrecio()))
                .toList();

        return new PedidoResponse(
                pedido.getId(),
                pedido.getRestaurante().getId(),
                pedido.getRestaurante().getNombre(),
                pedido.getCliente().getTelefono(),
                pedido.getCliente().getNombre(),
                pedido.getDireccion(),
                pedido.getEstado(),
                pedido.getTotal(),
                pedido.getTrackingToken(),
                pedido.getFecha(),
                detalles);
    }
}
