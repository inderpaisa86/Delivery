package org.delivery.service.impl;

import org.delivery.domain.Domiciliario;
import org.delivery.domain.Pedido;
import org.delivery.dto.TrackingResponse;
import org.delivery.dto.UbicacionRequest;
import org.delivery.dto.UbicacionResponse;
import org.delivery.repository.AsignacionDomicilioRepository;
import org.delivery.repository.DomiciliarioRepository;
import org.delivery.repository.PedidoRepository;
import org.delivery.service.TrackingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de tracking en tiempo real de domiciliarios y pedidos.
 */
@Service
public class TrackingServiceImpl implements TrackingService {

    private static final Logger log = LoggerFactory.getLogger(TrackingServiceImpl.class);

    private final DomiciliarioRepository domiciliarioRepository;
    private final PedidoRepository pedidoRepository;
    private final AsignacionDomicilioRepository asignacionRepository;

    public TrackingServiceImpl(DomiciliarioRepository domiciliarioRepository,
                               PedidoRepository pedidoRepository,
                               AsignacionDomicilioRepository asignacionRepository) {
        this.domiciliarioRepository = domiciliarioRepository;
        this.pedidoRepository = pedidoRepository;
        this.asignacionRepository = asignacionRepository;
    }

    @Override
    @Transactional
    public void actualizarUbicacion(UbicacionRequest request) {
        Domiciliario domiciliario = domiciliarioRepository.findById(request.domiciliarioId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Domiciliario no encontrado: " + request.domiciliarioId()));

        domiciliario.setLat(request.lat());
        domiciliario.setLng(request.lng());
        domiciliarioRepository.save(domiciliario);

        log.debug("Ubicación actualizada para domiciliario {}: ({}, {})",
                request.domiciliarioId(), request.lat(), request.lng());
    }

    @Override
    @Transactional(readOnly = true)
    public UbicacionResponse obtenerUbicacion(Long domiciliarioId) {
        Domiciliario d = domiciliarioRepository.findById(domiciliarioId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Domiciliario no encontrado: " + domiciliarioId));

        return new UbicacionResponse(d.getId(), d.getNombre(), d.getLat(), d.getLng());
    }

    @Override
    @Transactional(readOnly = true)
    public TrackingResponse obtenerTracking(String token) {
        Pedido pedido = pedidoRepository.findByTrackingToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Token de tracking inválido"));

        // Buscar si tiene domiciliario asignado
        Double domLat = null;
        Double domLng = null;
        String domNombre = null;

        var asignacion = asignacionRepository.findByPedidoId(pedido.getId());
        if (asignacion.isPresent()) {
            Domiciliario dom = asignacion.get().getDomiciliario();
            domLat = dom.getLat();
            domLng = dom.getLng();
            domNombre = dom.getNombre();
        }

        return new TrackingResponse(
                pedido.getId(),
                pedido.getEstado(),
                pedido.getDireccion(),
                pedido.getLat(),
                pedido.getLng(),
                domLat,
                domLng,
                domNombre);
    }
}
