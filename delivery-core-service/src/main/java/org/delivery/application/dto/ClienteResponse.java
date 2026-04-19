package org.delivery.application.dto;

public record ClienteResponse(
        Long id,
        String telefono,
        String nombre,
        String direccion,
        Long restauranteId
) {}
