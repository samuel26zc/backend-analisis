package com.analisis.backend.util;

/**
 * Utilidades geoespaciales para proyecciones cartográficas y cálculo de distancias.
 * Implementa la proyección Transverse Mercator de WGS84 a UTM Zona 18N (EPSG:32618)
 * utilizando enteros de 64 bits para evitar desbordamientos según las pautas del proyecto.
 */
public final class GeoUtils {

    // Constantes del elipsoide WGS84
    private static final double A = 6378137.0; // Semieje mayor en metros
    private static final double F = 1.0 / 298.257223563; // Aplanamiento
    private static final double B = A * (1.0 - F); // Semieje menor
    private static final double E2 = (A * A - B * B) / (A * A); // Primera excentricidad al cuadrado
    private static final double E_PRIME2 = (A * A - B * B) / (B * B); // Segunda excentricidad al cuadrado
    private static final double K0 = 0.9996; // Factor de escala en el meridiano central
    private static final double FALSE_EASTING = 500000.0;
    private static final double FALSE_NORTHING = 0.0;
    private static final double CENTRAL_MERIDIAN_ZONE_18 = -75.0; // Quindío está en Zona 18N (meridiano -75°)

    private GeoUtils() {
        // Constructor privado para clase de utilidades
    }

    /**
     * Registro con coordenadas proyectadas en metros enteros de 64 bits.
     */
    public record UtmPoint(long utmX, long utmY) {}

    /**
     * Convierte coordenadas geográficas WGS84 (latitud, longitud en grados)
     * a coordenadas UTM Zona 18N (EPSG:32618) en metros redondeados a long.
     */
    public static UtmPoint toUtmZone18N(double latitude, double longitude) {
        double latRad = Math.toRadians(latitude);
        double lonRad = Math.toRadians(longitude);
        double lon0Rad = Math.toRadians(CENTRAL_MERIDIAN_ZONE_18);

        double n = A / Math.sqrt(1.0 - E2 * Math.sin(latRad) * Math.sin(latRad));
        double t = Math.tan(latRad) * Math.tan(latRad);
        double c = E_PRIME2 * Math.cos(latRad) * Math.cos(latRad);
        double aDiff = (lonRad - lon0Rad) * Math.cos(latRad);

        // Distancia meridiana M
        double m = A * ((1.0 - E2 / 4.0 - 3.0 * E2 * E2 / 64.0 - 5.0 * E2 * E2 * E2 / 256.0) * latRad
                - (3.0 * E2 / 8.0 + 3.0 * E2 * E2 / 32.0 + 45.0 * E2 * E2 * E2 / 1024.0) * Math.sin(2.0 * latRad)
                + (15.0 * E2 * E2 / 256.0 + 45.0 * E2 * E2 * E2 / 1024.0) * Math.sin(4.0 * latRad)
                - (35.0 * E2 * E2 * E2 / 3072.0) * Math.sin(6.0 * latRad));

        double easting = FALSE_EASTING + K0 * n * (aDiff + (1.0 - t + c) * Math.pow(aDiff, 3) / 6.0
                + (5.0 - 18.0 * t + t * t + 72.0 * c - 58.0 * E_PRIME2) * Math.pow(aDiff, 5) / 120.0);

        double northing = FALSE_NORTHING + K0 * (m + n * Math.tan(latRad) * (aDiff * aDiff / 2.0
                + (5.0 - t + 9.0 * c + 4.0 * c * c) * Math.pow(aDiff, 4) / 24.0
                + (61.0 - 58.0 * t + t * t + 600.0 * c - 330.0 * E_PRIME2) * Math.pow(aDiff, 6) / 720.0));

        return new UtmPoint(Math.round(easting), Math.round(northing));
    }

    /**
     * Calcula la distancia euclidiana en metros en el plano UTM Zona 18N.
     */
    public static double euclideanDistance(long x1, long y1, long x2, long y2) {
        long dx = x2 - x1;
        long dy = y2 - y1;
        return Math.sqrt((double) dx * dx + (double) dy * dy);
    }

    /**
     * Calcula la distancia geodésica mediante la fórmula de Haversine en metros.
     */
    public static double haversineDistanceMeters(double lat1, double lon1, double lat2, double lon2) {
        final double r = 6371000.0; // Radio de la Tierra en metros
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2.0) * Math.sin(dLat / 2.0)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2.0) * Math.sin(dLon / 2.0);
        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
        return r * c;
    }
}
