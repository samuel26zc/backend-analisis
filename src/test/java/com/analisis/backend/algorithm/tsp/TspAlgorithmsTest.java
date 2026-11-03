package com.analisis.backend.algorithm.tsp;

import com.analisis.backend.model.domain.NetworkDataHolder;
import com.analisis.backend.model.domain.TspSolution;
import com.analisis.backend.repository.PlaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class TspAlgorithmsTest {

    @Autowired
    private NetworkDataHolder dataHolder;

    @Autowired
    private PlaceRepository placeRepository;

    private List<String> testTargets;

    @BeforeEach
    void setUp() {
        // Seleccionar 5 lugares representativos del Quindío
        testTargets = List.of("PARQ_CAFE", "CAL_CAB", "CIR_CAB", "TEB_CAB", "MON_CAB");
    }

    @Test
    @DisplayName("Fuerza Bruta y Held-Karp encuentran idéntico costo óptimo para k=5")
    void testExactTspEquivalence() {
        BruteForceTsp bruteForce = new BruteForceTsp(dataHolder);
        HeldKarpTsp heldKarp = new HeldKarpTsp(dataHolder);

        TspSolution solBf = bruteForce.solve("ARM_CAB", testTargets);
        TspSolution solHk = heldKarp.solve("ARM_CAB", testTargets);

        assertEquals(solBf.totalTravelTimeSeconds(), solHk.totalTravelTimeSeconds(), 1e-3,
                "Fuerza Bruta y Held-Karp deben coincidir exactamente en el costo óptimo");

        assertEquals(testTargets.size(), solHk.visitedPlaceIds().size());
        assertEquals(testTargets.size() + 2, solHk.fullTourPlaceIds().size());
        assertEquals("ARM_CAB", solHk.fullTourPlaceIds().get(0));
        assertEquals("ARM_CAB", solHk.fullTourPlaceIds().get(solHk.fullTourPlaceIds().size() - 1));
        assertFalse(solBf.timeoutExceeded());
    }

    @Test
    @DisplayName("Heurística Voraz construye un ciclo completo válido")
    void testGreedyTsp() {
        GreedyTsp greedy = new GreedyTsp(dataHolder);
        TspSolution sol = greedy.solve("ARM_CAB", testTargets);

        assertTrue(sol.totalTravelTimeSeconds() > 0.0);
        assertEquals(testTargets.size(), sol.visitedPlaceIds().size());
        assertEquals("ARM_CAB", sol.fullTourPlaceIds().get(0));
        assertEquals("ARM_CAB", sol.fullTourPlaceIds().get(sol.fullTourPlaceIds().size() - 1));
    }

    @Test
    @DisplayName("Planificador de Itinerario de un día poda atractivos que exceden 11h")
    void testDayItineraryPruning() {
        DayItineraryPlanner planner = new DayItineraryPlanner(dataHolder, placeRepository);

        // Incluimos parques extensos: Parque del Café (360 min) + PANACA (300 min) + Valle Cocora (240 min)
        // La suma de visitas (900 min = 15h) supera por mucho las 11h (660 min)
        List<String> largeList = List.of("PARQ_CAFE", "PANACA", "VALLE_COCORA", "JARDIN_BOTANICO", "RECUCA", "MIRADOR_FILANDIA", "PARQ_ARRIEROS");

        DayItineraryPlanner.ItineraryResult result = planner.planItinerary("ARM_CAB", largeList);

        assertTrue(result.isFitsIn11Hours(), "El itinerario final debe ser factible y caber en <= 11 horas");
        assertTrue(result.getTotalDurationMinutes() <= 660.0);
        assertFalse(result.getPrunedAttractions().isEmpty(), "Debe haber podado al menos un atractivo");
        assertFalse(result.getPruningJustifications().isEmpty(), "Debe incluir justificaciones formales de poda");
        assertFalse(result.getStops().isEmpty());
    }
}
