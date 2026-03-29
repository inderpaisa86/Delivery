package org.delivery.domain.entity;

import java.time.LocalDateTime;

import org.delivery.domain.enums.EstadoAsignacion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * Asignación de un domiciliario a un pedido.
 */
@Entity
@Table(name = "asignaciones_domicilio")
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoAsignacion estado = EstadoAsignacion.ASIGNADO;

    @Column(nullable = false)
    private LocalDateTime fecha = LocalDateTime.now();

    public AsignacionDomicilio() {}

    public AsignacionDomicilio(Pedido pedido, Domiciliario domiciliario) {
        this.pedido = pedido;
        this.domiciliario = domiciliario;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Pedido getPedido() { return pedido; }
    public void setPedido(Pedido pedido) { this.pedido = pedido; }
    public Domiciliario getDomiciliario() { return domiciliario; }
    public void setDomiciliario(Domiciliario domiciliario) { this.domiciliario = domiciliario; }
    public EstadoAsignacion getEstado() { return estado; }
    public void setEstado(EstadoAsignacion estado) { this.estado = estado; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
}
