package org.delivery.service.impl;

import java.util.Comparator;
import java.util.List;

import org.delivery.domain.AsignacionDomicilio;
import org.delivery.domain.Domiciliario;
import org.delivery.domain.Pedido;
import org.delivery.domain.enums.EstadoPedido;
import org.delivery.repository.AsignacionDomicilioRepository;
import org.delivery.repository.DomiciliarioRepository;
import org.delivery.repository.PedidoRepository;
import org.delivery.service.AsignacionService;
import org.delivery.service.GeoService;
import org.delivery.service.WhatsAppNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Asigna automáticamente el domiciliario disponible más cercano al pedido.
 */
@Service
public class AsignacionServiceImpl implements AsignacionService {

    private static final Logger log = LoggerFactory.getLogger(AsignacionServiceImpl.class);

    private final PedidoRepository pedidoRepository;
    private final DomiciliarioRepository domiciliarioRepository;
    private final AsignacionDomicilioRepository asignacionRepository;
    private final GeoService geoService;
    private final WhatsAppNotificationService notificationService;

    public AsignacionServiceImpl(PedidoRepository pedidoRepository,
                                 DomiciliarioRepository domiciliarioRepository,
                                 AsignacionDomicilioRepository asignacionRepository,
                                 GeoService geoService,
                                 WhatsAppNotificationService notificationService) {
        this.pedidoRepository = pedidoRepository;
        this.domiciliarioRepository = domiciliarioRepository;
        this.asignacionRepository = asignacionRepository;
        this.geoService = geoService;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public void asignarDomiciliario(Long pedidoId) {
        // Evitar reasignación duplicada
        if (asignacionRepository.existsByPedidoId(pedidoId)) {
            log.warn("Pedido #{} ya tiene domiciliario asignado", pedidoId);
            return;
        }

        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado: " + pedidoId));

        if (pedido.getLat() == null || pedido.getLng() == null) {
            log.warn("Pedido #{} no tiene coordenadas, no se puede asignar domiciliario", pedidoId);
            return;
        }

        List<Domiciliario> disponibles = domiciliarioRepository
                .findByDisponibleTrueAndLatIsNotNullAndLngIsNotNull();

        if (disponibles.isEmpty()) {
            log.warn("No hay domiciliarios disponibles para pedido #{}", pedidoId);
            return;
        }

        // Seleccionar el más cercano usando Haversine
        Domiciliario masCercano = disponibles.stream()
                .min(Comparator.comparingDouble(d ->
                        geoService.calcularDistanciaKm(
                                pedido.getLat(), pedido.getLng(),
                                d.getLat(), d.getLng())))
                .orElseThrow();

        // Crear asignación
        AsignacionDomicilio asignacion = new AsignacionDomicilio(pedido, masCercano);
        asignacionRepository.save(asignacion);

        // Marcar domiciliario como no disponible
        masCercano.setDisponible(false);
        domiciliarioRepository.save(masCercano);

        // Cambiar estado del pedido
        pedido.setEstado(EstadoPedido.EN_CAMINO);
        pedidoRepository.save(pedido);

        log.info("Domiciliario {} asignado a pedido #{}", masCercano.getNombre(), pedidoId);
        notificationService.notificarCambioEstado(
                pedido.getCliente().getTelefono(), pedidoId, EstadoPedido.EN_CAMINO);
    }
}
