package org.delivery.application.service;

import org.delivery.application.dto.UbicacionRequest;
import org.delivery.application.dto.UbicacionResponse;
import org.delivery.domain.entity.Domiciliario;
import org.delivery.infrastructure.persistence.repository.DomiciliarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DomiciliarioService")
class DomiciliarioServiceTest {

    @Mock DomiciliarioRepository domiciliarioRepository;
    @InjectMocks DomiciliarioService domiciliarioService;

    @Test
    @DisplayName("Actualiza ubicación")
    void shouldUpdateUbicacion() {
        Domiciliario d = new Domiciliario();
        d.setId(1L);
        when(domiciliarioRepository.findById(1L)).thenReturn(Optional.of(d));

        domiciliarioService.actualizarUbicacion(new UbicacionRequest(1L, 4.6, -74.0));

        assertEquals(4.6, d.getLat());
        assertEquals(-74.0, d.getLng());
        verify(domiciliarioRepository).save(d);
    }

    @Test
    @DisplayName("Lanza excepción si domiciliario no existe al actualizar")
    void shouldThrowWhenNotFoundOnUpdate() {
        when(domiciliarioRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class,
                () -> domiciliarioService.actualizarUbicacion(new UbicacionRequest(99L, 4.6, -74.0)));
    }

    @Test
    @DisplayName("Obtiene ubicación")
    void shouldGetUbicacion() {
        Domiciliario d = new Domiciliario();
        d.setId(1L); d.setNombre("Carlos"); d.setLat(4.6); d.setLng(-74.0);
        when(domiciliarioRepository.findById(1L)).thenReturn(Optional.of(d));

        UbicacionResponse response = domiciliarioService.obtenerUbicacion(1L);
        assertEquals("Carlos", response.nombre());
        assertEquals(4.6, response.lat());
    }

    @Test
    @DisplayName("Lanza excepción si domiciliario no existe al obtener ubicación")
    void shouldThrowWhenNotFoundOnGet() {
        when(domiciliarioRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> domiciliarioService.obtenerUbicacion(99L));
    }

    @Test
    @DisplayName("Obtiene disponibles por restaurante")
    void shouldGetDisponibles() {
        when(domiciliarioRepository.findByDisponibleTrueAndRestauranteIdAndLatIsNotNullAndLngIsNotNull(1L))
                .thenReturn(List.of(new Domiciliario()));
        assertEquals(1, domiciliarioService.obtenerDisponibles(1L).size());
    }
}
