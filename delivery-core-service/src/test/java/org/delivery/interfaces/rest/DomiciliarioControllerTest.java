package org.delivery.interfaces.rest;

import org.delivery.application.dto.UbicacionRequest;
import org.delivery.application.service.DomiciliarioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("DomiciliarioController")
class DomiciliarioControllerTest {

    private final DomiciliarioService domiciliarioService = mock(DomiciliarioService.class);
    private final DomiciliarioController controller = new DomiciliarioController(domiciliarioService);

    @Test
    @DisplayName("Obtener disponibles retorna 200")
    void shouldReturnDisponibles() {
        when(domiciliarioService.obtenerDisponibles(1L)).thenReturn(List.of());
        var result = controller.obtenerDisponibles(1L);
        assertEquals(200, result.getStatusCode().value());
    }

    @Test
    @DisplayName("Actualizar ubicación retorna 200")
    void shouldUpdateUbicacion() {
        var result = controller.actualizarUbicacion(new UbicacionRequest(1L, 4.6, -74.0));
        assertEquals(200, result.getStatusCode().value());
        verify(domiciliarioService).actualizarUbicacion(any());
    }
}
