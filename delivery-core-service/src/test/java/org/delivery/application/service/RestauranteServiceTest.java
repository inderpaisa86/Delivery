package org.delivery.application.service;

import org.delivery.domain.entity.Restaurante;
import org.delivery.infrastructure.persistence.repository.RestauranteRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RestauranteService")
class RestauranteServiceTest {

    @Mock RestauranteRepository restauranteRepository;
    @InjectMocks RestauranteService restauranteService;

    @Test
    @DisplayName("Obtiene restaurante por ID")
    void shouldGetById() {
        Restaurante r = new Restaurante(); r.setId(1L); r.setNombre("Test");
        when(restauranteRepository.findById(1L)).thenReturn(Optional.of(r));
        assertEquals("Test", restauranteService.obtenerPorId(1L).getNombre());
    }

    @Test
    @DisplayName("Lanza excepción si no existe")
    void shouldThrowWhenNotFound() {
        when(restauranteRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> restauranteService.obtenerPorId(99L));
    }

    @Test
    @DisplayName("Resuelve restaurante por WhatsApp")
    void shouldResolveByWhatsApp() {
        Restaurante r = new Restaurante(); r.setId(1L);
        when(restauranteRepository.findFirstByActivoTrue()).thenReturn(Optional.of(r));
        assertNotNull(restauranteService.resolverPorTelefonoWhatsApp("573001234567"));
    }

    @Test
    @DisplayName("Lanza excepción si no hay restaurantes activos")
    void shouldThrowWhenNoActive() {
        when(restauranteRepository.findFirstByActivoTrue()).thenReturn(Optional.empty());
        assertThrows(IllegalStateException.class,
                () -> restauranteService.resolverPorTelefonoWhatsApp("573001234567"));
    }
}
