package org.delivery.infrastructure.external.geo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("HaversineAdapter")
class HaversineAdapterTest {

    private final HaversineAdapter adapter = new HaversineAdapter();

    @Test
    @DisplayName("Distancia entre mismo punto es 0")
    void shouldReturnZeroForSamePoint() {
        assertEquals(0.0, adapter.calcularDistanciaKm(4.6097, -74.0817, 4.6097, -74.0817), 0.001);
    }

    @Test
    @DisplayName("Bogotá - Medellín ~250km")
    void shouldCalculateBogotaMedellin() {
        double distancia = adapter.calcularDistanciaKm(4.6097, -74.0817, 6.2442, -75.5812);
        assertTrue(distancia > 200 && distancia < 300,
                "Esperado ~250km, obtenido: " + distancia);
    }

    @Test
    @DisplayName("Distancia corta entre puntos cercanos")
    void shouldCalculateShortDistance() {
        double distancia = adapter.calcularDistanciaKm(4.6097, -74.0817, 4.6187, -74.0817);
        assertTrue(distancia > 0.5 && distancia < 2.0);
    }
}
