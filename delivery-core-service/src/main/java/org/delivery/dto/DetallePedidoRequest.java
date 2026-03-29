package org.delivery.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record DetallePedidoRequest(
        @NotNull Long productoId,
        @Min(1) int cantidad
) {}
