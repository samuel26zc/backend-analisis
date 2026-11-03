package com.analisis.backend.algorithm.scc;

import com.analisis.backend.model.domain.RoadEdge;
import com.analisis.backend.model.domain.RoadNetwork;
import com.analisis.backend.model.domain.RoadNode;
import com.analisis.backend.model.domain.SccResult;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implementación propia desde cero del Algoritmo de Tarjan para Componentes Fuertemente Conexas (SCC).
 * Complejidad: Theta(V + E) en tiempo, O(V) en memoria auxiliar.
 * Utiliza un recorrido DFS iterativo con pila para evitar StackOverflow en grafos reales de gran escala.
 */
public class TarjanScc {

    private final RoadNetwork network;
    private int currentIndex = 0;
    private final Map<Long, Integer> indices = new HashMap<>();
    private final Map<Long, Integer> lowLinks = new HashMap<>();
    private final Deque<Long> stack = new ArrayDeque<>();
    private final Set<Long> onStack = new HashSet<>();
    private final List<List<Long>> components = new ArrayList<>();

    public TarjanScc(RoadNetwork network) {
        this.network = network;
    }

    /**
     * Ejecuta el algoritmo de Tarjan y retorna las componentes fuertemente conexas.
     */
    public List<List<Long>> computeComponents() {
        indices.clear();
        lowLinks.clear();
        stack.clear();
        onStack.clear();
        components.clear();
        currentIndex = 0;

        for (Long nodeId : network.getNodes().keySet()) {
            if (!indices.containsKey(nodeId)) {
                strongConnect(nodeId);
            }
        }

        return components;
    }

    private void strongConnect(long rootNodeId) {
        // Marco de ejecución para DFS iterativo
        record Frame(long u, int edgeIndex) {}
        Deque<Frame> callStack = new ArrayDeque<>();

        // Inicializar raíz
        indices.put(rootNodeId, currentIndex);
        lowLinks.put(rootNodeId, currentIndex);
        currentIndex++;
        stack.push(rootNodeId);
        onStack.add(rootNodeId);
        callStack.push(new Frame(rootNodeId, 0));

        while (!callStack.isEmpty()) {
            Frame top = callStack.pop();
            long u = top.u();
            int edgeIdx = top.edgeIndex();
            List<RoadEdge> edges = network.getNeighbors(u);

            if (edgeIdx < edges.size()) {
                // Hay vecinos pendientes por procesar
                RoadEdge edge = edges.get(edgeIdx);
                long v = edge.target();

                // Re-apilar el marco actual incrementando el puntero de arista
                callStack.push(new Frame(u, edgeIdx + 1));

                if (!indices.containsKey(v)) {
                    // Vecino no visitado: descender en el árbol DFS
                    indices.put(v, currentIndex);
                    lowLinks.put(v, currentIndex);
                    currentIndex++;
                    stack.push(v);
                    onStack.add(v);
                    callStack.push(new Frame(v, 0));
                } else if (onStack.contains(v)) {
                    // Arco de retroceso o cruzado en la misma componente
                    int currentLow = lowLinks.get(u);
                    int vIndex = indices.get(v);
                    lowLinks.put(u, Math.min(currentLow, vIndex));
                }
            } else {
                // Todos los vecinos de 'u' han sido explorados (post-order)
                if (!callStack.isEmpty()) {
                    long parent = callStack.peek().u();
                    int parentLow = lowLinks.get(parent);
                    int uLow = lowLinks.get(u);
                    lowLinks.put(parent, Math.min(parentLow, uLow));
                }

                // Si 'u' es la raíz de una componente fuertemente conexa
                if (lowLinks.get(u).equals(indices.get(u))) {
                    List<Long> component = new ArrayList<>();
                    long w;
                    do {
                        w = stack.pop();
                        onStack.remove(w);
                        component.add(w);
                    } while (w != u);
                    components.add(component);
                }
            }
        }
    }

    /**
     * Extrae la mayor componente fuertemente conexa y construye el resultado formateado según API Map.
     */
    public SccResult extractLargestComponent() {
        long startTime = System.nanoTime();
        List<List<Long>> allComponents = computeComponents();
        long elapsedNanos = System.nanoTime() - startTime;
        double executionTimeMs = elapsedNanos / 1_000_000.0;

        List<Long> largestCompNodes = allComponents.stream()
                .max((a, b) -> Integer.compare(a.size(), b.size()))
                .orElse(Collections.emptyList());

        Set<Long> sccNodeSet = new HashSet<>(largestCompNodes);

        // Construir la subred filtrada
        Map<Long, RoadNode> filteredNodes = new HashMap<>();
        Map<Long, List<RoadEdge>> filteredAdjacency = new HashMap<>();

        for (Long nodeId : sccNodeSet) {
            RoadNode node = network.getNodes().get(nodeId);
            if (node != null) {
                filteredNodes.put(nodeId, node);
            }
            List<RoadEdge> edges = network.getNeighbors(nodeId);
            List<RoadEdge> sccEdges = new ArrayList<>();
            for (RoadEdge edge : edges) {
                if (sccNodeSet.contains(edge.target())) {
                    sccEdges.add(edge);
                }
            }
            filteredAdjacency.put(nodeId, sccEdges);
        }

        RoadNetwork sccNetwork = new RoadNetwork(filteredNodes, filteredAdjacency);

        int totalNodesBefore = network.getNodeCount();
        int totalEdgesBefore = network.getEdgeCount();
        int sccNodesCount = sccNetwork.getNodeCount();
        int sccEdgesCount = sccNetwork.getEdgeCount();

        double retentionNodes = (totalNodesBefore > 0) ? ((double) sccNodesCount / totalNodesBefore * 100.0) : 100.0;
        double retentionEdges = (totalEdgesBefore > 0) ? ((double) sccEdgesCount / totalEdgesBefore * 100.0) : 100.0;

        String algorithmName = "Algoritmo de Tarjan para Componentes Fuertemente Conexas (DFS con pila)";
        String complexity = "O(V + E) en tiempo, O(V) en memoria auxiliar";
        String justification = "Se trabaja exclusivamente sobre la mayor componente fuertemente conexa para garantizar que, " +
                "respetando el sentido de circulación de las vías, exista siempre un camino factible de ida y vuelta " +
                "entre cualquier par de lugares del catálogo, evitando callejones sin salida o nodos sumidero unidireccionales " +
                "que impedirían resolver el ciclo del problema del viajante.";

        return new SccResult(
                totalNodesBefore,
                totalEdgesBefore,
                sccNetwork,
                sccNodesCount,
                sccEdgesCount,
                retentionNodes,
                retentionEdges,
                executionTimeMs,
                allComponents.size(),
                algorithmName,
                complexity,
                justification
        );
    }
}
