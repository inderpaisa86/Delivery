package org.delivery.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.delivery.application.port.IGeoPort;
import org.delivery.application.port.IWhatsAppPort;
import org.delivery.domain.entity.AsignacionDomicilio;
import org.delivery.domain.entity.Domiciliario;
import org.delivery.domain.entity.Pedido;
import org.delivery.domain.enums.EstadoAsignacion;
import org.delivery.domain.enums.EstadoPedido;
import org.delivery.infrastructure.persistence.repository.IAsignacionDomicilioRepository;
import org.delivery.infrastructure.persistence.repository.IDomiciliarioRepository;
import org.delivery.infrastructure.persistence.repository.IPedidoRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class AsignacionService {

    private final IPedidoRepository pedidoRepository;
    private final IDomiciliarioRepository domiciliarioRepository;
    private final IAsignacionDomicilioRepository asignacionRepository;
    private final IGeoPort geoPort;
    private final IWhatsAppPort whatsAppPort;

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

        asignacionRepository.save(new AsignacionDomicilio(pedido, masCercano));

        masCercano.setDisponible(false);
        domiciliarioRepository.save(masCercano);

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
