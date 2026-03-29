package org.delivery.controller;

import org.delivery.dto.TrackingResponse;
import org.delivery.dto.UbicacionResponse;
import org.delivery.service.TrackingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * API pública de tracking en tiempo real.
 * El token de tracking es único por pedido y se genera al crear el pedido.
 */
@RestController
public class TrackingController {

    private final TrackingService trackingService;

    public TrackingController(TrackingService trackingService) {
        this.trackingService = trackingService;
    }

    @GetMapping("/track/{token}")
    public ResponseEntity<TrackingResponse> obtenerTracking(@PathVariable String token) {
        return ResponseEntity.ok(trackingService.obtenerTracking(token));
    }

    @GetMapping("/ubicacion/{domiciliarioId}")
    public ResponseEntity<UbicacionResponse> obtenerUbicacion(@PathVariable Long domiciliarioId) {
        return ResponseEntity.ok(trackingService.obtenerUbicacion(domiciliarioId));
    }
}
