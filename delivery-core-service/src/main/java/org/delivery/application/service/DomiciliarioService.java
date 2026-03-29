package org.delivery.application.service;

import org.delivery.application.dto.UbicacionRequest;
import org.delivery.application.dto.UbicacionResponse;
import org.delivery.domain.entity.Domiciliario;
import org.delivery.infrastructure.persistence.repository.DomiciliarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Caso de uso: gestión de domiciliarios y su ubicación.
 */
@Service
public class DomiciliarioService {

    private static final Logger log = LoggerFactory.getLogger(DomiciliarioService.class);

    private final DomiciliarioRepository domiciliarioRepository;

    public DomiciliarioService(DomiciliarioRepository domiciliarioRepository) {
        this.domiciliarioRepository = domiciliarioRepository;
    }

    @Transactional
    public void actualizarUbicacion(UbicacionRequest request) {
        Domiciliario domiciliario = domiciliarioRepository.findById(request.domiciliarioId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Domiciliario no encontrado: " + request.domiciliarioId()));

        domiciliario.setLat(request.lat());
        domiciliario.setLng(request.lng());
        domiciliarioRepository.save(domiciliario);

        log.debug("Ubicación actualizada: domiciliario #{} ({}, {})",
                request.domiciliarioId(), request.lat(), request.lng());
    }

    @Transactional(readOnly = true)
    public UbicacionResponse obtenerUbicacion(Long domiciliarioId) {
        Domiciliario d = domiciliarioRepository.findById(domiciliarioId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Domiciliario no encontrado: " + domiciliarioId));
        return new UbicacionResponse(d.getId(), d.getNombre(), d.getLat(), d.getLng());
    }

    @Transactional(readOnly = true)
    public List<Domiciliario> obtenerDisponibles(Long restauranteId) {
        return domiciliarioRepository
                .findByDisponibleTrueAndRestauranteIdAndLatIsNotNullAndLngIsNotNull(restauranteId);
    }
}
