package org.delivery.application.service;

import org.delivery.domain.entity.Restaurante;
import org.delivery.infrastructure.persistence.repository.RestauranteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: gestión de restaurantes (tenants).
 */
@Service
public class RestauranteService {

    private final RestauranteRepository restauranteRepository;

    public RestauranteService(RestauranteRepository restauranteRepository) {
        this.restauranteRepository = restauranteRepository;
    }

    @Transactional(readOnly = true)
    public Restaurante obtenerPorId(Long id) {
        return restauranteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Restaurante no encontrado: " + id));
    }

    /**
     * Resuelve el restaurante asociado a un número de WhatsApp.
     * En producción, cada restaurante tendría su propio número de WhatsApp Business.
     * Por ahora retorna el primer restaurante activo.
     */
    @Transactional(readOnly = true)
    public Restaurante resolverPorTelefonoWhatsApp(String phoneNumberId) {
        return restauranteRepository.findFirstByActivoTrue()
                .orElseThrow(() -> new IllegalStateException("No hay restaurantes activos"));
    }
}
