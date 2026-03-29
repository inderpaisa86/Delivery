package org.delivery.infrastructure.persistence.repository;

import org.delivery.domain.entity.Pedido;
import org.delivery.domain.enums.EstadoPedido;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IPedidoRepository extends JpaRepository<Pedido, Long> {
    Optional<Pedido> findByTrackingToken(String trackingToken);
    Page<Pedido> findByRestauranteId(Long restauranteId, Pageable pageable);
    Page<Pedido> findByClienteTelefono(String telefono, Pageable pageable);

    /** Busca el pedido más reciente de un cliente que esté pendiente de ubicación (lat es null) */
    Optional<Pedido> findFirstByClienteTelefonoAndEstadoAndLatIsNullOrderByFechaDesc(
            String telefono, EstadoPedido estado);
}
