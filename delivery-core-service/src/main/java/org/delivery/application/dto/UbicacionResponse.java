package org.delivery.application.dto;

public record UbicacionResponse(
        Long domiciliarioId,
        String nombre,
        Double lat,
        Double lng
) {}
