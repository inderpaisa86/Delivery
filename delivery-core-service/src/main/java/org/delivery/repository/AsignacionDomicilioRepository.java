package org.delivery.repository;

import java.util.Optional;

import org.delivery.domain.AsignacionDomicilio;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AsignacionDomicilioRepository extends JpaRepository<AsignacionDomicilio, Long> {
    Optional<AsignacionDomicilio> findByPedidoId(Long pedidoId);
    boolean existsByPedidoId(Long pedidoId);
}
