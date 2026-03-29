package org.delivery.application.service;

import org.delivery.application.dto.ProductoRequest;
import org.delivery.application.dto.ProductoResponse;
import org.delivery.domain.entity.Producto;
import org.delivery.domain.entity.Restaurante;
import org.delivery.infrastructure.persistence.repository.IProductoRepository;
import org.delivery.infrastructure.persistence.repository.IRestauranteRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductoService")
class ProductoServiceTest {

    @Mock IProductoRepository productoRepository;
    @Mock IRestauranteRepository restauranteRepository;
    @InjectMocks ProductoService productoService;

    private Restaurante restaurante() {
        Restaurante r = new Restaurante(); r.setId(1L); r.setNombre("Test"); return r;
    }

    private Producto producto(Restaurante r) {
        Producto p = Producto.builder().nombre("hamburguesa").precio(BigDecimal.valueOf(15000)).restaurante(r).build();
        p.setId(1L);
        return p;
    }

    @Test
    @DisplayName("Crea producto")
    void shouldCreate() {
        Restaurante r = restaurante();
        when(restauranteRepository.findById(1L)).thenReturn(Optional.of(r));
        when(productoRepository.save(any())).thenAnswer(inv -> { Producto p = inv.getArgument(0); p.setId(1L); return p; });

        ProductoResponse res = productoService.crear(new ProductoRequest(1L, "hamburguesa", BigDecimal.valueOf(15000)));
        assertEquals("hamburguesa", res.nombre());
        assertEquals(1L, res.restauranteId());
    }

    @Test
    @DisplayName("Lanza excepción si restaurante no existe al crear")
    void shouldThrowOnCreateNoRestaurante() {
        when(restauranteRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class,
                () -> productoService.crear(new ProductoRequest(99L, "x", BigDecimal.ONE)));
    }

    @Test
    @DisplayName("Actualiza producto")
    void shouldUpdate() {
        Restaurante r = restaurante();
        Producto p = producto(r);
        when(productoRepository.findById(1L)).thenReturn(Optional.of(p));
        when(productoRepository.save(any())).thenReturn(p);

        ProductoResponse res = productoService.actualizar(1L, new ProductoRequest(1L, "pizza", BigDecimal.valueOf(25000)));
        assertEquals("pizza", res.nombre());
    }

    @Test
    @DisplayName("Lanza excepción si producto no existe al actualizar")
    void shouldThrowOnUpdateNotFound() {
        when(productoRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class,
                () -> productoService.actualizar(99L, new ProductoRequest(1L, "x", BigDecimal.ONE)));
    }

    @Test
    @DisplayName("Desactiva producto")
    void shouldDeactivate() {
        Restaurante r = restaurante();
        Producto p = producto(r);
        when(productoRepository.findById(1L)).thenReturn(Optional.of(p));
        productoService.desactivar(1L);
        assertFalse(p.isActivo());
        verify(productoRepository).save(p);
    }

    @Test
    @DisplayName("Lanza excepción si producto no existe al desactivar")
    void shouldThrowOnDeactivateNotFound() {
        when(productoRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> productoService.desactivar(99L));
    }

    @Test
    @DisplayName("Lista productos paginados por restaurante")
    void shouldListPaginated() {
        Restaurante r = restaurante();
        Producto p = producto(r);
        Page<Producto> page = new PageImpl<>(List.of(p));
        when(productoRepository.findByRestauranteIdAndActivoTrue(eq(1L), any())).thenReturn(page);

        Page<ProductoResponse> result = productoService.listarPorRestaurante(1L, PageRequest.of(0, 10));
        assertEquals(1, result.getTotalElements());
    }
}
