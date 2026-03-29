package org.delivery.interfaces.rest;

import org.delivery.application.dto.TrackingResponse;
import org.delivery.application.dto.UbicacionResponse;
import org.delivery.application.service.DomiciliarioService;
import org.delivery.application.service.TrackingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * API pública de tracking en tiempo real.
 */
@RestController
public class TrackingController {

    private final TrackingService trackingService;
    private final DomiciliarioService domiciliarioService;

    public TrackingController(TrackingService trackingService,
                              DomiciliarioService domiciliarioService) {
        this.trackingService = trackingService;
        this.domiciliarioService = domiciliarioService;
    }

    @GetMapping("/track/{token}")
    public ResponseEntity<TrackingResponse> obtenerTracking(@PathVariable String token) {
        return ResponseEntity.ok(trackingService.obtenerTracking(token));
    }

    @GetMapping("/ubicacion/{domiciliarioId}")
    public ResponseEntity<UbicacionResponse> obtenerUbicacion(@PathVariable Long domiciliarioId) {
        return ResponseEntity.ok(domiciliarioService.obtenerUbicacion(domiciliarioId));
    }
}
