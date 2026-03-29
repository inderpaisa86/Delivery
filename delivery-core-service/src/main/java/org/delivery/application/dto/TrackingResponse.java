package org.delivery.application.dto;

import org.delivery.domain.enums.EstadoPedido;

public record TrackingResponse(
        Long pedidoId,
        EstadoPedido estado,
        String direccion,
        Double pedidoLat,
        Double pedidoLng,
        Double domiciliarioLat,
        Double domiciliarioLng,
        String domiciliarioNombre
) {}
