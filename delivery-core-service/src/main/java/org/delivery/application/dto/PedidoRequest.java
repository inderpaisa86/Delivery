package org.delivery.application.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record PedidoRequest(
        @NotNull Long restauranteId,
        @NotBlank String telefono,
        String nombre,
        @NotBlank String direccion,
        Double lat,
        Double lng,
        @NotEmpty List<DetallePedidoRequest> detalles
) {}
