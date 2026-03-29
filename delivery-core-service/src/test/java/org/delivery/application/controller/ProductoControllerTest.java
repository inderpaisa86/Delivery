package org.delivery.application.controller;

import org.delivery.application.dto.ProductoRequest;
import org.delivery.application.dto.ProductoResponse;
import org.delivery.application.service.ProductoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("ProductoController")
class ProductoControllerTest {

    private final ProductoService service = mock(ProductoService.class);
    private final ProductoController controller = new ProductoController(service);

    private ProductoResponse response() {
        return new ProductoResponse(1L, "hamburguesa", BigDecimal.valueOf(15000), true, 1L);
    }

    @Test void shouldCreate() {
        when(service.crear(any())).thenReturn(response());
        assertEquals(201, controller.crear(new ProductoRequest(1L, "hamburguesa", BigDecimal.valueOf(15000))).getStatusCode().value());
    }

    @Test void shouldUpdate() {
        when(service.actualizar(eq(1L), any())).thenReturn(response());
        assertEquals(200, controller.actualizar(1L, new ProductoRequest(1L, "pizza", BigDecimal.valueOf(25000))).getStatusCode().value());
    }

    @Test void shouldDeactivate() {
        assertEquals(204, controller.desactivar(1L).getStatusCode().value());
        verify(service).desactivar(1L);
    }

    @Test void shouldList() {
        Page<ProductoResponse> page = new PageImpl<>(List.of(response()));
        when(service.listarPorRestaurante(eq(1L), any())).thenReturn(page);
        assertEquals(200, controller.listar(1L, PageRequest.of(0, 10)).getStatusCode().value());
    }
}
