package org.delivery.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "restaurantes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Restaurante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 20)
    private String telefono;

    @Column(length = 255)
    private String direccion;

    private Double lat;
    private Double lng;

    @Builder.Default
    @Column(nullable = false)
    private boolean activo = true;
}
