package org.delivery.application.dto;

public record RestauranteResponse(
        Long id,
        String nombre,
        String telefono,
        String direccion,
        Double lat,
        Double lng,
        boolean activo,
        String whatsappPhoneId
) {}
