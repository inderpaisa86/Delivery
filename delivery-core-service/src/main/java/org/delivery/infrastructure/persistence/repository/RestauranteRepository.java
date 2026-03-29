package org.delivery.infrastructure.persistence.repository;

import org.delivery.domain.entity.Restaurante;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestauranteRepository extends JpaRepository<Restaurante, Long> {
    java.util.Optional<Restaurante> findFirstByActivoTrue();
}
