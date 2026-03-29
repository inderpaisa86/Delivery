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

import jakarta.validation.Valid;

/**
 * API REST de domiciliarios.
 */
@RestController
@RequestMapping("/domiciliarios")
public class DomiciliarioController {

    private final DomiciliarioService domiciliarioService;

    public DomiciliarioController(DomiciliarioService domiciliarioService) {
        this.domiciliarioService = domiciliarioService;
    }

    @GetMapping("/disponibles")
    public ResponseEntity<List<Domiciliario>> obtenerDisponibles(
            @RequestParam Long restauranteId) {
        return ResponseEntity.ok(domiciliarioService.obtenerDisponibles(restauranteId));
    }

    @PostMapping("/ubicacion")
    public ResponseEntity<Void> actualizarUbicacion(@Valid @RequestBody UbicacionRequest request) {
        domiciliarioService.actualizarUbicacion(request);
        return ResponseEntity.ok().build();
    }
}
