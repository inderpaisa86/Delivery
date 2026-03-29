package org.delivery.interfaces.rest;

import java.util.List;

import org.delivery.application.dto.UbicacionRequest;
import org.delivery.application.service.DomiciliarioService;
import org.delivery.domain.entity.Domiciliario;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * API REST de domiciliarios.
 */
@RestController
@RequestMapping("/domiciliarios")
@Tag(name = "Domiciliarios", description = "Gestión de domiciliarios y ubicación")
public class DomiciliarioController {

    private final DomiciliarioService domiciliarioService;

    public DomiciliarioController(DomiciliarioService domiciliarioService) {
        this.domiciliarioService = domiciliarioService;
    }

    @GetMapping("/disponibles")
    @Operation(summary = "Listar disponibles", description = "Retorna domiciliarios disponibles de un restaurante")
    public ResponseEntity<List<Domiciliario>> obtenerDisponibles(
            @RequestParam Long restauranteId) {
        return ResponseEntity.ok(domiciliarioService.obtenerDisponibles(restauranteId));
    }

    @PostMapping("/ubicacion")
    @Operation(summary = "Actualizar ubicación", description = "El domiciliario envía su lat/lng en tiempo real")
    public ResponseEntity<Void> actualizarUbicacion(@Valid @RequestBody UbicacionRequest request) {
        domiciliarioService.actualizarUbicacion(request);
        return ResponseEntity.ok().build();
    }
}
