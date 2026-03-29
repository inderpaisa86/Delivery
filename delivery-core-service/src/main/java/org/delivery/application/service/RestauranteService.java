package org.delivery.application.service;

import lombok.RequiredArgsConstructor;
import org.delivery.domain.entity.Restaurante;
import org.delivery.infrastructure.persistence.repository.RestauranteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RestauranteService {

    private final RestauranteRepository restauranteRepository;

    @Transactional(readOnly = true)
    public Restaurante obtenerPorId(Long id) {
        return restauranteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Restaurante no encontrado: " + id));
    }

    @Transactional(readOnly = true)
    public Restaurante resolverPorTelefonoWhatsApp(String phoneNumberId) {
        return restauranteRepository.findFirstByActivoTrue()
                .orElseThrow(() -> new IllegalStateException("No hay restaurantes activos"));
    }
}
