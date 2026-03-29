package org.delivery.interfaces.rest;

import org.delivery.application.dto.PedidoResponse;
import org.delivery.application.service.PedidoService;
import org.delivery.domain.enums.EstadoPedido;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("PedidoController")
class PedidoControllerTest {

    private final PedidoService pedidoService = mock(PedidoService.class);
    private final PedidoController controller = new PedidoController(pedidoService);

    private PedidoResponse response() {
        return new PedidoResponse(1L, 1L, "Test", "573001234567", "Juan", "Dir",
                EstadoPedido.NUEVO, BigDecimal.valueOf(30000), "token",
                LocalDateTime.now(), List.of());
    }

    @Test
    @DisplayName("Crear pedido retorna 201")
    void shouldReturn201OnCreate() {
        when(pedidoService.crearPedido(any())).thenReturn(response());
        var result = controller.crearPedido(null);
        assertEquals(HttpStatus.CREATED, result.getStatusCode());
    }

    @Test
    @DisplayName("Obtener pedido retorna 200")
    void shouldReturn200OnGet() {
        when(pedidoService.obtenerPedido(1L)).thenReturn(response());
        var result = controller.obtenerPedido(1L);
        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    @DisplayName("Cambiar estado retorna 200")
    void shouldReturn200OnChangeEstado() {
        when(pedidoService.cambiarEstado(1L, EstadoPedido.CONFIRMADO)).thenReturn(response());
        var result = controller.cambiarEstado(1L, EstadoPedido.CONFIRMADO);
        assertEquals(HttpStatus.OK, result.getStatusCode());
    }
}
