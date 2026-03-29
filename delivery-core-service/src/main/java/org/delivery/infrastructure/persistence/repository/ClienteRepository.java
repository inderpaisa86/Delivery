package org.delivery.infrastructure.persistence.repository;

import java.util.Optional;

import org.delivery.domain.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    Optional<Cliente> findByTelefonoAndRestauranteId(String telefono, Long restauranteId);
}
