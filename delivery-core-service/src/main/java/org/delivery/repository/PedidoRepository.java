package org.delivery.repository;

import java.util.Optional;

import org.delivery.domain.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    Optional<Pedido> findByTrackingToken(String trackingToken);
}
