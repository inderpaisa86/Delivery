package org.delivery.repository;

import java.util.List;
import java.util.Optional;

import org.delivery.domain.Producto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
    List<Producto> findByActivoTrue();
    Optional<Producto> findByNombreIgnoreCase(String nombre);
}
