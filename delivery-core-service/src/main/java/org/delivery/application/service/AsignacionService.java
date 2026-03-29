package org.delivery.application.service;

import java.util.Comparator;
import java.util.List;

import org.delivery.application.port.GeoPort;
import org.delivery.application.port.WhatsAppPort;
import org.delivery.domain.entity.AsignacionDomicilio;
import org.delivery.domain.entity.Domiciliario;
import org.delivery.domain.entity.Pedido;
import org.delivery.domain.enums.EstadoAsignacion;
import org.delivery.domain.enums.EstadoPedido;
import org.delivery.infrastructure.persistence.repository.AsignacionDomicilioRepository;
import org.delivery.infrastructure.persistence.repository.DomiciliarioRepository;
import org.delivery.infrastructure.persistence.repository.PedidoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: asignación automática de domiciliarios.
 * Se ejecuta de forma asíncrona para no bloquear el cambio de estado.
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

    /**
     * Busca el domiciliario disponible más cercano al pedido y lo asigna.
     * Se ejecuta de forma asíncrona para no bloquear el flujo principal.
     */
    @Async
    @Transactional
    public void asignarDomiciliario(Long pedidoId) {
        if (asignacionRepository.existsByPedidoId(pedidoId)) {
            log.warn("Pedido #{} ya tiene domiciliario asignado, evitando duplicado", pedidoId);
            return;
        }

        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado: " + pedidoId));

        if (pedido.getLat() == null || pedido.getLng() == null) {
            log.warn("Pedido #{} sin coordenadas, asignación no posible", pedidoId);
            return;
        }

        Long restauranteId = pedido.getRestaurante().getId();
        List<Domiciliario> disponibles = domiciliarioRepository
                .findByDisponibleTrueAndRestauranteIdAndLatIsNotNullAndLngIsNotNull(restauranteId);

        if (disponibles.isEmpty()) {
            log.warn("Sin domiciliarios disponibles para pedido #{} (restaurante #{})",
                    pedidoId, restauranteId);
            return;
        }

        Domiciliario masCercano = disponibles.stream()
                .min(Comparator.comparingDouble(d ->
                        geoPort.calcularDistanciaKm(
                                pedido.getLat(), pedido.getLng(),
                                d.getLat(), d.getLng())))
                .orElseThrow();

        // Crear asignación
        AsignacionDomicilio asignacion = new AsignacionDomicilio(pedido, masCercano);
        asignacionRepository.save(asignacion);

        // Marcar domiciliario como ocupado
        masCercano.setDisponible(false);
        domiciliarioRepository.save(masCercano);

        // Transicionar pedido a EN_CAMINO
        pedido.setEstado(EstadoPedido.EN_CAMINO);
        pedidoRepository.save(pedido);

        double distancia = geoPort.calcularDistanciaKm(
                pedido.getLat(), pedido.getLng(),
                masCercano.getLat(), masCercano.getLng());

        log.info("Domiciliario {} asignado a pedido #{} ({} km)",
                masCercano.getNombre(), pedidoId, String.format("%.2f", distancia));

        whatsAppPort.notificarCambioEstado(
                pedido.getCliente().getTelefono(), pedidoId, EstadoPedido.EN_CAMINO);
    }

    /**
     * Libera al domiciliario asignado a un pedido (cuando se entrega o cancela).
     */
    @Transactional
    public void liberarDomiciliario(Long pedidoId) {
        asignacionRepository.findByPedidoId(pedidoId).ifPresent(asignacion -> {
            Domiciliario domiciliario = asignacion.getDomiciliario();
            domiciliario.setDisponible(true);
            domiciliarioRepository.save(domiciliario);

            asignacion.setEstado(EstadoAsignacion.ENTREGADO);
            asignacionRepository.save(asignacion);

            log.info("Domiciliario {} liberado del pedido #{}",
                    domiciliario.getNombre(), pedidoId);
        });
    }
}
