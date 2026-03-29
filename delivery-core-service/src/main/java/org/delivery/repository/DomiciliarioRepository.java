package org.delivery.repository;

import java.util.List;

import org.delivery.domain.Domiciliario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DomiciliarioRepository extends JpaRepository<Domiciliario, Long> {
    List<Domiciliario> findByDisponibleTrueAndLatIsNotNullAndLngIsNotNull();
}
