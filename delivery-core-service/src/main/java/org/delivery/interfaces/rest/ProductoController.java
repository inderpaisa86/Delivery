package org.delivery.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.delivery.application.dto.ProductoRequest;
import org.delivery.application.dto.ProductoResponse;
import org.delivery.application.service.ProductoService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/productos")
@Tag(name = "Productos", description = "Gestión del menú de productos")
@RequiredArgsConstructor
public class ProductoController {

    private final ProductoService productoService;

    @PostMapping
    @Operation(summary = "Crear producto")
    public ResponseEntity<ProductoResponse> crear(@Valid @RequestBody ProductoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productoService.crear(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar producto")
    public ResponseEntity<ProductoResponse> actualizar(
            @PathVariable Long id, @Valid @RequestBody ProductoRequest request) {
        return ResponseEntity.ok(productoService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Desactivar producto")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        productoService.desactivar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(summary = "Listar productos por restaurante (paginado)")
    public ResponseEntity<Page<ProductoResponse>> listar(
            @RequestParam Long restauranteId, Pageable pageable) {
        return ResponseEntity.ok(productoService.listarPorRestaurante(restauranteId, pageable));
    }
}
