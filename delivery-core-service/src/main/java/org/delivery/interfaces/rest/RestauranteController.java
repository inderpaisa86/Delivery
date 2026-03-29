package org.delivery.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.delivery.application.dto.RestauranteRequest;
import org.delivery.application.dto.RestauranteResponse;
import org.delivery.application.service.RestauranteService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/restaurantes")
@Tag(name = "Restaurantes", description = "Gestión de restaurantes (tenants)")
@RequiredArgsConstructor
public class RestauranteController {

    private final RestauranteService restauranteService;

    @PostMapping
    @Operation(summary = "Crear restaurante")
    public ResponseEntity<RestauranteResponse> crear(@Valid @RequestBody RestauranteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(restauranteService.crear(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar restaurante")
    public ResponseEntity<RestauranteResponse> actualizar(
            @PathVariable Long id, @Valid @RequestBody RestauranteRequest request) {
        return ResponseEntity.ok(restauranteService.actualizar(id, request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener restaurante")
    public ResponseEntity<RestauranteResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(restauranteService.obtenerPorId(id));
    }

    @GetMapping
    @Operation(summary = "Listar restaurantes activos")
    public ResponseEntity<List<RestauranteResponse>> listar() {
        return ResponseEntity.ok(restauranteService.listarActivos());
    }
}
