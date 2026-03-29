package org.delivery.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "domiciliarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Domiciliario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(unique = true, length = 20)
    private String cedula;

    @Column(columnDefinition = "TEXT")
    private String foto;

    @Column(nullable = false, length = 20)
    private String telefono;

    @Builder.Default
    @Column(nullable = false)
    private boolean disponible = true;

    private Double lat;
    private Double lng;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurante_id", nullable = false)
    private Restaurante restaurante;
}
