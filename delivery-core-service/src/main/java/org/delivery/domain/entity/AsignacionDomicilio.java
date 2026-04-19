package org.delivery.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.delivery.domain.enums.EstadoAsignacion;

import java.time.LocalDateTime;

@Entity
@Table(name = "asignaciones_domicilio")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AsignacionDomicilio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false, unique = true)
    private Pedido pedido;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "domiciliario_id", nullable = false)
    private Domiciliario domiciliario;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoAsignacion estado = EstadoAsignacion.ASIGNADO;

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime fecha = LocalDateTime.now();

    private LocalDateTime fechaEntrega;

    public AsignacionDomicilio(Pedido pedido, Domiciliario domiciliario) {
        this.pedido = pedido;
        this.domiciliario = domiciliario;
        this.estado = EstadoAsignacion.ASIGNADO;
        this.fecha = LocalDateTime.now();
    }
}
