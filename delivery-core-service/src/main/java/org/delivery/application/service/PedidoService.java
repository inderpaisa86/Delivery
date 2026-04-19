package org.delivery.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.delivery.application.dto.*;
import org.delivery.application.port.IWhatsAppPort;
import org.delivery.domain.entity.*;
import org.delivery.domain.enums.EstadoPedido;
import org.delivery.infrastructure.persistence.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class PedidoService {

    private final IPedidoRepository pedidoRepository;
    private final IClienteRepository clienteRepository;
    private final IProductoRepository productoRepository;
    private final IRestauranteRepository restauranteRepository;
    private final PedidoStateMachine stateMachine;
    private final IWhatsAppPort whatsAppPort;

    @Transactional
    public PedidoResponse crearPedido(PedidoRequest request) {
        Restaurante restaurante = restauranteRepository.findById(request.restauranteId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Restaurante no encontrado: " + request.restauranteId()));

        Cliente cliente = clienteRepository
                .findByTelefonoAndRestauranteId(request.telefono(), restaurante.getId())
                .orElseGet(() -> clienteRepository.save(
                        Cliente.builder()
                                .telefono(request.telefono())
                                .nombre(request.nombre())
                                .direccion(request.direccion())
                                .restaurante(restaurante)
                                .build()));

        Pedido pedido = Pedido.builder()
                .cliente(cliente)
                .restaurante(restaurante)
                .direccion(request.direccion())
                .lat(request.lat())
                .lng(request.lng())
                .trackingToken(UUID.randomUUID().toString())
                .build();

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

    @Transactional(readOnly = true)
    public Page<PedidoResponse> listarPorRestaurante(Long restauranteId, Pageable pageable) {
        return pedidoRepository.findByRestauranteId(restauranteId, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<PedidoResponse> listarPorRestauranteHoy(Long restauranteId, Pageable pageable) {
        LocalDateTime hoy = LocalDate.now().atStartOfDay();
        return pedidoRepository.findByRestauranteIdAndFechaGreaterThanEqual(
                restauranteId, hoy, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<PedidoResponse> listarPorCliente(String telefono, Pageable pageable) {
        return pedidoRepository.findByClienteTelefono(telefono, pageable)
                .map(this::toResponse);
    }

    /**
     * Actualiza la ubicación de entrega de un pedido pendiente (sin lat/lng).
     * Retorna true si encontró y actualizó un pedido.
     */
    @Transactional
    public boolean actualizarUbicacionPedido(String telefono, Double lat, Double lng, String direccion) {
        return pedidoRepository
                .findFirstByClienteTelefonoAndEstadoAndLatIsNullOrderByFechaDesc(
                        telefono, EstadoPedido.NUEVO)
                .map(pedido -> {
                    pedido.setLat(lat);
                    pedido.setLng(lng);
                    if (direccion != null && !direccion.isBlank()) {
                        pedido.setDireccion(direccion);
                    }
                    pedidoRepository.save(pedido);
                    log.info("Ubicación actualizada para pedido #{} ({}, {})", pedido.getId(), lat, lng);
                    return true;
                })
                .orElse(false);
    }

    @Transactional
    public PedidoResponse actualizarUbicacionPorId(Long id, Double lat, Double lng, String direccion) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado: " + id));
        pedido.setLat(lat);
        pedido.setLng(lng);
        if (direccion != null && !direccion.isBlank()) {
            pedido.setDireccion(direccion);
        }
        pedido = pedidoRepository.save(pedido);
        log.info("Ubicación actualizada para pedido #{} ({}, {}) - {}", id, lat, lng, direccion);
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
                pedido.getLat(),
                pedido.getLng(),
                pedido.getEstado(),
                pedido.getTotal(),
                pedido.getTrackingToken(),
                pedido.getFecha(),
                detalles);
    }
}
