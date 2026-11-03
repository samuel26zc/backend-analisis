package com.analisis.backend.model.domain;

import com.analisis.backend.model.enums.DijkstraVersion;

import java.util.List;

/**
 * Resultado de la ejecución de una consulta de camino mínimo con Dijkstra.
 */
public record DijkstraResult(
        long originNodeId,
        long destinationNodeId,
        double travelTimeSeconds,
        double distanceMeters,
        int exploredNodesCount,
        double executionTimeMs,
        DijkstraVersion version,
        List<Long> nodePath,
        String invariant,
        boolean reachable
) {}
