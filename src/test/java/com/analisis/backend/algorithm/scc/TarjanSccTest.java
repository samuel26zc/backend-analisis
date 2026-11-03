package com.analisis.backend.algorithm.scc;

import com.analisis.backend.model.domain.RoadEdge;
import com.analisis.backend.model.domain.RoadNetwork;
import com.analisis.backend.model.domain.RoadNode;
import com.analisis.backend.model.domain.SccResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TarjanSccTest {

    @Test
    @DisplayName("Tarjan SCC en grafo con ciclo y nodo sumidero")
    void testTarjanWithCycleAndSink() {
        RoadNetwork net = new RoadNetwork();
        // Nodos 1, 2, 3 forman un ciclo fuertemente conexo (1 -> 2 -> 3 -> 1)
        net.addNode(new RoadNode(1L, 4.5, -75.6, 468000, 501000));
        net.addNode(new RoadNode(2L, 4.51, -75.61, 469000, 502000));
        net.addNode(new RoadNode(3L, 4.52, -75.62, 470000, 503000));
        // Nodo 4 es un sumidero aislado (3 -> 4)
        net.addNode(new RoadNode(4L, 4.53, -75.63, 471000, 504000));

        net.addEdge(RoadEdge.of(1L, 2L, 1000.0, 50.0, "primary", true));
        net.addEdge(RoadEdge.of(2L, 3L, 1000.0, 50.0, "primary", true));
        net.addEdge(RoadEdge.of(3L, 1L, 1000.0, 50.0, "primary", true));
        net.addEdge(RoadEdge.of(3L, 4L, 500.0, 30.0, "residential", true));

        TarjanScc tarjan = new TarjanScc(net);
        List<List<Long>> components = tarjan.computeComponents();

        assertEquals(2, components.size(), "Debe haber 2 componentes: {4} y {1, 2, 3}");

        SccResult result = tarjan.extractLargestComponent();
        assertEquals(3, result.sccNodesCount(), "La mayor componente debe tener 3 nodos");
        assertEquals(3, result.sccEdgesCount(), "La mayor componente debe tener 3 aristas (el ciclo)");
        assertTrue(result.largestSccNetwork().getNodes().containsKey(1L));
        assertTrue(result.largestSccNetwork().getNodes().containsKey(2L));
        assertTrue(result.largestSccNetwork().getNodes().containsKey(3L));
        assertFalse(result.largestSccNetwork().getNodes().containsKey(4L), "El sumidero debe quedar excluido");
    }
}
