package org.delivery.application.controller;

import org.delivery.application.dto.RestauranteRequest;
import org.delivery.application.dto.RestauranteResponse;
import org.delivery.application.service.RestauranteService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("RestauranteController")
class RestauranteControllerTest {

    private final RestauranteService service = mock(RestauranteService.class);
    private final RestauranteController controller = new RestauranteController(service);

    private RestauranteResponse response() {
        return new RestauranteResponse(1L, "Test", "573001000000", "Dir", 4.6, -74.0, true, "phone-123");
    }

    @Test void shouldCreate() {
        when(service.crear(any())).thenReturn(response());
        assertEquals(201, controller.crear(new RestauranteRequest("Test", null, null, null, null, null)).getStatusCode().value());
    }

    @Test void shouldUpdate() {
        when(service.actualizar(eq(1L), any())).thenReturn(response());
        assertEquals(200, controller.actualizar(1L, new RestauranteRequest("Test", null, null, null, null, null)).getStatusCode().value());
    }

    @Test void shouldGet() {
        when(service.obtenerPorId(1L)).thenReturn(response());
        assertEquals(200, controller.obtener(1L).getStatusCode().value());
    }

    @Test void shouldList() {
        when(service.listarActivos()).thenReturn(List.of(response()));
        assertEquals(200, controller.listar().getStatusCode().value());
    }
}
