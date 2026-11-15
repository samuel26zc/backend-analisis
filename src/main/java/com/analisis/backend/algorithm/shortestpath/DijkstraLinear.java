package com.analisis.backend.algorithm.shortestpath;

import com.analisis.backend.model.domain.DijkstraResult;
import com.analisis.backend.model.domain.RoadEdge;
import com.analisis.backend.model.domain.RoadNetwork;
import com.analisis.backend.model.enums.DijkstraVersion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementación propia de Dijkstra con búsqueda lineal del nodo mínimo en O(V^2) sobre arreglo.
 * Se detiene inmediatamente al seleccionar el nodo destino.
 */
public class DijkstraLinear {

    private final RoadNetwork network;

    public DijkstraLinear(RoadNetwork network) {
        this.network = network;
    }

    public DijkstraResult findShortestPath(long originId, long destinationId) {
        long startTime = System.nanoTime();

        int n = network.getNodeCount();
        long[] nodeIds = new long[n];
        Map<Long, Integer> idToIndex = new HashMap<>(n * 2);

        int idx = 0;
        for (Long nodeId : network.getNodes().keySet()) {
            nodeIds[idx] = nodeId;
            idToIndex.put(nodeId, idx);
            idx++;
        }

        double[] dist = new double[n];
        double[] distMeters = new double[n];
        int[] prev = new int[n];
        boolean[] visited = new boolean[n];

        Arrays.fill(dist, Double.POSITIVE_INFINITY);
        Arrays.fill(distMeters, Double.POSITIVE_INFINITY);
        Arrays.fill(prev, -1);

        Integer originIdxObj = idToIndex.get(originId);
        Integer destIdxObj = idToIndex.get(destinationId);

        int exploredNodesCount = 0;
        boolean reached = false;
        int destIdx = (destIdxObj != null) ? destIdxObj : -1;

        if (originIdxObj != null && destIdxObj != null) {
            int originIdx = originIdxObj;
            dist[originIdx] = 0.0;
            distMeters[originIdx] = 0.0;

            for (int step = 0; step < n; step++) {
                // Búsqueda lineal O(V) en el arreglo de distancias tentativas
                int uIdx = -1;
                double minDist = Double.POSITIVE_INFINITY;

                for (int i = 0; i < n; i++) {
                    if (!visited[i] && dist[i] < minDist) {
                        minDist = dist[i];
                        uIdx = i;
                    }
                }

                if (uIdx == -1 || minDist == Double.POSITIVE_INFINITY) {
                    break;
                }

                visited[uIdx] = true;
                exploredNodesCount++;

                // Parada inmediata al extraer el nodo destino
                if (uIdx == destIdx) {
                    reached = true;
                    break;
                }

                long uId = nodeIds[uIdx];
                for (RoadEdge edge : network.getNeighbors(uId)) {
                    Integer vIdxObj = idToIndex.get(edge.target());
                    if (vIdxObj != null) {
                        int vIdx = vIdxObj;
                        if (!visited[vIdx]) {
                            double newDist = minDist + edge.travelTimeSeconds();
                            if (newDist < dist[vIdx]) {
                                dist[vIdx] = newDist;
                                distMeters[vIdx] = distMeters[uIdx] + edge.distanceMeters();
                                prev[vIdx] = uIdx;
                            }
                        }
                    }
                }
            }
        }

        long elapsedNanos = System.nanoTime() - startTime;
        double executionTimeMs = elapsedNanos / 1_000_000.0;

        List<Long> path = new ArrayList<>();
        double totalTime = reached ? dist[destIdx] : -1.0;
        double totalDistMeters = reached ? distMeters[destIdx] : -1.0;

        if (reached) {
            int curr = destIdx;
            while (curr != -1) {
                path.add(nodeIds[curr]);
                curr = prev[curr];
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
