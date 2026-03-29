package org.delivery.controller;

import java.util.List;

import org.delivery.domain.Domiciliario;
import org.delivery.dto.UbicacionRequest;
import org.delivery.repository.DomiciliarioRepository;
import org.delivery.service.TrackingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * API REST para gestión de domiciliarios.
 */
@RestController
@RequestMapping("/domiciliarios")
public class DomiciliarioController {

    private final DomiciliarioRepository domiciliarioRepository;
    private final TrackingService trackingService;

    public DomiciliarioController(DomiciliarioRepository domiciliarioRepository,
                                  TrackingService trackingService) {
        this.domiciliarioRepository = domiciliarioRepository;
        this.trackingService = trackingService;
    }

    @GetMapping("/disponibles")
    public ResponseEntity<List<Domiciliario>> obtenerDisponibles() {
        return ResponseEntity.ok(
                domiciliarioRepository.findByDisponibleTrueAndLatIsNotNullAndLngIsNotNull());
    }

    @PostMapping("/ubicacion")
    public ResponseEntity<Void> actualizarUbicacion(@Valid @RequestBody UbicacionRequest request) {
        trackingService.actualizarUbicacion(request);
        return ResponseEntity.ok().build();
    }
}
