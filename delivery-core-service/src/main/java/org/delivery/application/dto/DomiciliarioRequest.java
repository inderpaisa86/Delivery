package org.delivery.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DomiciliarioRequest(
        @NotNull Long restauranteId,
        @NotBlank String nombre,
        @NotBlank String telefono,
        Double lat,
        Double lng
) {}
