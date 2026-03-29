package org.delivery.infrastructure.external.geo;

import org.delivery.application.port.GeoPort;
import org.springframework.stereotype.Component;

/**
 * Implementación del cálculo de distancia usando la fórmula Haversine.
 */
@Component
public class HaversineAdapter implements GeoPort {

    private static final double RADIO_TIERRA_KM = 6371.0;

    @Override
    public double calcularDistanciaKm(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return RADIO_TIERRA_KM * c;
    }
}
