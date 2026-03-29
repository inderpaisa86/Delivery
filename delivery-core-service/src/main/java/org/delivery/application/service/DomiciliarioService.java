package org.delivery.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.delivery.application.dto.UbicacionRequest;
import org.delivery.application.dto.UbicacionResponse;
import org.delivery.domain.entity.Domiciliario;
import org.delivery.infrastructure.persistence.repository.DomiciliarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class DomiciliarioService {

    private final DomiciliarioRepository domiciliarioRepository;

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
