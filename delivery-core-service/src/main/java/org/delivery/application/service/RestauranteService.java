package org.delivery.application.service;

import lombok.RequiredArgsConstructor;
import org.delivery.application.dto.RestauranteRequest;
import org.delivery.application.dto.RestauranteResponse;
import org.delivery.domain.entity.Restaurante;
import org.delivery.infrastructure.persistence.repository.IRestauranteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RestauranteService {

    private final IRestauranteRepository restauranteRepository;

    @Transactional
    public RestauranteResponse crear(RestauranteRequest request) {
        Restaurante r = Restaurante.builder()
                .nombre(request.nombre())
                .telefono(request.telefono())
                .direccion(request.direccion())
                .lat(request.lat())
                .lng(request.lng())
                .whatsappPhoneId(request.whatsappPhoneId())
                .build();
        return toResponse(restauranteRepository.save(r));
    }

    @Transactional
    public RestauranteResponse actualizar(Long id, RestauranteRequest request) {
        Restaurante r = restauranteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Restaurante no encontrado: " + id));
        r.setNombre(request.nombre());
        r.setTelefono(request.telefono());
        r.setDireccion(request.direccion());
        r.setLat(request.lat());
        r.setLng(request.lng());
        r.setWhatsappPhoneId(request.whatsappPhoneId());
        return toResponse(restauranteRepository.save(r));
    }

    @Transactional(readOnly = true)
    public RestauranteResponse obtenerPorId(Long id) {
        return toResponse(restauranteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Restaurante no encontrado: " + id)));
    }

    @Transactional(readOnly = true)
    public List<RestauranteResponse> listarActivos() {
        return restauranteRepository.findByActivoTrue().stream()
                .map(this::toResponse).toList();
    }

    /**
     * Resuelve restaurante por whatsappPhoneId.
     * Si no encuentra por phoneId, retorna el primer restaurante activo (fallback).
     */
    @Transactional(readOnly = true)
    public Restaurante resolverPorTelefonoWhatsApp(String phoneNumberId) {
        return restauranteRepository.findByWhatsappPhoneIdAndActivoTrue(phoneNumberId)
                .orElseGet(() -> restauranteRepository.findFirstByActivoTrue()
                        .orElseThrow(() -> new IllegalStateException("No hay restaurantes activos")));
    }

    private RestauranteResponse toResponse(Restaurante r) {
        return new RestauranteResponse(r.getId(), r.getNombre(), r.getTelefono(),
                r.getDireccion(), r.getLat(), r.getLng(), r.isActivo(), r.getWhatsappPhoneId());
    }
}
