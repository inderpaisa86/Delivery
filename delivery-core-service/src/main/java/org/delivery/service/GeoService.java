package org.delivery.service;

/**
 * Servicio de cálculos geográficos.
 */
public interface GeoService {

    /**
     * Calcula la distancia en kilómetros entre dos puntos usando la fórmula Haversine.
     */
    double calcularDistanciaKm(double lat1, double lng1, double lat2, double lng2);
}
