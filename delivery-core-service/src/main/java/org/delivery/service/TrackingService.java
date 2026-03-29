package org.delivery.service;

import org.delivery.dto.TrackingResponse;
import org.delivery.dto.UbicacionRequest;
import org.delivery.dto.UbicacionResponse;

/**
 * Servicio de tracking en tiempo real.
 */
public interface TrackingService {

    void actualizarUbicacion(UbicacionRequest request);

    UbicacionResponse obtenerUbicacion(Long domiciliarioId);

    TrackingResponse obtenerTracking(String token);
}
