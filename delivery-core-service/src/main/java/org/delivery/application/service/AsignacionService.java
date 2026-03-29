package org.delivery.application.service;

import java.util.Comparator;
import java.util.List;

import org.delivery.application.port.GeoPort;
import org.delivery.application.port.WhatsAppPort;
import org.delivery.domain.entity.AsignacionDomicilio;
import org.delivery.domain.entity.Domiciliario;
import org.delivery.domain.entity.Pedido;
import org.delivery.domain.enums.EstadoPedido;
import org.delivery.infrastructure.persistence.repository.AsignacionDomicilioRepository;
import org.delivery.infrastructure.persistence.repository.DomiciliarioRepository;
import org.delivery.infrastructure.persistence.repository.PedidoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: asignación automática de domiciliarios.
 * Busca el domiciliario disponible más cercano usando Haversine.
 */
@Service
public class AsignacionService {

    private static final Logger log = LoggerFactory.getLogger(AsignacionService.class);

    private final PedidoRepository pedidoRepository;
    private final DomiciliarioRepository domiciliarioRepository;
    private final AsignacionDomicilioRepository asignacionRepository;
    private final GeoPort geoPort;
    private final WhatsAppPort whatsAppPort;

    public AsignacionService(PedidoRepository pedidoRepository,
                             DomiciliarioRepository domiciliarioRepository,
                             AsignacionDomicilioRepository asignacionRepository,
                             GeoPort geoPort,
                             WhatsAppPort whatsAppPort) {
        this.pedidoRepository = pedidoRepository;
        this.domiciliarioRepository = domiciliarioRepository;
        this.asignacionRepository = asignacionRepository;
        this.geoPort = geoPort;
        this.whatsAppPort = whatsAppPort;
    }

    @Transactional
    public void asignarDomiciliario(Long pedidoId) {
        if (asignacionRepository.existsByPedidoId(pedidoId)) {
            log.warn("Pedido #{} ya tiene domiciliario asignado", pedidoId);
            return;
        }

        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado: " + pedidoId));

        if (pedido.getLat() == null || pedido.getLng() == null) {
            log.warn("Pedido #{} sin coordenadas, no se puede asignar domiciliario", pedidoId);
            return;
        }

        Long restauranteId = pedido.getRestaurante().getId();
        List<Domiciliario> disponibles = domiciliarioRepository
                .findByDisponibleTrueAndRestauranteIdAndLatIsNotNullAndLngIsNotNull(restauranteId);

        if (disponibles.isEmpty()) {
            log.warn("No hay domiciliarios disponibles para pedido #{} (restaurante #{})",
                    pedidoId, restauranteId);
            return;
        }

        Domiciliario masCercano = disponibles.stream()
                .min(Comparator.comparingDouble(d ->
                        geoPort.calcularDistanciaKm(
                                pedido.getLat(), pedido.getLng(),
                                d.getLat(), d.getLng())))
                .orElseThrow();

        AsignacionDomicilio asignacion = new AsignacionDomicilio(pedido, masCercano);
        asignacionRepository.save(asignacion);

        masCercano.setDisponible(false);
        domiciliarioRepository.save(masCercano);

        pedido.setEstado(EstadoPedido.EN_CAMINO);
        pedidoRepository.save(pedido);

        double distancia = geoPort.calcularDistanciaKm(
                pedido.getLat(), pedido.getLng(),
                masCercano.getLat(), masCercano.getLng());

        log.info("Domiciliario {} asignado a pedido #{} (distancia: {} km)",
                masCercano.getNombre(), pedidoId, String.format("%.2f", distancia));

        whatsAppPort.notificarCambioEstado(
                pedido.getCliente().getTelefono(), pedidoId, EstadoPedido.EN_CAMINO);
    }
}
