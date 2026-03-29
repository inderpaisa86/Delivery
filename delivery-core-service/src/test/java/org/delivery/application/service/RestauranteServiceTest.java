package org.delivery.application.service;

import org.delivery.application.dto.RestauranteRequest;
import org.delivery.application.dto.RestauranteResponse;
import org.delivery.domain.entity.Restaurante;
import org.delivery.infrastructure.persistence.repository.IRestauranteRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RestauranteService")
class RestauranteServiceTest {

    @Mock IRestauranteRepository restauranteRepository;
    @InjectMocks RestauranteService restauranteService;

    private Restaurante restaurante() {
        Restaurante r = new Restaurante();
        r.setId(1L); r.setNombre("Test"); r.setTelefono("573001000000");
        r.setDireccion("Calle 85"); r.setWhatsappPhoneId("phone-123");
        return r;
    }

    @Test
    @DisplayName("Crea restaurante")
    void shouldCreate() {
        when(restauranteRepository.save(any())).thenAnswer(inv -> { Restaurante r = inv.getArgument(0); r.setId(1L); return r; });
        RestauranteResponse res = restauranteService.crear(
                new RestauranteRequest("Test", "573001000000", "Calle 85", 4.6, -74.0, "phone-123"));
        assertEquals("Test", res.nombre());
    }

    @Test
    @DisplayName("Actualiza restaurante")
    void shouldUpdate() {
        Restaurante r = restaurante();
        when(restauranteRepository.findById(1L)).thenReturn(Optional.of(r));
        when(restauranteRepository.save(any())).thenReturn(r);

        RestauranteResponse res = restauranteService.actualizar(1L,
                new RestauranteRequest("Nuevo", "573009999999", "Calle 100", 4.7, -74.1, "phone-456"));
        assertEquals("Nuevo", res.nombre());
    }

    @Test
    @DisplayName("Lanza excepción si no existe al actualizar")
    void shouldThrowOnUpdateNotFound() {
        when(restauranteRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class,
                () -> restauranteService.actualizar(99L, new RestauranteRequest("x", null, null, null, null, null)));
    }

    @Test
    @DisplayName("Obtiene restaurante por ID")
    void shouldGetById() {
        Restaurante r = restaurante();
        when(restauranteRepository.findById(1L)).thenReturn(Optional.of(r));
        assertEquals("Test", restauranteService.obtenerPorId(1L).nombre());
    }

    @Test
    @DisplayName("Lanza excepción si no existe al obtener")
    void shouldThrowOnGetNotFound() {
        when(restauranteRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> restauranteService.obtenerPorId(99L));
    }

    @Test
    @DisplayName("Lista restaurantes activos")
    void shouldListActive() {
        when(restauranteRepository.findByActivoTrue()).thenReturn(List.of(restaurante()));
        assertEquals(1, restauranteService.listarActivos().size());
    }

    @Test
    @DisplayName("Resuelve por whatsappPhoneId")
    void shouldResolveByPhoneId() {
        Restaurante r = restaurante();
        when(restauranteRepository.findByWhatsappPhoneIdAndActivoTrue("phone-123")).thenReturn(Optional.of(r));
        assertNotNull(restauranteService.resolverPorTelefonoWhatsApp("phone-123"));
    }

    @Test
    @DisplayName("Fallback al primer activo si no encuentra por phoneId")
    void shouldFallbackToFirstActive() {
        Restaurante r = restaurante();
        when(restauranteRepository.findByWhatsappPhoneIdAndActivoTrue("unknown")).thenReturn(Optional.empty());
        when(restauranteRepository.findFirstByActivoTrue()).thenReturn(Optional.of(r));
        assertNotNull(restauranteService.resolverPorTelefonoWhatsApp("unknown"));
    }

    @Test
    @DisplayName("Lanza excepción si no hay restaurantes activos")
    void shouldThrowWhenNoActive() {
        when(restauranteRepository.findByWhatsappPhoneIdAndActivoTrue(anyString())).thenReturn(Optional.empty());
        when(restauranteRepository.findFirstByActivoTrue()).thenReturn(Optional.empty());
        assertThrows(IllegalStateException.class, () -> restauranteService.resolverPorTelefonoWhatsApp("x"));
    }
}
