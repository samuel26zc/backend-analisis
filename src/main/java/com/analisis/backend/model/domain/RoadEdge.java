package com.analisis.backend.model.domain;

/**
 * Arista dirigida de la red vial con pesos de distancia (m) y tiempo de viaje estimado (s).
 */
public record RoadEdge(
        long source,
        long target,
        double distanceMeters,
        double speedKmh,
        double travelTimeSeconds,
        String roadType,
        boolean oneway
) {
    public static RoadEdge of(long source, long target, double distanceMeters, double speedKmh, String roadType, boolean oneway) {
        double effectiveSpeed = (speedKmh > 0.0) ? speedKmh : imputeSpeed(roadType);
        double speedMps = (effectiveSpeed * 1000.0) / 3600.0;
        double travelTimeSeconds = distanceMeters / speedMps;
        return new RoadEdge(source, target, distanceMeters, effectiveSpeed, travelTimeSeconds, roadType, oneway);
    }

    /**
     * Imputa velocidad estándar por defecto según el tipo de vía de OpenStreetMap si no tiene etiqueta maxspeed.
     */
    public static double imputeSpeed(String roadType) {
        if (roadType == null) return 40.0;
        return switch (roadType.toLowerCase()) {
            case "motorway", "trunk" -> 80.0;
            case "primary" -> 60.0;
            case "secondary" -> 50.0;
            case "tertiary" -> 40.0;
            case "residential", "living_street", "unclassified" -> 30.0;
            default -> 35.0;
        };
    }
}
