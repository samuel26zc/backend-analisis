package com.analisis.backend.algorithm.shortestpath;

import com.analisis.backend.model.domain.DijkstraResult;
import com.analisis.backend.model.domain.RoadEdge;
import com.analisis.backend.model.domain.RoadNetwork;
import com.analisis.backend.model.domain.RoadNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DijkstraTest {

    private RoadNetwork network;

    @BeforeEach
    void setUp() {
        network = new RoadNetwork();
        // Construir un grafo de prueba en diamante con dos caminos alternativos
        // Nodo 1 -> Nodo 2 (rápido por autopista) -> Nodo 4
        // Nodo 1 -> Nodo 3 (lento por vía residencial) -> Nodo 4
        network.addNode(new RoadNode(1L, 4.50, -75.68, 468000, 501000));
        network.addNode(new RoadNode(2L, 4.53, -75.68, 468000, 504000));
        network.addNode(new RoadNode(3L, 4.51, -75.65, 471000, 502000));
        network.addNode(new RoadNode(4L, 4.55, -75.65, 471000, 506000));

        // Camino por 2: 3000m a 60km/h (180s) + 3000m a 60km/h (180s) = 360s
        network.addEdge(RoadEdge.of(1L, 2L, 3000.0, 60.0, "primary", true));
        network.addEdge(RoadEdge.of(2L, 4L, 3000.0, 60.0, "primary", true));

        // Camino por 3: 2000m a 30km/h (240s) + 2000m a 30km/h (240s) = 480s
        network.addEdge(RoadEdge.of(1L, 3L, 2000.0, 30.0, "residential", true));
        network.addEdge(RoadEdge.of(3L, 4L, 2000.0, 30.0, "residential", true));
    }

    @Test
    @DisplayName("Dijkstra Lineal vs Min-Heap producen idéntico tiempo y ruta mínima")
    void testDijkstraConsistency() {
        DijkstraLinear linear = new DijkstraLinear(network);
        DijkstraPriorityQueue pq = new DijkstraPriorityQueue(network);

        DijkstraResult resLinear = linear.findShortestPath(1L, 4L);
        DijkstraResult resPq = pq.findShortestPath(1L, 4L);

        assertTrue(resLinear.reachable());
        assertTrue(resPq.reachable());

        assertEquals(360.0, resLinear.travelTimeSeconds(), 1e-4, "Tiempo de versión lineal incorrecto");
        assertEquals(360.0, resPq.travelTimeSeconds(), 1e-4, "Tiempo de versión PQ incorrecto");

        assertEquals(List.of(1L, 2L, 4L), resLinear.nodePath(), "Ruta lineal debe ser [1, 2, 4]");
        assertEquals(List.of(1L, 2L, 4L), resPq.nodePath(), "Ruta PQ debe ser [1, 2, 4]");

        assertTrue(resLinear.exploredNodesCount() > 0);
        assertTrue(resPq.exploredNodesCount() > 0);
        assertNotNull(resPq.invariant());
    }

    @Test
    @DisplayName("Red vial de OpenStreetMap (quindio_network.json) se carga y ejecuta Tarjan SCC y Dijkstra correctamente")
    void testOpenStreetMapQuindioNetwork() throws Exception {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        try (java.io.InputStream is = getClass().getClassLoader().getResourceAsStream("data/quindio_network.json")) {
            assertNotNull(is, "El archivo data/quindio_network.json debe existir en resources");
            com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(is);
            RoadNetwork osmNet = new RoadNetwork();

            for (com.fasterxml.jackson.databind.JsonNode n : root.get("nodes")) {
                osmNet.addNode(new RoadNode(
                        n.get("id").asLong(),
                        n.get("latitude").asDouble(),
                        n.get("longitude").asDouble(),
                        n.get("utmX").asLong(),
                        n.get("utmY").asLong()
                ));
            }
            for (com.fasterxml.jackson.databind.JsonNode e : root.get("edges")) {
                osmNet.addEdge(new RoadEdge(
                        e.get("source").asLong(),
                        e.get("target").asLong(),
                        e.get("distanceMeters").asDouble(),
                        e.get("speedKmh").asDouble(),
                        e.get("travelTimeSeconds").asDouble(),
                        e.get("highway").asText(),
                        e.get("oneway").asBoolean()
                ));
            }

            assertTrue(osmNet.getNodeCount() > 10000, "La red OSM del Quindío debe tener miles de nodos reales");
            assertTrue(osmNet.getEdgeCount() > 20000, "La red OSM del Quindío debe tener decenas de miles de aristas");

            com.analisis.backend.algorithm.scc.TarjanScc tarjan = new com.analisis.backend.algorithm.scc.TarjanScc(osmNet);
            com.analisis.backend.model.domain.SccResult scc = tarjan.extractLargestComponent();
            RoadNetwork sccNet = scc.largestSccNetwork();

            assertTrue(scc.sccNodesCount() > 10000, "La mayor SCC de OpenStreetMap debe conservar más de 10,000 nodos");

            // Probar camino mínimo en la SCC de OpenStreetMap entre Armenia (10042792787) y Salento (12984614208)
            long armeniaNode = 10042792787L;
            long salentoNode = 12984614208L;

            DijkstraPriorityQueue pq = new DijkstraPriorityQueue(sccNet);
            DijkstraLinear linear = new DijkstraLinear(sccNet);

            DijkstraResult resPq = pq.findShortestPath(armeniaNode, salentoNode);
            DijkstraResult resLin = linear.findShortestPath(armeniaNode, salentoNode);

            assertTrue(resPq.reachable(), "Debe existir camino vial OSM entre Armenia y Salento");
            assertTrue(resLin.reachable(), "Debe existir camino vial OSM en versión lineal");
            assertEquals(resPq.travelTimeSeconds(), resLin.travelTimeSeconds(), 1e-3,
                    "Dijkstra Lineal y Cola de Prioridad deben coincidir exactamente sobre la red OSM");
            assertTrue(resPq.nodePath().size() > 20, "La ruta sobre la red OSM debe contener decenas de nodos viales");
        }
    }
}
