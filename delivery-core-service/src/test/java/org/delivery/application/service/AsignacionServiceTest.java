package org.delivery.application.service;

import org.delivery.application.port.GeoPort;
import org.delivery.application.port.WhatsAppPort;
import org.delivery.domain.entity.*;
import org.delivery.domain.enums.EstadoAsignacion;
import org.delivery.domain.enums.EstadoPedido;
import org.delivery.infrastructure.persistence.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AsignacionService")
class AsignacionServiceTest {

    @Mock PedidoRepository pedidoRepository;
    @Mock DomiciliarioRepository domiciliarioRepository;
    @Mock AsignacionDomicilioRepository asignacionRepository;
    @Mock GeoPort geoPort;
    @Mock WhatsAppPort whatsAppPort;
    @InjectMocks AsignacionService asignacionService;

    private Restaurante restaurante() {
        Restaurante r = new Restaurante(); r.setId(1L); return r;
    }

    private Pedido pedido() {
        Restaurante r = restaurante();
        Cliente c = new Cliente(); c.setTelefono("573001234567"); c.setRestaurante(r);
        Pedido p = Pedido.builder().cliente(c).restaurante(r).lat(4.6).lng(-74.0).build();
        p.setId(1L);
        return p;
    }

    private Domiciliario domiciliario() {
        Domiciliario d = new Domiciliario();
        d.setId(1L); d.setNombre("Carlos"); d.setLat(4.61); d.setLng(-74.01);
        d.setDisponible(true);
        return d;
    }

    @Test
    @DisplayName("Asigna domiciliario más cercano")
    void shouldAssignClosestDomiciliario() {
        Pedido p = pedido();
        Domiciliario d = domiciliario();

        when(asignacionRepository.existsByPedidoId(1L)).thenReturn(false);
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(p));
        when(domiciliarioRepository.findByDisponibleTrueAndRestauranteIdAndLatIsNotNullAndLngIsNotNull(1L))
                .thenReturn(List.of(d));
        when(geoPort.calcularDistanciaKm(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(1.5);

        asignacionService.asignarDomiciliario(1L);

        verify(asignacionRepository).save(any(AsignacionDomicilio.class));
        verify(domiciliarioRepository).save(d);
        verify(pedidoRepository).save(p);
        verify(whatsAppPort).notificarCambioEstado("573001234567", 1L, EstadoPedido.EN_CAMINO);
    }

    @Test
    @DisplayName("No asigna si ya tiene domiciliario")
    void shouldNotAssignIfAlreadyAssigned() {
        when(asignacionRepository.existsByPedidoId(1L)).thenReturn(true);
        asignacionService.asignarDomiciliario(1L);
        verify(pedidoRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("No asigna si pedido sin coordenadas")
    void shouldNotAssignIfNoCoordinates() {
        Pedido p = pedido();
        p.setLat(null); p.setLng(null);
        when(asignacionRepository.existsByPedidoId(1L)).thenReturn(false);
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(p));

        asignacionService.asignarDomiciliario(1L);
        verify(domiciliarioRepository, never()).findByDisponibleTrueAndRestauranteIdAndLatIsNotNullAndLngIsNotNull(anyLong());
    }

    @Test
    @DisplayName("No asigna si no hay domiciliarios disponibles")
    void shouldNotAssignIfNoneAvailable() {
        Pedido p = pedido();
        when(asignacionRepository.existsByPedidoId(1L)).thenReturn(false);
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(p));
        when(domiciliarioRepository.findByDisponibleTrueAndRestauranteIdAndLatIsNotNullAndLngIsNotNull(1L))
                .thenReturn(List.of());

        asignacionService.asignarDomiciliario(1L);
        verify(asignacionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Libera domiciliario correctamente")
    void shouldReleaseDomiciliario() {
        Domiciliario d = domiciliario();
        d.setDisponible(false);
        Pedido p = pedido();
        AsignacionDomicilio a = new AsignacionDomicilio(p, d);

        when(asignacionRepository.findByPedidoId(1L)).thenReturn(Optional.of(a));

        asignacionService.liberarDomiciliario(1L);

        verify(domiciliarioRepository).save(d);
        verify(asignacionRepository).save(a);
        assertEquals(EstadoAsignacion.ENTREGADO, a.getEstado());
        assertTrue(d.isDisponible());
    }

    @Test
    @DisplayName("Liberar sin asignación no hace nada")
    void shouldDoNothingWhenNoAssignment() {
        when(asignacionRepository.findByPedidoId(99L)).thenReturn(Optional.empty());
        asignacionService.liberarDomiciliario(99L);
        verify(domiciliarioRepository, never()).save(any());
    }
}
