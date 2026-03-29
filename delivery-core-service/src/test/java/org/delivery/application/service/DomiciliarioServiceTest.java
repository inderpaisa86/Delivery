package org.delivery.application.service;

import org.delivery.application.dto.DomiciliarioRequest;
import org.delivery.application.dto.DomiciliarioResponse;
import org.delivery.application.dto.UbicacionRequest;
import org.delivery.application.dto.UbicacionResponse;
import org.delivery.domain.entity.Domiciliario;
import org.delivery.domain.entity.Restaurante;
import org.delivery.infrastructure.persistence.repository.DomiciliarioRepository;
import org.delivery.infrastructure.persistence.repository.RestauranteRepository;
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
@DisplayName("DomiciliarioService")
class DomiciliarioServiceTest {

    @Mock DomiciliarioRepository domiciliarioRepository;
    @Mock RestauranteRepository restauranteRepository;
    @InjectMocks DomiciliarioService domiciliarioService;

    private Restaurante restaurante() { Restaurante r = new Restaurante(); r.setId(1L); return r; }

    private Domiciliario domiciliario() {
        Domiciliario d = new Domiciliario();
        d.setId(1L); d.setNombre("Carlos"); d.setTelefono("573101111111");
        d.setLat(4.6); d.setLng(-74.0); d.setRestaurante(restaurante());
        return d;
    }

    @Test
    @DisplayName("Crea domiciliario")
    void shouldCreate() {
        Restaurante r = restaurante();
        when(restauranteRepository.findById(1L)).thenReturn(Optional.of(r));
        when(domiciliarioRepository.save(any())).thenAnswer(inv -> { Domiciliario d = inv.getArgument(0); d.setId(1L); return d; });

        DomiciliarioResponse res = domiciliarioService.crear(
                new DomiciliarioRequest(1L, "Carlos", "573101111111", 4.6, -74.0));
        assertEquals("Carlos", res.nombre());
    }

    @Test
    @DisplayName("Lanza excepción si restaurante no existe al crear")
    void shouldThrowOnCreateNoRestaurante() {
        when(restauranteRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class,
                () -> domiciliarioService.crear(new DomiciliarioRequest(99L, "x", "123", null, null)));
    }

    @Test
    @DisplayName("Actualiza domiciliario")
    void shouldUpdate() {
        Domiciliario d = domiciliario();
        when(domiciliarioRepository.findById(1L)).thenReturn(Optional.of(d));
        when(domiciliarioRepository.save(any())).thenReturn(d);

        DomiciliarioResponse res = domiciliarioService.actualizar(1L,
                new DomiciliarioRequest(1L, "Pedro", "573109999999", 4.7, -74.1));
        assertEquals("Pedro", res.nombre());
    }

    @Test
    @DisplayName("Lanza excepción si no existe al actualizar")
    void shouldThrowOnUpdateNotFound() {
        when(domiciliarioRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class,
                () -> domiciliarioService.actualizar(99L, new DomiciliarioRequest(1L, "x", "123", null, null)));
    }

    @Test
    @DisplayName("Actualiza ubicación")
    void shouldUpdateUbicacion() {
        Domiciliario d = domiciliario();
        when(domiciliarioRepository.findById(1L)).thenReturn(Optional.of(d));
        domiciliarioService.actualizarUbicacion(new UbicacionRequest(1L, 4.65, -74.05));
        assertEquals(4.65, d.getLat());
        verify(domiciliarioRepository).save(d);
    }

    @Test
    @DisplayName("Lanza excepción si no existe al actualizar ubicación")
    void shouldThrowOnUbicacionNotFound() {
        when(domiciliarioRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class,
                () -> domiciliarioService.actualizarUbicacion(new UbicacionRequest(99L, 4.6, -74.0)));
    }

    @Test
    @DisplayName("Obtiene ubicación")
    void shouldGetUbicacion() {
        Domiciliario d = domiciliario();
        when(domiciliarioRepository.findById(1L)).thenReturn(Optional.of(d));
        UbicacionResponse res = domiciliarioService.obtenerUbicacion(1L);
        assertEquals("Carlos", res.nombre());
    }

    @Test
    @DisplayName("Lanza excepción si no existe al obtener ubicación")
    void shouldThrowOnGetUbicacionNotFound() {
        when(domiciliarioRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> domiciliarioService.obtenerUbicacion(99L));
    }

    @Test
    @DisplayName("Obtiene disponibles por restaurante")
    void shouldGetDisponibles() {
        Domiciliario d = domiciliario();
        when(domiciliarioRepository.findByDisponibleTrueAndRestauranteIdAndLatIsNotNullAndLngIsNotNull(1L))
                .thenReturn(List.of(d));
        assertEquals(1, domiciliarioService.obtenerDisponibles(1L).size());
    }
}
