package org.delivery.application.dto;

public record DomiciliarioResponse(
        Long id,
        String nombre,
        String telefono,
        boolean disponible,
        Double lat,
        Double lng,
        Long restauranteId
) {}
