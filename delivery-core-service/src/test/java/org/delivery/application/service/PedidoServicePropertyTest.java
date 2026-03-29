package org.delivery.application.service;

import net.jqwik.api.*;
import net.jqwik.api.constraints.*;
import org.delivery.application.dto.DetallePedidoRequest;
import org.delivery.application.dto.PedidoRequest;
import org.delivery.application.dto.PedidoResponse;
import org.delivery.application.port.IWhatsAppPort;
import org.delivery.domain.entity.*;
import org.delivery.infrastructure.persistence.repository.*;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Feature: delivery-core-refactor, Property 4: Token de tracking es UUID válido y único
 *
 * Validates: Requirements 5.1
 */
class PedidoServicePropertyTest {

    private final IPedidoRepository pedidoRepository = mock(IPedidoRepository.class);
    private final IClienteRepository clienteRepository = mock(IClienteRepository.class);
    private final IProductoRepository productoRepository = mock(IProductoRepository.class);
    private final IRestauranteRepository restauranteRepository = mock(IRestauranteRepository.class);
    private final PedidoStateMachine stateMachine = mock(PedidoStateMachine.class);
    private final IWhatsAppPort whatsAppPort = mock(IWhatsAppPort.class);

    private final PedidoService pedidoService = new PedidoService(
            pedidoRepository, clienteRepository, productoRepository,
            restauranteRepository, stateMachine, whatsAppPort);

    @Property(tries = 100)
    void trackingTokenEsUuidValidoYNoNulo(
            @ForAll("pedidoRequests") PedidoRequest request
    ) {
        // Arrange
        Restaurante restaurante = new Restaurante();
        restaurante.setId(request.restauranteId());
        restaurante.setNombre("Restaurante " + request.restauranteId());

        Cliente cliente = new Cliente();
        cliente.setId(1L);
        cliente.setTelefono(request.telefono());
        cliente.setNombre(request.nombre());
        cliente.setRestaurante(restaurante);

        when(restauranteRepository.findById(request.restauranteId()))
                .thenReturn(Optional.of(restaurante));
        when(clienteRepository.findByTelefonoAndRestauranteId(eq(request.telefono()), eq(request.restauranteId())))
                .thenReturn(Optional.of(cliente));

        for (DetallePedidoRequest detalle : request.detalles()) {
            Producto producto = new Producto();
            producto.setId(detalle.productoId());
            producto.setNombre("Producto " + detalle.productoId());
            producto.setPrecio(BigDecimal.valueOf(10000));
            when(productoRepository.findById(detalle.productoId()))
                    .thenReturn(Optional.of(producto));
        }

        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> {
            Pedido p = inv.getArgument(0);
            p.setId((long) (Math.random() * 10000));
            return p;
        });

        // Act
        PedidoResponse response = pedidoService.crearPedido(request);

        // Assert — trackingToken is non-null and valid UUID
        assertNotNull(response.trackingToken(), "trackingToken no debe ser nulo");
        assertFalse(response.trackingToken().isBlank(), "trackingToken no debe estar vacío");
        assertDoesNotThrow(
                () -> UUID.fromString(response.trackingToken()),
                "trackingToken debe ser un UUID válido: " + response.trackingToken());

        // Reset mocks for next iteration
        Mockito.reset(pedidoRepository, clienteRepository, productoRepository,
                restauranteRepository, whatsAppPort);
    }

    @Provide
    Arbitrary<PedidoRequest> pedidoRequests() {
        Arbitrary<Long> restauranteIds = Arbitraries.longs().between(1, 100);
        Arbitrary<String> telefonos = Arbitraries.strings()
                .numeric().ofMinLength(10).ofMaxLength(13)
                .map(s -> "57" + s);
        Arbitrary<String> nombres = Arbitraries.strings()
                .alpha().ofMinLength(2).ofMaxLength(20);
        Arbitrary<String> direcciones = Arbitraries.strings()
                .alpha().ofMinLength(5).ofMaxLength(50)
                .map(s -> "Calle " + s);
        Arbitrary<List<DetallePedidoRequest>> detalles = detalleRequests()
                .list().ofMinSize(1).ofMaxSize(5);

        return Combinators.combine(restauranteIds, telefonos, nombres, direcciones, detalles)
                .as((rId, tel, nom, dir, det) ->
                        new PedidoRequest(rId, tel, nom, dir, 4.6, -74.0, det));
    }

    Arbitrary<DetallePedidoRequest> detalleRequests() {
        Arbitrary<Long> productoIds = Arbitraries.longs().between(1, 50);
        Arbitrary<Integer> cantidades = Arbitraries.integers().between(1, 10);
        return Combinators.combine(productoIds, cantidades)
                .as(DetallePedidoRequest::new);
    }
}
