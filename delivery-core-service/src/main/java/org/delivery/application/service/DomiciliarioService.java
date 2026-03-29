package org.delivery.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.delivery.application.dto.DomiciliarioRequest;
import org.delivery.application.dto.DomiciliarioResponse;
import org.delivery.application.dto.UbicacionRequest;
import org.delivery.application.dto.UbicacionResponse;
import org.delivery.domain.entity.Domiciliario;
import org.delivery.domain.entity.Restaurante;
import org.delivery.infrastructure.persistence.repository.IDomiciliarioRepository;
import org.delivery.infrastructure.persistence.repository.IRestauranteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class DomiciliarioService {

    private final IDomiciliarioRepository domiciliarioRepository;
    private final IRestauranteRepository restauranteRepository;

    @Transactional
    public DomiciliarioResponse crear(DomiciliarioRequest request) {
        Restaurante r = restauranteRepository.findById(request.restauranteId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Restaurante no encontrado: " + request.restauranteId()));

        Domiciliario d = Domiciliario.builder()
                .nombre(request.nombre())
                .cedula(request.cedula())
                .foto(request.foto())
                .telefono(request.telefono())
                .lat(request.lat())
                .lng(request.lng())
                .restaurante(r)
                .build();
        return toResponse(domiciliarioRepository.save(d));
    }

    @Transactional
    public DomiciliarioResponse actualizar(Long id, DomiciliarioRequest request) {
        Domiciliario d = domiciliarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Domiciliario no encontrado: " + id));
        d.setNombre(request.nombre());
        d.setCedula(request.cedula());
        d.setFoto(request.foto());
        d.setTelefono(request.telefono());
        // Solo actualizar coordenadas si vienen en el request
        if (request.lat() != null) d.setLat(request.lat());
        if (request.lng() != null) d.setLng(request.lng());
        return toResponse(domiciliarioRepository.save(d));
    }

    @Transactional
    public void actualizarUbicacion(UbicacionRequest request) {
        Domiciliario d = domiciliarioRepository.findById(request.domiciliarioId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Domiciliario no encontrado: " + request.domiciliarioId()));
        d.setLat(request.lat());
        d.setLng(request.lng());
        domiciliarioRepository.save(d);
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
    public List<DomiciliarioResponse> obtenerDisponibles(Long restauranteId) {
        return domiciliarioRepository
                .findByDisponibleTrueAndRestauranteIdAndLatIsNotNullAndLngIsNotNull(restauranteId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public DomiciliarioResponse buscarPorCedula(String cedula) {
        Domiciliario d = domiciliarioRepository.findByCedula(cedula)
                .orElseThrow(() -> new IllegalArgumentException("Domiciliario no encontrado con cédula: " + cedula));
        return toResponse(d);
    }

    private DomiciliarioResponse toResponse(Domiciliario d) {
        return new DomiciliarioResponse(d.getId(), d.getNombre(), d.getCedula(), d.getFoto(),
                d.getTelefono(), d.isDisponible(), d.getLat(), d.getLng(), d.getRestaurante().getId());
    }
}
