package com.analisis.backend.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GeoUtilsTest {

    @Test
    @DisplayName("Conversión de Armenia a UTM Zona 18N")
    void testArmeniaUtmProjection() {
        // Armenia Plaza de Bolívar: 4.53389, -75.68111
        GeoUtils.UtmPoint utm = GeoUtils.toUtmZone18N(4.53389, -75.68111);

        // En zona 18N con meridiano central -75°, Armenia (-75.68111°) está al oeste
        // X ronda los 424,000 - 425,000 metros y el norte (Y) ronda los 500,000 - 505,000 metros
        assertTrue(utm.utmX() > 420000 && utm.utmX() < 430000, "X fuera de rango esperado: " + utm.utmX());
        assertTrue(utm.utmY() > 500000 && utm.utmY() < 505000, "Y fuera de rango esperado: " + utm.utmY());
    }

    @Test
    @DisplayName("Distancia euclidiana en UTM 18N")
    void testEuclideanDistance() {
        double dist = GeoUtils.euclideanDistance(0, 0, 300, 400);
        assertEquals(500.0, dist, 1e-6);
    }

    @Test
    @DisplayName("Distancia Haversine entre Armenia y Calarcá")
    void testHaversineDistance() {
        // Distancia en línea recta entre Armenia (~4.533, -75.681) y Calarcá (~4.529, -75.641) es aprox 4.5 km
        double distMeters = GeoUtils.haversineDistanceMeters(4.53389, -75.68111, 4.52972, -75.64167);
        assertTrue(distMeters > 4000.0 && distMeters < 5000.0, "Distancia calculada: " + distMeters);
    }
}
