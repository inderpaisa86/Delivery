package org.delivery.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.delivery.domain.enums.EstadoPedido;

public record PedidoResponse(
        Long id,
        Integer numeroDiario,
        Long restauranteId,
        String restauranteNombre,
        String clienteTelefono,
        String clienteNombre,
        String direccion,
        Double lat,
        Double lng,
        EstadoPedido estado,
        BigDecimal total,
        String trackingToken,
        LocalDateTime fecha,
        List<DetalleResponse> detalles
) {
    public record DetalleResponse(
            String producto,
            int cantidad,
            BigDecimal precio
    ) {}
}
