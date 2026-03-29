package org.delivery.application.controller;

import org.delivery.application.dto.PedidoResponse;
import org.delivery.application.service.PedidoService;
import org.delivery.domain.enums.EstadoPedido;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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

    @Test void shouldCreate() {
        when(pedidoService.crearPedido(any())).thenReturn(response());
        assertEquals(HttpStatus.CREATED, controller.crearPedido(null).getStatusCode());
    }

    @Test void shouldGet() {
        when(pedidoService.obtenerPedido(1L)).thenReturn(response());
        assertEquals(HttpStatus.OK, controller.obtenerPedido(1L).getStatusCode());
    }

    @Test void shouldListByRestaurante() {
        Page<PedidoResponse> page = new PageImpl<>(List.of(response()));
        when(pedidoService.listarPorRestaurante(eq(1L), any())).thenReturn(page);
        assertEquals(HttpStatus.OK, controller.listarPorRestaurante(1L, PageRequest.of(0, 10)).getStatusCode());
    }

    @Test void shouldListByCliente() {
        Page<PedidoResponse> page = new PageImpl<>(List.of(response()));
        when(pedidoService.listarPorCliente(eq("573001234567"), any())).thenReturn(page);
        assertEquals(HttpStatus.OK, controller.listarPorCliente("573001234567", PageRequest.of(0, 10)).getStatusCode());
    }

    @Test void shouldChangeEstado() {
        when(pedidoService.cambiarEstado(1L, EstadoPedido.CONFIRMADO)).thenReturn(response());
        assertEquals(HttpStatus.OK, controller.cambiarEstado(1L, EstadoPedido.CONFIRMADO).getStatusCode());
    }
}
