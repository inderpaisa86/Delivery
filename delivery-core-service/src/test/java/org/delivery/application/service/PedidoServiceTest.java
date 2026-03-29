package org.delivery.application.service;

import org.delivery.application.dto.*;
import org.delivery.application.port.WhatsAppPort;
import org.delivery.domain.entity.*;
import org.delivery.domain.enums.EstadoPedido;
import org.delivery.infrastructure.persistence.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PedidoService")
class PedidoServiceTest {

    @Mock PedidoRepository pedidoRepository;
    @Mock ClienteRepository clienteRepository;
    @Mock ProductoRepository productoRepository;
    @Mock RestauranteRepository restauranteRepository;
    @Mock PedidoStateMachine stateMachine;
    @Mock WhatsAppPort whatsAppPort;
    @InjectMocks PedidoService pedidoService;

    private Restaurante restaurante() {
        Restaurante r = new Restaurante();
        r.setId(1L);
        r.setNombre("Test Restaurant");
        return r;
    }

    private Cliente cliente(Restaurante r) {
        Cliente c = new Cliente();
        c.setId(1L);
        c.setTelefono("573001234567");
        c.setNombre("Juan");
        c.setRestaurante(r);
        return c;
    }

    private Producto producto() {
        Producto p = new Producto();
        p.setId(1L);
        p.setNombre("hamburguesa");
        p.setPrecio(BigDecimal.valueOf(15000));
        return p;
    }

    private Pedido pedido(Cliente c, Restaurante r) {
        Pedido p = Pedido.builder()
                .cliente(c).restaurante(r)
                .direccion("Calle 100").trackingToken("token-123")
                .build();
        p.setId(1L);
        p.addDetalle(new DetallePedido(producto(), 2));
        p.calcularTotal();
        return p;
    }

    @Test
    @DisplayName("Crea pedido con cliente existente")
    void shouldCreatePedidoWithExistingClient() {
        Restaurante r = restaurante();
        Cliente c = cliente(r);
        Producto prod = producto();

        when(restauranteRepository.findById(1L)).thenReturn(Optional.of(r));
        when(clienteRepository.findByTelefonoAndRestauranteId("573001234567", 1L))
                .thenReturn(Optional.of(c));
        when(productoRepository.findById(1L)).thenReturn(Optional.of(prod));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> {
            Pedido p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        PedidoRequest request = new PedidoRequest(1L, "573001234567", "Juan",
                "Calle 100", 4.6, -74.0, List.of(new DetallePedidoRequest(1L, 2)));

        PedidoResponse response = pedidoService.crearPedido(request);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals(BigDecimal.valueOf(30000), response.total());
        assertNotNull(response.trackingToken(), "trackingToken debe ser generado");
        assertDoesNotThrow(() -> java.util.UUID.fromString(response.trackingToken()),
                "trackingToken debe ser un UUID válido");
        verify(whatsAppPort).notificarCambioEstado("573001234567", 1L, EstadoPedido.NUEVO);
    }

    @Test
    @DisplayName("Crea pedido con cliente nuevo")
    void shouldCreatePedidoWithNewClient() {
        Restaurante r = restaurante();
        Producto prod = producto();

        when(restauranteRepository.findById(1L)).thenReturn(Optional.of(r));
        when(clienteRepository.findByTelefonoAndRestauranteId(anyString(), anyLong()))
                .thenReturn(Optional.empty());
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> {
            Cliente c = inv.getArgument(0);
            c.setId(2L);
            return c;
        });
        when(productoRepository.findById(1L)).thenReturn(Optional.of(prod));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> {
            Pedido p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        PedidoRequest request = new PedidoRequest(1L, "573009999999", null,
                "Calle 50", null, null, List.of(new DetallePedidoRequest(1L, 1)));

        PedidoResponse response = pedidoService.crearPedido(request);
        assertNotNull(response);
        verify(clienteRepository).save(any(Cliente.class));
    }

    @Test
    @DisplayName("Lanza excepción si restaurante no existe")
    void shouldThrowWhenRestauranteNotFound() {
        when(restauranteRepository.findById(99L)).thenReturn(Optional.empty());
        PedidoRequest request = new PedidoRequest(99L, "573001234567", null,
                "Dir", null, null, List.of(new DetallePedidoRequest(1L, 1)));

        assertThrows(IllegalArgumentException.class, () -> pedidoService.crearPedido(request));
    }

    @Test
    @DisplayName("Lanza excepción si producto no existe")
    void shouldThrowWhenProductoNotFound() {
        Restaurante r = restaurante();
        Cliente c = cliente(r);
        when(restauranteRepository.findById(1L)).thenReturn(Optional.of(r));
        when(clienteRepository.findByTelefonoAndRestauranteId(anyString(), anyLong()))
                .thenReturn(Optional.of(c));
        when(productoRepository.findById(99L)).thenReturn(Optional.empty());

        PedidoRequest request = new PedidoRequest(1L, "573001234567", null,
                "Dir", null, null, List.of(new DetallePedidoRequest(99L, 1)));

        assertThrows(IllegalArgumentException.class, () -> pedidoService.crearPedido(request));
    }

    @Test
    @DisplayName("Obtiene pedido por ID")
    void shouldGetPedidoById() {
        Restaurante r = restaurante();
        Cliente c = cliente(r);
        Pedido p = pedido(c, r);
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(p));

        PedidoResponse response = pedidoService.obtenerPedido(1L);
        assertEquals(1L, response.id());
    }

    @Test
    @DisplayName("Lanza excepción si pedido no existe al obtener")
    void shouldThrowWhenPedidoNotFoundOnGet() {
        when(pedidoRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> pedidoService.obtenerPedido(99L));
    }

    @Test
    @DisplayName("Cambia estado del pedido")
    void shouldChangeEstado() {
        Restaurante r = restaurante();
        Cliente c = cliente(r);
        Pedido p = pedido(c, r);
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(p));
        when(pedidoRepository.save(any(Pedido.class))).thenReturn(p);

        PedidoResponse response = pedidoService.cambiarEstado(1L, EstadoPedido.CONFIRMADO);
        assertNotNull(response);
        verify(stateMachine).cambiarEstado(p, EstadoPedido.CONFIRMADO);
        verify(whatsAppPort).notificarCambioEstado(eq("573001234567"), eq(1L), eq(EstadoPedido.CONFIRMADO));
    }

    @Test
    @DisplayName("Lanza excepción si pedido no existe al cambiar estado")
    void shouldThrowWhenPedidoNotFoundOnChangeEstado() {
        when(pedidoRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class,
                () -> pedidoService.cambiarEstado(99L, EstadoPedido.CONFIRMADO));
    }

    @Test
    @DisplayName("toResponse mapea correctamente todos los campos del Pedido a PedidoResponse")
    void shouldMapPedidoToResponseCorrectly() {
        Restaurante r = restaurante();
        Cliente c = cliente(r);
        Pedido p = pedido(c, r);
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(p));

        PedidoResponse response = pedidoService.obtenerPedido(1L);

        assertEquals(1L, response.id());
        assertEquals(1L, response.restauranteId());
        assertEquals("Test Restaurant", response.restauranteNombre());
        assertEquals("573001234567", response.clienteTelefono());
        assertEquals("Juan", response.clienteNombre());
        assertEquals("Calle 100", response.direccion());
        assertEquals(EstadoPedido.NUEVO, response.estado());
        assertEquals(BigDecimal.valueOf(30000), response.total());
        assertEquals("token-123", response.trackingToken());
        assertNotNull(response.fecha());
        assertNotNull(response.detalles());
        assertEquals(1, response.detalles().size());
        assertEquals("hamburguesa", response.detalles().get(0).producto());
        assertEquals(2, response.detalles().get(0).cantidad());
        assertEquals(BigDecimal.valueOf(15000), response.detalles().get(0).precio());
    }
}
