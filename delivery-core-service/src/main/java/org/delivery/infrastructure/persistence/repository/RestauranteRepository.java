package org.delivery.infrastructure.persistence.repository;

import org.delivery.domain.entity.Restaurante;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RestauranteRepository extends JpaRepository<Restaurante, Long> {
    Optional<Restaurante> findFirstByActivoTrue();
    Optional<Restaurante> findByWhatsappPhoneIdAndActivoTrue(String whatsappPhoneId);
    List<Restaurante> findByActivoTrue();
}
