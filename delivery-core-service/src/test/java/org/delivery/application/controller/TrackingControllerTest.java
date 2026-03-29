package org.delivery.application.controller;

import org.delivery.application.dto.TrackingResponse;
import org.delivery.application.dto.UbicacionResponse;
import org.delivery.application.service.TrackingService;
import org.delivery.domain.enums.EstadoPedido;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("TrackingController")
class TrackingControllerTest {

    private final TrackingService trackingService = mock(TrackingService.class);
    private final TrackingController controller = new TrackingController(trackingService);

    @Test
    @DisplayName("Obtener tracking retorna 200")
    void shouldReturnTracking() {
        when(trackingService.obtenerTracking("token"))
                .thenReturn(new TrackingResponse(1L, EstadoPedido.EN_CAMINO, "Dir",
                        4.6, -74.0, 4.61, -74.01, "Carlos"));
        var result = controller.obtenerTracking("token");
        assertEquals(200, result.getStatusCode().value());
    }

    @Test
    @DisplayName("Obtener ubicación retorna 200")
    void shouldReturnUbicacion() {
        when(trackingService.obtenerUbicacion(1L))
                .thenReturn(new UbicacionResponse(1L, "Carlos", 4.6, -74.0));
        var result = controller.obtenerUbicacion(1L);
        assertEquals(200, result.getStatusCode().value());
    }
}
