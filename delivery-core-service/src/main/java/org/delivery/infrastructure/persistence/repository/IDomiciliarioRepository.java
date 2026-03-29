package org.delivery.infrastructure.persistence.repository;

import java.util.List;

import org.delivery.domain.entity.Domiciliario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IDomiciliarioRepository extends JpaRepository<Domiciliario, Long> {
    List<Domiciliario> findByDisponibleTrueAndRestauranteIdAndLatIsNotNullAndLngIsNotNull(Long restauranteId);
}
