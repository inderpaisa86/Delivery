package org.delivery.application.service;

import lombok.RequiredArgsConstructor;
import org.delivery.application.dto.ProductoRequest;
import org.delivery.application.dto.ProductoResponse;
import org.delivery.domain.entity.Producto;
import org.delivery.domain.entity.Restaurante;
import org.delivery.infrastructure.persistence.repository.ProductoRepository;
import org.delivery.infrastructure.persistence.repository.RestauranteRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final RestauranteRepository restauranteRepository;

    @Transactional
    public ProductoResponse crear(ProductoRequest request) {
        Restaurante r = restauranteRepository.findById(request.restauranteId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Restaurante no encontrado: " + request.restauranteId()));

        Producto p = Producto.builder()
                .nombre(request.nombre())
                .precio(request.precio())
                .restaurante(r)
                .build();
        return toResponse(productoRepository.save(p));
    }

    @Transactional
    public ProductoResponse actualizar(Long id, ProductoRequest request) {
        Producto p = productoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + id));
        p.setNombre(request.nombre());
        p.setPrecio(request.precio());
        return toResponse(productoRepository.save(p));
    }

    @Transactional
    public void desactivar(Long id) {
        Producto p = productoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + id));
        p.setActivo(false);
        productoRepository.save(p);
    }

    @Transactional(readOnly = true)
    public Page<ProductoResponse> listarPorRestaurante(Long restauranteId, Pageable pageable) {
        return productoRepository.findByRestauranteIdAndActivoTrue(restauranteId, pageable)
                .map(this::toResponse);
    }

    private ProductoResponse toResponse(Producto p) {
        return new ProductoResponse(p.getId(), p.getNombre(), p.getPrecio(),
                p.isActivo(), p.getRestaurante().getId());
    }
}
