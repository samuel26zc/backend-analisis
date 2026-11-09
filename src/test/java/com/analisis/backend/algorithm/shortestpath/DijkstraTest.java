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
    @DisplayName("NetworkDataHolder almacena y consulta correctamente matrices de tiempo y distancia calibradas")
    void testNetworkDataHolderCalibratedMatrices() {
        com.analisis.backend.model.domain.NetworkDataHolder holder = new com.analisis.backend.model.domain.NetworkDataHolder();
        
        java.util.Map<String, Double> armeniaTimes = new java.util.HashMap<>();
        armeniaTimes.put("ARM_CAB", 0.0);
        armeniaTimes.put("CAL_CAB", 441.7);
        armeniaTimes.put("SAL_CAB", 1879.9);

        java.util.Map<String, Double> armeniaDists = new java.util.HashMap<>();
        armeniaDists.put("ARM_CAB", 0.0);
        armeniaDists.put("CAL_CAB", 5177.7);
        armeniaDists.put("SAL_CAB", 23729.5);

        holder.getTimeMatrixSeconds().put("ARM_CAB", armeniaTimes);
        holder.getDistanceMatrixMeters().put("ARM_CAB", armeniaDists);

        // Origen igual a destino
        assertEquals(0.0, holder.getTravelTimeBetweenPlaces("ARM_CAB", "ARM_CAB"));
        assertEquals(0.0, holder.getDistanceBetweenPlaces("ARM_CAB", "ARM_CAB"));

        // Calarcá: 5.17 km (~441 s)
        assertEquals(441.7, holder.getTravelTimeBetweenPlaces("ARM_CAB", "CAL_CAB"), 0.1);
        assertEquals(5177.7, holder.getDistanceBetweenPlaces("ARM_CAB", "CAL_CAB"), 0.1);

        // Salento: 23.73 km (~1880 s)
        assertEquals(1879.9, holder.getTravelTimeBetweenPlaces("ARM_CAB", "SAL_CAB"), 0.1);
        assertEquals(23729.5, holder.getDistanceBetweenPlaces("ARM_CAB", "SAL_CAB"), 0.1);

        // Destino desconocido retorna infinito
        assertTrue(Double.isInfinite(holder.getTravelTimeBetweenPlaces("ARM_CAB", "UNKNOWN")));
        assertTrue(Double.isInfinite(holder.getDistanceBetweenPlaces("ARM_CAB", "UNKNOWN")));
    }
}
