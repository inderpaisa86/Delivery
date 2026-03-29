package org.delivery.infrastructure.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.delivery.domain.entity.Producto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
    List<Producto> findByActivoTrueAndRestauranteId(Long restauranteId);
    Optional<Producto> findByNombreIgnoreCase(String nombre);
}
