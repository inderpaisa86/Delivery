package org.delivery.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record ProductoRequest(
        @NotNull Long restauranteId,
        @NotBlank String nombre,
        @Positive BigDecimal precio
) {}
