package com.analisis.backend;

import com.analisis.backend.dto.request.CompareTspRequestDto;
import com.analisis.backend.dto.request.DayItineraryRequestDto;
import com.analisis.backend.dto.request.TspSolveRequestDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PhaseTwoIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /api/v1/convex-hull/network retorna la envolvente de la red")
    void testNetworkConvexHull() throws Exception {
        mockMvc.perform(get("/api/v1/convex-hull/network"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.hullVerticesCount").value(greaterThan(3)))
                .andExpect(jsonPath("$.data.perimeterKilometers").value(greaterThan(0.0)))
                .andExpect(jsonPath("$.data.areaSquareKilometers").value(greaterThan(0.0)));
    }

    @Test
    @DisplayName("POST /api/v1/tsp/solve con Held-Karp")
    void testTspSolve() throws Exception {
        TspSolveRequestDto request = TspSolveRequestDto.builder()
                .originPlaceId("ARM_CAB")
                .targetPlaceIds(List.of("PARQ_CAFE", "CAL_CAB", "CIR_CAB"))
                .algorithm("HELD_KARP")
                .build();

        mockMvc.perform(post("/api/v1/tsp/solve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.k").value(3))
                .andExpect(jsonPath("$.data.fullTourPlaceIds", hasSize(5)))
                .andExpect(jsonPath("$.data.totalTravelTimeSeconds").value(greaterThan(0.0)));
    }

    @Test
    @DisplayName("POST /api/v1/tsp/compare-methods compara FB, Held-Karp y Voraz")
    void testCompareTspMethods() throws Exception {
        CompareTspRequestDto request = CompareTspRequestDto.builder()
                .originPlaceId("ARM_CAB")
                .targetPlaceIds(List.of("PARQ_CAFE", "CAL_CAB", "CIR_CAB", "TEB_CAB"))
                .build();

        mockMvc.perform(post("/api/v1/tsp/compare-methods")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.exactMatches").value(true))
                .andExpect(jsonPath("$.data.gapGreedyPercent").isNumber());
    }

    @Test
    @DisplayName("POST /api/v1/tsp/day-itinerary genera itinerario de 11h")
    void testDayItinerary() throws Exception {
        DayItineraryRequestDto request = DayItineraryRequestDto.builder()
                .originPlaceId("ARM_CAB")
                .attractionIds(List.of("PARQ_CAFE", "PANACA", "VALLE_COCORA", "JARDIN_BOTANICO", "RECUCA", "MIRADOR_FILANDIA"))
                .build();

        mockMvc.perform(post("/api/v1/tsp/day-itinerary")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fitsIn11Hours").value(true))
                .andExpect(jsonPath("$.data.totalDurationHours").value(lessThanOrEqualTo(11.0)))
                .andExpect(jsonPath("$.data.stops", not(empty())));
    }

    @Test
    @DisplayName("GET /api/v1/experiments/all y /system-info")
    void testExperimentsEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/experiments/system-info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.processor").isNotEmpty())
                .andExpect(jsonPath("$.data.operatingSystem").isNotEmpty());

        mockMvc.perform(get("/api/v1/experiments/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.systemInfo").exists())
                .andExpect(jsonPath("$.data.r1SccTimes", not(empty())))
                .andExpect(jsonPath("$.data.r2DijkstraQueries", not(empty())))
                .andExpect(jsonPath("$.data.r3ConvexHullBenchmarks", not(empty())))
                .andExpect(jsonPath("$.data.r4ExactTspTimes", not(empty())))
                .andExpect(jsonPath("$.data.r4HeuristicQuality", not(empty())));
    }
}
