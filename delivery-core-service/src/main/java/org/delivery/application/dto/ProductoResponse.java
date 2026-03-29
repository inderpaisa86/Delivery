package org.delivery.application.dto;

import java.math.BigDecimal;

public record ProductoResponse(
        Long id,
        String nombre,
        BigDecimal precio,
        boolean activo,
        Long restauranteId
) {}
