package org.delivery.dto;

public record UbicacionResponse(
        Long domiciliarioId,
        String nombre,
        Double lat,
        Double lng
) {}
