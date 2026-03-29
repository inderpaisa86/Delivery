package org.delivery.application.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.delivery.application.dto.TrackingResponse;
import org.delivery.application.dto.UbicacionResponse;
import org.delivery.application.service.TrackingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "Tracking", description = "Tracking público de pedidos en tiempo real")
@RequiredArgsConstructor
public class TrackingController {

    private final TrackingService trackingService;

    @GetMapping("/track/{token}")
    @Operation(summary = "Tracking de pedido")
    public ResponseEntity<TrackingResponse> obtenerTracking(@PathVariable String token) {
        return ResponseEntity.ok(trackingService.obtenerTracking(token));
    }

    @GetMapping("/ubicacion/{domiciliarioId}")
    @Operation(summary = "Ubicación domiciliario")
    public ResponseEntity<UbicacionResponse> obtenerUbicacion(@PathVariable Long domiciliarioId) {
        return ResponseEntity.ok(trackingService.obtenerUbicacion(domiciliarioId));
    }
}
