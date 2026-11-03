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
import java.util.PriorityQueue;
import java.util.Set;

/**
 * Implementación propia de Dijkstra utilizando Cola de Prioridad (Min-Heap) en O((V + E) log V).
 * Se detiene inmediatamente al extraer el nodo destino.
 */
public class DijkstraPriorityQueue {

    private final RoadNetwork network;

    public DijkstraPriorityQueue(RoadNetwork network) {
        this.network = network;
    }

    private record QueueEntry(long nodeId, double timeSeconds) implements Comparable<QueueEntry> {
        @Override
        public int compareTo(QueueEntry other) {
            return Double.compare(this.timeSeconds, other.timeSeconds);
        }
    }

    public DijkstraResult findShortestPath(long originId, long destinationId) {
        long startTime = System.nanoTime();

        Map<Long, Double> dist = new HashMap<>();
        Map<Long, Double> distMeters = new HashMap<>();
        Map<Long, Long> prev = new HashMap<>();
        Set<Long> visited = new HashSet<>();
        PriorityQueue<QueueEntry> pq = new PriorityQueue<>();

        dist.put(originId, 0.0);
        distMeters.put(originId, 0.0);
        pq.add(new QueueEntry(originId, 0.0));

        int exploredNodesCount = 0;
        boolean reached = false;

        while (!pq.isEmpty()) {
            QueueEntry current = pq.poll();
            long u = current.nodeId();

            // Descartar si ya fue explorado con una distancia menor
            if (visited.contains(u)) {
                continue;
            }

            visited.add(u);
            exploredNodesCount++;

            // Parada inmediata al extraer el nodo destino
            if (u == destinationId) {
                reached = true;
                break;
            }

            double dU = current.timeSeconds();

            for (RoadEdge edge : network.getNeighbors(u)) {
                long v = edge.target();
                if (!visited.contains(v)) {
                    double newDist = dU + edge.travelTimeSeconds();
                    double oldDist = dist.getOrDefault(v, Double.POSITIVE_INFINITY);

                    if (newDist < oldDist) {
                        dist.put(v, newDist);
                        distMeters.put(v, distMeters.getOrDefault(u, 0.0) + edge.distanceMeters());
                        prev.put(v, u);
                        pq.add(new QueueEntry(v, newDist));
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
                DijkstraVersion.COLA_PRIORIDAD,
                path,
                invariant,
                reached
        );
    }

    /**
     * Calcula los tiempos mínimos desde un origen a TODOS los nodos alcanzables sin detenerse en un destino particular.
     * Utilizado para calcular filas completas de la matriz de tiempos asimétrica entre los lugares del catálogo.
     */
    public Map<Long, Double> computeOneToAll(long originId) {
        Map<Long, Double> dist = new HashMap<>();
        Set<Long> visited = new HashSet<>();
        PriorityQueue<QueueEntry> pq = new PriorityQueue<>();

        dist.put(originId, 0.0);
        pq.add(new QueueEntry(originId, 0.0));

        while (!pq.isEmpty()) {
            QueueEntry current = pq.poll();
            long u = current.nodeId();

            if (visited.contains(u)) {
                continue;
            }
            visited.add(u);

            double dU = current.timeSeconds();

            for (RoadEdge edge : network.getNeighbors(u)) {
                long v = edge.target();
                if (!visited.contains(v)) {
                    double newDist = dU + edge.travelTimeSeconds();
                    double oldDist = dist.getOrDefault(v, Double.POSITIVE_INFINITY);

                    if (newDist < oldDist) {
                        dist.put(v, newDist);
                        pq.add(new QueueEntry(v, newDist));
                    }
                }
            }
        }

        return dist;
    }
}
