package com.analisis.backend.model.domain;

import com.analisis.backend.util.GeoUtils;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Representación en memoria de la red vial mediante listas de adyacencia optimizadas.
 */
@Getter
public class RoadNetwork {

    private final Map<Long, RoadNode> nodes;
    private final Map<Long, List<RoadEdge>> adjacencyList;

    public RoadNetwork() {
        this.nodes = new HashMap<>();
        this.adjacencyList = new HashMap<>();
    }

    public RoadNetwork(Map<Long, RoadNode> nodes, Map<Long, List<RoadEdge>> adjacencyList) {
        this.nodes = new HashMap<>(nodes);
        this.adjacencyList = new HashMap<>();
        adjacencyList.forEach((k, v) -> this.adjacencyList.put(k, new ArrayList<>(v)));
    }

    public void addNode(RoadNode node) {
        nodes.put(node.id(), node);
        adjacencyList.computeIfAbsent(node.id(), k -> new ArrayList<>());
    }

    public void addEdge(RoadEdge edge) {
        adjacencyList.computeIfAbsent(edge.source(), k -> new ArrayList<>()).add(edge);
        // Asegurar que el nodo destino esté registrado en el mapa de adyacencia
        adjacencyList.computeIfAbsent(edge.target(), k -> new ArrayList<>());
    }

    public List<RoadEdge> getNeighbors(long nodeId) {
        return adjacencyList.getOrDefault(nodeId, Collections.emptyList());
    }

    public int getNodeCount() {
        return nodes.size();
    }

    public int getEdgeCount() {
        return adjacencyList.values().stream().mapToInt(List::size).sum();
    }

    /**
     * Encuentra el nodo más cercano en línea recta calculando distancia euclidiana en UTM 18N.
     */
    public RoadNode findNearestNode(long utmX, long utmY) {
        RoadNode nearest = null;
        double minDistance = Double.MAX_VALUE;

        for (RoadNode node : nodes.values()) {
            double dist = GeoUtils.euclideanDistance(utmX, utmY, node.utmX(), node.utmY());
            if (dist < minDistance) {
                minDistance = dist;
                nearest = node;
            }
        }
        return nearest;
    }

    /**
     * Estimación analítica rigurosa del consumo de memoria en MB entre Matriz de Adyacencia
     * y Listas de Adyacencia bajo supuestos de JVM HotSpot de 64 bits.
     */
    public MemoryEstimation estimateMemoryUsage() {
        int v = getNodeCount();
        int e = getEdgeCount();

        // 1. Matriz de adyacencia: V referencias de array (24 bytes cada fila) + V*V primitivos double (8 bytes c/u)
        double matrixBytes = (24.0 * v) + (8.0 * (double) v * v);
        double matrixMb = matrixBytes / (1024.0 * 1024.0);

        // 2. Listas de adyacencia: Array principal de cabeceras (24 bytes * V)
        // Cada entrada de arista: Objeto RoadEdge (cabecera 16B + 8B target + 8B dist + 8B time + 8B ref) = 48 bytes
        double listBytes = (24.0 * v) + (48.0 * e);
        double listMb = listBytes / (1024.0 * 1024.0);

        double ratio = (listMb > 0.0) ? (matrixMb / listMb) : 0.0;

        String matrixAssumptions = String.format(
                "Matriz V x V con primitivos double (8 bytes por celda para peso de tiempo) más sobrecarga de arrays en JVM de 64 bits (24V + 8V² bytes = %,.0f bytes).",
                matrixBytes
        );

        String listAssumptions = String.format(
                "Listas de adyacencia con array principal de V cabeceras (24V bytes) y E nodos de arista conteniendo id destino, pesos y referencias (24V + 48E = %,.0f bytes).",
                listBytes
        );

        String justification = String.format(
                "La red vial del Quindío es un grafo altamente disperso donde el grado medio de salida es de %.2f aristas por nodo (E ≈ %.1fV). " +
                        "La matriz de adyacencia requeriría %.2f MB con más del 99.9%% de sus celdas desaprovechadas (almacenando infinito o cero). " +
                        "En contraste, las listas de adyacencia requieren únicamente %.2f MB (%.1f veces menos memoria), " +
                        "permiten cargar la red completa en memoria de forma instantánea y habilitan a Dijkstra para iterar exclusivamente sobre los vecinos reales.",
                (v > 0 ? (double) e / v : 0.0),
                (v > 0 ? (double) e / v : 0.0),
                matrixMb,
                listMb,
                ratio
        );

        return new MemoryEstimation(v, e, matrixMb, matrixAssumptions, listMb, listAssumptions, ratio, justification);
    }

    public record MemoryEstimation(
            int nodeCount,
            int edgeCount,
            double matrixMegabytes,
            String matrixAssumptions,
            double listMegabytes,
            String listAssumptions,
            double ratioMatrixToList,
            String justification
    ) {}
}
