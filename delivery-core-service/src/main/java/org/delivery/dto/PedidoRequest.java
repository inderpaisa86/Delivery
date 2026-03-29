package org.delivery.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record PedidoRequest(
        @NotBlank String telefono,
        String nombre,
        @NotBlank String direccion,
        Double lat,
        Double lng,
        @NotEmpty List<DetallePedidoRequest> detalles
) {}
