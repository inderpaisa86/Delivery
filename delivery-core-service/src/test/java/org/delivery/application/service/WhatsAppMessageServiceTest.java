package org.delivery.application.service;

import org.delivery.application.dto.PedidoResponse;
import org.delivery.application.dto.WhatsAppMessage;
import org.delivery.application.port.IWhatsAppPort;
import org.delivery.domain.entity.Producto;
import org.delivery.domain.entity.Restaurante;
import org.delivery.domain.enums.EstadoPedido;
import org.delivery.infrastructure.persistence.repository.IProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

    @Mock IProductoRepository productoRepository;
    @Mock PedidoService pedidoService;
    @Mock RestauranteService restauranteService;
    @Mock IWhatsAppPort whatsAppPort;
    WhatsAppMessageService service;

    @BeforeEach
    void setUp() {
        service = new WhatsAppMessageService(
                productoRepository, pedidoService, restauranteService, whatsAppPort, "test-verify-token");
    }

    private Restaurante restaurante() {
        Restaurante r = new Restaurante(); r.setId(1L); r.setNombre("Test"); return r;
    }

    @Test
    @DisplayName("Procesa pedido válido y envía resumen")
    void shouldProcessAndSendSummary() {
        Restaurante r = restaurante();
        Producto p = new Producto(); p.setId(1L); p.setNombre("hamburguesa");

        when(restauranteService.resolverPorTelefonoWhatsApp(anyString())).thenReturn(r);
        when(productoRepository.findByNombreIgnoreCase("hamburguesa")).thenReturn(Optional.of(p));
        when(pedidoService.crearPedido(any())).thenReturn(
                new PedidoResponse(1L, 1L, "Test", "573001234567", null, "Dir",
                        EstadoPedido.NUEVO, BigDecimal.valueOf(30000), "token",
                        LocalDateTime.now(), List.of(
                                new PedidoResponse.DetalleResponse("hamburguesa", 2, BigDecimal.valueOf(15000)))));

        service.procesarMensaje(new WhatsAppMessage("573001234567", "2 hamburguesa"));

        verify(pedidoService).crearPedido(any());
        verify(whatsAppPort).enviarMensaje(eq("573001234567"), contains("Pedido #1"));
    }

    @Test
    @DisplayName("Envía menú cuando escribe 'menu'")
    void shouldSendMenu() {
        Restaurante r = restaurante();
        Producto p = new Producto(); p.setId(1L); p.setNombre("hamburguesa");
        p.setPrecio(BigDecimal.valueOf(15000));

        when(restauranteService.resolverPorTelefonoWhatsApp(anyString())).thenReturn(r);
        when(productoRepository.findByActivoTrueAndRestauranteId(1L)).thenReturn(List.of(p));

        service.procesarMensaje(new WhatsAppMessage("573001234567", "menu"));

        verify(whatsAppPort).enviarMensaje(eq("573001234567"), contains("Menú disponible"));
        verify(pedidoService, never()).crearPedido(any());
    }

    @Test
    @DisplayName("Envía menú cuando escribe 'menú' con tilde")
    void shouldSendMenuWithAccent() {
        Restaurante r = restaurante();
        when(restauranteService.resolverPorTelefonoWhatsApp(anyString())).thenReturn(r);
        when(productoRepository.findByActivoTrueAndRestauranteId(1L)).thenReturn(List.of());

        service.procesarMensaje(new WhatsAppMessage("573001234567", "menú"));

        verify(whatsAppPort).enviarMensaje(eq("573001234567"), contains("No hay productos"));
    }

    @Test
    @DisplayName("Envía ayuda si no detecta productos")
    void shouldSendHelp() {
        Restaurante r = restaurante();
        when(restauranteService.resolverPorTelefonoWhatsApp(anyString())).thenReturn(r);

        service.procesarMensaje(new WhatsAppMessage("573001234567", "hola"));

        verify(whatsAppPort).enviarMensaje(eq("573001234567"), contains("Hola"));
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
    @DisplayName("Envía error si falla la creación")
    void shouldSendErrorOnFailure() {
        Restaurante r = restaurante();
        Producto p = new Producto(); p.setId(1L); p.setNombre("pizza");

        when(restauranteService.resolverPorTelefonoWhatsApp(anyString())).thenReturn(r);
        when(productoRepository.findByNombreIgnoreCase("pizza")).thenReturn(Optional.of(p));
        when(pedidoService.crearPedido(any())).thenThrow(new RuntimeException("DB error"));

        service.procesarMensaje(new WhatsAppMessage("573001234567", "1 pizza"));
        verify(whatsAppPort).enviarMensaje(eq("573001234567"), contains("No pudimos"));
    }

    @Test
    @DisplayName("Ignora texto muy largo")
    void shouldIgnoreLongText() {
        Restaurante r = restaurante();
        when(restauranteService.resolverPorTelefonoWhatsApp(anyString())).thenReturn(r);

        service.procesarMensaje(new WhatsAppMessage("573001234567", "x".repeat(600)));
        verify(whatsAppPort).enviarMensaje(eq("573001234567"), contains("Hola"));
    }

    @Test
    @DisplayName("Ignora cantidades inválidas")
    void shouldIgnoreInvalidQuantities() {
        Restaurante r = restaurante();
        when(restauranteService.resolverPorTelefonoWhatsApp(anyString())).thenReturn(r);

        service.procesarMensaje(new WhatsAppMessage("573001234567", "abc hamburguesa"));
        verify(whatsAppPort).enviarMensaje(eq("573001234567"), contains("Hola"));
    }
}
