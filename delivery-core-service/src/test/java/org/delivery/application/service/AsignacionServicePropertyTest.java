package org.delivery.application.service;

import net.jqwik.api.*;
import net.jqwik.api.constraints.DoubleRange;
import net.jqwik.api.constraints.Size;
import org.delivery.application.port.GeoPort;
import org.delivery.application.port.WhatsAppPort;
import org.delivery.domain.entity.*;
import org.delivery.domain.enums.EstadoAsignacion;
import org.delivery.domain.enums.EstadoPedido;
import org.delivery.infrastructure.external.geo.HaversineAdapter;
import org.delivery.infrastructure.persistence.repository.*;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AsignacionServicePropertyTest {

    private final HaversineAdapter haversine = new HaversineAdapter();

    // Feature: delivery-core-refactor, Property 2: Selección del domiciliario más cercano
    // **Validates: Requirements 4.1, 4.2**
    @Property(tries = 100)
    void elDomiciliarioSeleccionadoTieneMenorDistancia(
            @ForAll("pedidoConCoordenadas") Pedido pedido,
            @ForAll("listaDomiciliarios") List<Domiciliario> domiciliarios
    ) {
        // Setup mocks
        PedidoRepository pedidoRepo = mock(PedidoRepository.class);
        DomiciliarioRepository domiciliarioRepo = mock(DomiciliarioRepository.class);
        AsignacionDomicilioRepository asignacionRepo = mock(AsignacionDomicilioRepository.class);
        WhatsAppPort whatsAppPort = mock(WhatsAppPort.class);

        // Use real Haversine for distance calculation
        GeoPort geoPort = haversine;

        AsignacionService service = new AsignacionService(
                pedidoRepo, domiciliarioRepo, asignacionRepo, geoPort, whatsAppPort
        );

        when(asignacionRepo.existsByPedidoId(anyLong())).thenReturn(false);
        when(pedidoRepo.findById(anyLong())).thenReturn(Optional.of(pedido));
        when(domiciliarioRepo.findByDisponibleTrueAndRestauranteIdAndLatIsNotNullAndLngIsNotNull(anyLong()))
                .thenReturn(domiciliarios);

        // Capture the saved assignment to identify which domiciliario was selected
        ArgumentCaptor<AsignacionDomicilio> captor = ArgumentCaptor.forClass(AsignacionDomicilio.class);

        service.asignarDomiciliario(pedido.getId());

        verify(asignacionRepo).save(captor.capture());
        Domiciliario selected = captor.getValue().getDomiciliario();

        // Calculate the distance of the selected domiciliario
        double selectedDistance = haversine.calcularDistanciaKm(
                pedido.getLat(), pedido.getLng(),
                selected.getLat(), selected.getLng()
        );

        // Verify that no other domiciliario has a strictly smaller distance
        for (Domiciliario d : domiciliarios) {
            double dist = haversine.calcularDistanciaKm(
                    pedido.getLat(), pedido.getLng(),
                    d.getLat(), d.getLng()
            );
            assertTrue(selectedDistance <= dist + 1e-9,
                    "Selected domiciliario (dist=" + selectedDistance +
                    ") should have distance <= all others, but found dist=" + dist);
        }
    }

    // Feature: delivery-core-refactor, Property 3: Asignación y liberación restaura disponibilidad
    // **Validates: Requirements 4.8**
    @Property(tries = 100)
    void asignarYLiberarRestauraDisponibilidad(
            @ForAll("pedidoConCoordenadas") Pedido pedido,
            @ForAll("domiciliarioDisponible") Domiciliario domiciliario
    ) {
        // Setup mocks
        PedidoRepository pedidoRepo = mock(PedidoRepository.class);
        DomiciliarioRepository domiciliarioRepo = mock(DomiciliarioRepository.class);
        AsignacionDomicilioRepository asignacionRepo = mock(AsignacionDomicilioRepository.class);
        WhatsAppPort whatsAppPort = mock(WhatsAppPort.class);
        GeoPort geoPort = haversine;

        AsignacionService service = new AsignacionService(
                pedidoRepo, domiciliarioRepo, asignacionRepo, geoPort, whatsAppPort
        );

        // Precondition: domiciliario is available
        assertTrue(domiciliario.isDisponible(), "Precondition: domiciliario should be available");

        // Phase 1: Assign
        when(asignacionRepo.existsByPedidoId(anyLong())).thenReturn(false);
        when(pedidoRepo.findById(anyLong())).thenReturn(Optional.of(pedido));
        when(domiciliarioRepo.findByDisponibleTrueAndRestauranteIdAndLatIsNotNullAndLngIsNotNull(anyLong()))
                .thenReturn(List.of(domiciliario));

        service.asignarDomiciliario(pedido.getId());

        // After assignment, domiciliario should NOT be available
        assertFalse(domiciliario.isDisponible(), "After assignment, domiciliario should not be available");

        // Phase 2: Release - capture the saved assignment to use for release
        ArgumentCaptor<AsignacionDomicilio> captor = ArgumentCaptor.forClass(AsignacionDomicilio.class);
        verify(asignacionRepo).save(captor.capture());
        AsignacionDomicilio asignacion = captor.getValue();

        when(asignacionRepo.findByPedidoId(anyLong())).thenReturn(Optional.of(asignacion));

        service.liberarDomiciliario(pedido.getId());

        // After release, domiciliario should be available again
        assertTrue(domiciliario.isDisponible(), "After release, domiciliario should be available again");
        assertEquals(EstadoAsignacion.ENTREGADO, asignacion.getEstado());
    }

    // --- Generators ---

    @Provide
    Arbitrary<Pedido> pedidoConCoordenadas() {
        return Combinators.combine(
                Arbitraries.doubles().between(-90.0, 90.0),
                Arbitraries.doubles().between(-180.0, 180.0),
                Arbitraries.longs().between(1L, 10000L)
        ).as((lat, lng, id) -> {
            Restaurante r = new Restaurante();
            r.setId(1L);
            Cliente c = new Cliente();
            c.setTelefono("573001234567");
            c.setRestaurante(r);
            Pedido p = Pedido.builder()
                    .cliente(c)
                    .restaurante(r)
                    .lat(lat)
                    .lng(lng)
                    .build();
            p.setId(id);
            return p;
        });
    }

    @Provide
    Arbitrary<List<Domiciliario>> listaDomiciliarios() {
        return domiciliarioDisponible()
                .list()
                .ofMinSize(1)
                .ofMaxSize(20)
                .map(list -> {
                    // Assign unique IDs
                    for (int i = 0; i < list.size(); i++) {
                        list.get(i).setId((long) (i + 1));
                    }
                    return list;
                });
    }

    @Provide
    Arbitrary<Domiciliario> domiciliarioDisponible() {
        return Combinators.combine(
                Arbitraries.doubles().between(-90.0, 90.0),
                Arbitraries.doubles().between(-180.0, 180.0),
                Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(10)
        ).as((lat, lng, nombre) -> {
            Domiciliario d = new Domiciliario();
            d.setId(1L);
            d.setNombre(nombre);
            d.setTelefono("573001234567");
            d.setLat(lat);
            d.setLng(lng);
            d.setDisponible(true);
            Restaurante r = new Restaurante();
            r.setId(1L);
            d.setRestaurante(r);
            return d;
        });
    }
}
