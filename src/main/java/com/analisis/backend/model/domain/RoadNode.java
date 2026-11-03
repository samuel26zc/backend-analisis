package com.analisis.backend.model.domain;

/**
 * Nodo de la red vial con identificador único y coordenadas geográficas (WGS84) y métricas (UTM 18N).
 */
public record RoadNode(
        long id,
        double latitude,
        double longitude,
        long utmX,
        long utmY
) {}
