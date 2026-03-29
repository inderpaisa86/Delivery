package org.delivery.infrastructure.persistence.repository;

import org.delivery.domain.entity.Producto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
    List<Producto> findByActivoTrueAndRestauranteId(Long restauranteId);
    Page<Producto> findByRestauranteIdAndActivoTrue(Long restauranteId, Pageable pageable);
    Optional<Producto> findByNombreIgnoreCase(String nombre);
}
