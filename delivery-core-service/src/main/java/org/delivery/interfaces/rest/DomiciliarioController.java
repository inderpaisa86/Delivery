package org.delivery.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.delivery.application.dto.UbicacionRequest;
import org.delivery.application.service.DomiciliarioService;
import org.delivery.domain.entity.Domiciliario;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/domiciliarios")
@Tag(name = "Domiciliarios", description = "Gestión de domiciliarios y ubicación")
@RequiredArgsConstructor
public class DomiciliarioController {

    private final DomiciliarioService domiciliarioService;

    @GetMapping("/disponibles")
    @Operation(summary = "Listar disponibles")
    public ResponseEntity<List<Domiciliario>> obtenerDisponibles(@RequestParam Long restauranteId) {
        return ResponseEntity.ok(domiciliarioService.obtenerDisponibles(restauranteId));
    }

    @PostMapping("/ubicacion")
    @Operation(summary = "Actualizar ubicación")
    public ResponseEntity<Void> actualizarUbicacion(@Valid @RequestBody UbicacionRequest request) {
        domiciliarioService.actualizarUbicacion(request);
        return ResponseEntity.ok().build();
    }
}
