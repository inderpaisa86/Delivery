package org.delivery.application.dto;

import jakarta.validation.constraints.NotBlank;

public record RestauranteRequest(
        @NotBlank String nombre,
        String telefono,
        String direccion,
        Double lat,
        Double lng,
        String whatsappPhoneId
) {}
