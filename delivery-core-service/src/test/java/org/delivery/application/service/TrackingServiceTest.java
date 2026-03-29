package org.delivery.application.service;

import org.delivery.application.dto.TrackingResponse;
import org.delivery.domain.entity.*;
import org.delivery.domain.enums.EstadoPedido;
import org.delivery.infrastructure.persistence.repository.AsignacionDomicilioRepository;
import org.delivery.infrastructure.persistence.repository.PedidoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TrackingService")
class TrackingServiceTest {

    @Mock PedidoRepository pedidoRepository;
    @Mock AsignacionDomicilioRepository asignacionRepository;
    @InjectMocks TrackingService trackingService;

    @Test
    @DisplayName("Obtiene tracking con domiciliario asignado")
    void shouldGetTrackingWithDomiciliario() {
        Restaurante r = new Restaurante(); r.setId(1L);
        Cliente c = new Cliente(); c.setTelefono("573001234567"); c.setRestaurante(r);
        Pedido p = Pedido.builder().cliente(c).restaurante(r).direccion("Calle 100")
                .lat(4.6).lng(-74.0).build();
        p.setId(1L); p.setEstado(EstadoPedido.EN_CAMINO);

        Domiciliario d = new Domiciliario();
        d.setId(1L); d.setNombre("Carlos"); d.setLat(4.61); d.setLng(-74.01);

        when(pedidoRepository.findByTrackingToken("token-abc")).thenReturn(Optional.of(p));
        when(asignacionRepository.findByPedidoId(1L))
                .thenReturn(Optional.of(new AsignacionDomicilio(p, d)));

        TrackingResponse response = trackingService.obtenerTracking("token-abc");

        assertEquals(EstadoPedido.EN_CAMINO, response.estado());
        assertEquals("Carlos", response.domiciliarioNombre());
        assertEquals(4.61, response.domiciliarioLat());
    }

    @Test
    @DisplayName("Obtiene tracking sin domiciliario")
    void shouldGetTrackingWithoutDomiciliario() {
        Restaurante r = new Restaurante(); r.setId(1L);
        Cliente c = new Cliente(); c.setTelefono("573001234567"); c.setRestaurante(r);
        Pedido p = Pedido.builder().cliente(c).restaurante(r).direccion("Calle 100").build();
        p.setId(1L);

        when(pedidoRepository.findByTrackingToken("token-abc")).thenReturn(Optional.of(p));
        when(asignacionRepository.findByPedidoId(1L)).thenReturn(Optional.empty());

        TrackingResponse response = trackingService.obtenerTracking("token-abc");
        assertNull(response.domiciliarioNombre());
    }

    @Test
    @DisplayName("Lanza excepción con token inválido")
    void shouldThrowWithInvalidToken() {
        when(pedidoRepository.findByTrackingToken("bad")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> trackingService.obtenerTracking("bad"));
    }
}
