package org.delivery.infrastructure.persistence.repository;

import org.delivery.domain.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IUsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByUsernameAndActivoTrue(String username);
    List<Usuario> findByRestauranteIdAndActivoTrue(Long restauranteId);
}
