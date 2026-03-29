package org.delivery.dto;

import jakarta.validation.constraints.NotNull;

public record UbicacionRequest(
        @NotNull Long domiciliarioId,
        @NotNull Double lat,
        @NotNull Double lng
) {}
