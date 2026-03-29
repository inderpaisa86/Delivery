package org.delivery.application.service;

import org.delivery.application.dto.PedidoResponse;
import org.delivery.application.dto.WhatsAppMessage;
import org.delivery.application.port.WhatsAppPort;
import org.delivery.domain.entity.Producto;
import org.delivery.domain.entity.Restaurante;
import org.delivery.domain.enums.EstadoPedido;
import org.delivery.infrastructure.persistence.repository.ProductoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WhatsAppMessageService")
class WhatsAppMessageServiceTest {

    @Mock ProductoRepository productoRepository;
    @Mock PedidoService pedidoService;
    @Mock RestauranteService restauranteService;
    @Mock WhatsAppPort whatsAppPort;
    @InjectMocks WhatsAppMessageService service;

    @Test
    @DisplayName("Procesa pedido válido desde WhatsApp")
    void shouldProcessValidOrder() {
        Restaurante r = new Restaurante(); r.setId(1L); r.setNombre("Test");
        Producto p = new Producto(); p.setId(1L); p.setNombre("hamburguesa");

        when(restauranteService.resolverPorTelefonoWhatsApp(anyString())).thenReturn(r);
        when(productoRepository.findByNombreIgnoreCase("hamburguesa")).thenReturn(Optional.of(p));
        when(pedidoService.crearPedido(any())).thenReturn(
                new PedidoResponse(1L, 1L, "Test", "573001234567", null, "Dir",
                        EstadoPedido.NUEVO, BigDecimal.valueOf(30000), "token",
                        LocalDateTime.now(), List.of()));

        service.procesarMensaje(new WhatsAppMessage("573001234567", "2 hamburguesa"));

        verify(pedidoService).crearPedido(any());
    }

    @Test
    @DisplayName("Envía ayuda si no detecta productos")
    void shouldSendHelpWhenNoProducts() {
        Restaurante r = new Restaurante(); r.setId(1L);
        when(restauranteService.resolverPorTelefonoWhatsApp(anyString())).thenReturn(r);

        service.procesarMensaje(new WhatsAppMessage("573001234567", "hola"));

        verify(whatsAppPort).enviarMensaje(eq("573001234567"), contains("Hola"));
        verify(pedidoService, never()).crearPedido(any());
    }

    @Test
    @DisplayName("No procesa si restaurante no se resuelve")
    void shouldNotProcessIfNoRestaurante() {
        when(restauranteService.resolverPorTelefonoWhatsApp(anyString()))
                .thenThrow(new IllegalStateException("No hay restaurantes"));

        service.procesarMensaje(new WhatsAppMessage("573001234567", "2 hamburguesa"));

        verify(pedidoService, never()).crearPedido(any());
    }

    @Test
    @DisplayName("Envía error si falla la creación del pedido")
    void shouldSendErrorOnFailure() {
        Restaurante r = new Restaurante(); r.setId(1L);
        Producto p = new Producto(); p.setId(1L); p.setNombre("pizza");

        when(restauranteService.resolverPorTelefonoWhatsApp(anyString())).thenReturn(r);
        when(productoRepository.findByNombreIgnoreCase("pizza")).thenReturn(Optional.of(p));
        when(pedidoService.crearPedido(any())).thenThrow(new RuntimeException("DB error"));

        service.procesarMensaje(new WhatsAppMessage("573001234567", "1 pizza"));

        verify(whatsAppPort).enviarMensaje(eq("573001234567"), contains("No pudimos"));
    }

    @Test
    @DisplayName("Ignora texto null o muy largo")
    void shouldIgnoreNullOrLongText() {
        Restaurante r = new Restaurante(); r.setId(1L);
        when(restauranteService.resolverPorTelefonoWhatsApp(anyString())).thenReturn(r);

        service.procesarMensaje(new WhatsAppMessage("573001234567", "x".repeat(600)));
        verify(whatsAppPort).enviarMensaje(eq("573001234567"), contains("Hola"));
    }

    @Test
    @DisplayName("Ignora cantidades inválidas")
    void shouldIgnoreInvalidQuantities() {
        Restaurante r = new Restaurante(); r.setId(1L);
        when(restauranteService.resolverPorTelefonoWhatsApp(anyString())).thenReturn(r);

        service.procesarMensaje(new WhatsAppMessage("573001234567", "abc hamburguesa"));
        verify(whatsAppPort).enviarMensaje(eq("573001234567"), contains("Hola"));
    }
}
