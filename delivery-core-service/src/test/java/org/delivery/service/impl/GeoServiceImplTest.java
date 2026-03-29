package org.delivery.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("GeoServiceImpl - Haversine")
class GeoServiceImplTest {

    private final GeoServiceImpl geoService = new GeoServiceImpl();

    @Test
    @DisplayName("Distancia entre mismo punto es 0")
    void shouldReturnZeroForSamePoint() {
        double distancia = geoService.calcularDistanciaKm(4.6097, -74.0817, 4.6097, -74.0817);
        assertEquals(0.0, distancia, 0.001);
    }

    @Test
    @DisplayName("Calcula distancia entre Bogotá y Medellín (~250km)")
    void shouldCalculateDistanceBetweenCities() {
        // Bogotá: 4.6097, -74.0817 | Medellín: 6.2442, -75.5812
        double distancia = geoService.calcularDistanciaKm(4.6097, -74.0817, 6.2442, -75.5812);
        assertTrue(distancia > 200 && distancia < 300,
                "Distancia esperada ~250km, obtenida: " + distancia);
    }

    @Test
    @DisplayName("Distancia corta entre puntos cercanos")
    void shouldCalculateShortDistance() {
        // Dos puntos a ~1km de distancia
        double distancia = geoService.calcularDistanciaKm(4.6097, -74.0817, 4.6187, -74.0817);
        assertTrue(distancia > 0.5 && distancia < 2.0,
                "Distancia esperada ~1km, obtenida: " + distancia);
    }
}
