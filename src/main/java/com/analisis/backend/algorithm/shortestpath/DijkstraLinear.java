package com.analisis.backend.algorithm.shortestpath;

import com.analisis.backend.model.domain.DijkstraResult;
import com.analisis.backend.model.domain.RoadEdge;
import com.analisis.backend.model.domain.RoadNetwork;
import com.analisis.backend.model.enums.DijkstraVersion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implementación propia de Dijkstra con búsqueda lineal del nodo mínimo en O(V^2).
 * Se detiene inmediatamente al seleccionar el nodo destino.
 */
public class DijkstraLinear {

    private final RoadNetwork network;

    public DijkstraLinear(RoadNetwork network) {
        this.network = network;
    }

    public DijkstraResult findShortestPath(long originId, long destinationId) {
        long startTime = System.nanoTime();

        Map<Long, Double> dist = new HashMap<>();
        Map<Long, Double> distMeters = new HashMap<>();
        Map<Long, Long> prev = new HashMap<>();
        Set<Long> visited = new HashSet<>();

        // Inicializar distancias tentativas a infinito
        for (Long nodeId : network.getNodes().keySet()) {
            dist.put(nodeId, Double.POSITIVE_INFINITY);
            distMeters.put(nodeId, Double.POSITIVE_INFINITY);
        }

        dist.put(originId, 0.0);
        distMeters.put(originId, 0.0);

        int exploredNodesCount = 0;
        boolean reached = false;

        while (true) {
            // Búsqueda lineal O(V) del nodo no visitado con menor distancia tentativa
            Long u = null;
            double minDist = Double.POSITIVE_INFINITY;

            for (Map.Entry<Long, Double> entry : dist.entrySet()) {
                Long nodeId = entry.getKey();
                double d = entry.getValue();
                if (!visited.contains(nodeId) && d < minDist) {
                    minDist = d;
                    u = nodeId;
                }
            }

            if (u == null || minDist == Double.POSITIVE_INFINITY) {
                // No hay más nodos alcanzables
                break;
            }

            visited.add(u);
            exploredNodesCount++;

            // Parada inmediata al extraer el nodo destino
            if (u == destinationId) {
                reached = true;
                break;
            }

            // Relajación de aristas
            for (RoadEdge edge : network.getNeighbors(u)) {
                long v = edge.target();
                if (!visited.contains(v)) {
                    double newDist = dist.get(u) + edge.travelTimeSeconds();
                    if (newDist < dist.get(v)) {
                        dist.put(v, newDist);
                        distMeters.put(v, distMeters.get(u) + edge.distanceMeters());
                        prev.put(v, u);
                    }
                }
            }
        }

        long elapsedNanos = System.nanoTime() - startTime;
        double executionTimeMs = elapsedNanos / 1_000_000.0;

        List<Long> path = new ArrayList<>();
        double totalTime = reached ? dist.get(destinationId) : -1.0;
        double totalDistMeters = reached ? distMeters.get(destinationId) : -1.0;

        if (reached) {
            Long curr = destinationId;
            while (curr != null) {
                path.add(curr);
                curr = prev.get(curr);
            }
            Collections.reverse(path);
        }

        String invariant = "Invariante de Dijkstra: Para todo nodo u en el conjunto de nodos explorados S, dist[u] representa " +
                "la distancia mínima definitiva desde el origen. Permite detenerse al extraer el destino t porque cualquier " +
                "camino alternativo pasaría por la frontera con distancia tentativa >= dist[t], y al ser todos los pesos " +
                "w(u,v) >= 0 (no negatividad de tiempos de viaje), el costo no puede disminuir.";

        return new DijkstraResult(
                originId,
                destinationId,
                totalTime,
                totalDistMeters,
                exploredNodesCount,
                executionTimeMs,
                DijkstraVersion.LINEAL,
                path,
                invariant,
                reached
        );
    }
}
