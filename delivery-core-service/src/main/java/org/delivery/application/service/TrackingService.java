package org.delivery.application.service;

import lombok.RequiredArgsConstructor;
import org.delivery.application.dto.TrackingResponse;
import org.delivery.application.dto.UbicacionResponse;
import org.delivery.domain.entity.Domiciliario;
import org.delivery.domain.entity.Pedido;
import org.delivery.infrastructure.persistence.repository.IAsignacionDomicilioRepository;
import org.delivery.infrastructure.persistence.repository.IPedidoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TrackingService {

    private final IPedidoRepository pedidoRepository;
    private final IAsignacionDomicilioRepository asignacionRepository;
    private final DomiciliarioService domiciliarioService;

    @Transactional(readOnly = true)
    public TrackingResponse obtenerTracking(String token) {
        Pedido pedido = pedidoRepository.findByTrackingToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Token de tracking inválido"));

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
                pedido.getId(), pedido.getEstado(), pedido.getDireccion(),
                pedido.getLat(), pedido.getLng(),
                domLat, domLng, domNombre);
    }

    @Transactional(readOnly = true)
    public UbicacionResponse obtenerUbicacion(Long domiciliarioId) {
        return domiciliarioService.obtenerUbicacion(domiciliarioId);
    }
}
