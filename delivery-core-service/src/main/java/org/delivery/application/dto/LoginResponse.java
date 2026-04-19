package org.delivery.application.dto;

public record LoginResponse(
        Long userId,
        String username,
        String rol,
        String perfil,
        Long restauranteId,
        String restauranteNombre,
        String token
) {}
