package org.delivery.interfaces.rest;

import org.delivery.application.dto.DomiciliarioRequest;
import org.delivery.application.dto.DomiciliarioResponse;
import org.delivery.application.dto.UbicacionRequest;
import org.delivery.application.service.DomiciliarioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("DomiciliarioController")
class DomiciliarioControllerTest {

    private final DomiciliarioService service = mock(DomiciliarioService.class);
    private final DomiciliarioController controller = new DomiciliarioController(service);

    private DomiciliarioResponse response() {
        return new DomiciliarioResponse(1L, "Carlos", "573101111111", true, 4.6, -74.0, 1L);
    }

    @Test void shouldCreate() {
        when(service.crear(any())).thenReturn(response());
        assertEquals(201, controller.crear(new DomiciliarioRequest(1L, "Carlos", "573101111111", 4.6, -74.0)).getStatusCode().value());
    }

    @Test void shouldUpdate() {
        when(service.actualizar(eq(1L), any())).thenReturn(response());
        assertEquals(200, controller.actualizar(1L, new DomiciliarioRequest(1L, "Pedro", "573109999999", 4.7, -74.1)).getStatusCode().value());
    }

    @Test void shouldGetDisponibles() {
        when(service.obtenerDisponibles(1L)).thenReturn(List.of(response()));
        assertEquals(200, controller.obtenerDisponibles(1L).getStatusCode().value());
    }

    @Test void shouldUpdateUbicacion() {
        assertEquals(200, controller.actualizarUbicacion(new UbicacionRequest(1L, 4.6, -74.0)).getStatusCode().value());
        verify(service).actualizarUbicacion(any());
    }
}
