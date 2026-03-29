package org.delivery.application.port;

/**
 * Puerto para cálculos geográficos.
 */
public interface IGeoPort {

    double calcularDistanciaKm(double lat1, double lng1, double lat2, double lng2);
}
