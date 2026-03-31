package org.delivery.application.dto;

public record UsuarioResponse(
        Long id,
        String username,
        String foto,
        String rol,
        boolean activo,
        Long restauranteId,
        String restauranteNombre
) {}
