package org.delivery.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.delivery.domain.enums.EstadoPedido;

public record PedidoResponse(
        Long id,
        String clienteTelefono,
        String clienteNombre,
        String direccion,
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
