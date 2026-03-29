package org.delivery.infrastructure.persistence.repository;

import java.util.Optional;

import org.delivery.domain.entity.AsignacionDomicilio;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IAsignacionDomicilioRepository extends JpaRepository<AsignacionDomicilio, Long> {
    Optional<AsignacionDomicilio> findByPedidoId(Long pedidoId);
    boolean existsByPedidoId(Long pedidoId);
}
