package org.delivery.service.impl;

import java.util.UUID;

import org.delivery.domain.Cliente;
import org.delivery.domain.DetallePedido;
import org.delivery.domain.Pedido;
import org.delivery.domain.Producto;
import org.delivery.domain.enums.EstadoPedido;
import org.delivery.dto.DetallePedidoRequest;
import org.delivery.dto.PedidoRequest;
import org.delivery.dto.PedidoResponse;
import org.delivery.repository.ClienteRepository;
import org.delivery.repository.PedidoRepository;
import org.delivery.repository.ProductoRepository;
import org.delivery.service.PedidoService;
import org.delivery.service.PedidoStateMachine;
import org.delivery.service.WhatsAppNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación del servicio de pedidos.
 */
@Service
public class PedidoServiceImpl implements PedidoService {

    private static final Logger log = LoggerFactory.getLogger(PedidoServiceImpl.class);

    private final PedidoRepository pedidoRepository;
    private final ClienteRepository clienteRepository;
    private final ProductoRepository productoRepository;
    private final PedidoStateMachine stateMachine;
    private final WhatsAppNotificationService notificationService;

    public PedidoServiceImpl(PedidoRepository pedidoRepository,
                             ClienteRepository clienteRepository,
                             ProductoRepository productoRepository,
                             PedidoStateMachine stateMachine,
                             WhatsAppNotificationService notificationService) {
        this.pedidoRepository = pedidoRepository;
        this.clienteRepository = clienteRepository;
        this.productoRepository = productoRepository;
        this.stateMachine = stateMachine;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public PedidoResponse crearPedido(PedidoRequest request) {
        // Crear o recuperar cliente
        Cliente cliente = clienteRepository.findByTelefono(request.telefono())
                .orElseGet(() -> {
                    Cliente nuevo = new Cliente(request.telefono(), request.nombre(), request.direccion());
                    return clienteRepository.save(nuevo);
                });

        // Crear pedido
        Pedido pedido = new Pedido();
        pedido.setCliente(cliente);
        pedido.setDireccion(request.direccion());
        pedido.setLat(request.lat());
        pedido.setLng(request.lng());
        pedido.setTrackingToken(UUID.randomUUID().toString());

        // Agregar detalles
        for (DetallePedidoRequest detReq : request.detalles()) {
            Producto producto = productoRepository.findById(detReq.productoId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Producto no encontrado: " + detReq.productoId()));
            pedido.addDetalle(new DetallePedido(producto, detReq.cantidad()));
        }

        pedido.calcularTotal();
        pedido = pedidoRepository.save(pedido);

        log.info("Pedido #{} creado para cliente {}", pedido.getId(), cliente.getTelefono());
        notificationService.notificarCambioEstado(
                cliente.getTelefono(), pedido.getId(), EstadoPedido.NUEVO);

        return toResponse(pedido);
    }

    @Override
    @Transactional(readOnly = true)
    public PedidoResponse obtenerPedido(Long id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado: " + id));
        return toResponse(pedido);
    }

    @Override
    @Transactional
    public PedidoResponse cambiarEstado(Long id, EstadoPedido nuevoEstado) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado: " + id));

        stateMachine.validarTransicion(pedido.getEstado(), nuevoEstado);
        pedido.setEstado(nuevoEstado);
        pedido = pedidoRepository.save(pedido);

        log.info("Pedido #{} cambió a estado {}", id, nuevoEstado);
        notificationService.notificarCambioEstado(
                pedido.getCliente().getTelefono(), id, nuevoEstado);

        return toResponse(pedido);
    }

    private PedidoResponse toResponse(Pedido pedido) {
        var detalles = pedido.getDetalles().stream()
                .map(d -> new PedidoResponse.DetalleResponse(
                        d.getProducto().getNombre(),
                        d.getCantidad(),
                        d.getPrecio()))
                .toList();

        return new PedidoResponse(
                pedido.getId(),
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
