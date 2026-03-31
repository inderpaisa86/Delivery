package org.delivery.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UsuarioRequest(
        @NotBlank String username,
        @NotBlank String password,
        String foto,
        @NotNull Long restauranteId
) {}
