package org.delivery.infrastructure.persistence.repository;

import java.util.Optional;

import org.delivery.domain.entity.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    Optional<Pedido> findByTrackingToken(String trackingToken);
}
