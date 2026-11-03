package com.analisis.backend;

import com.analisis.backend.dto.request.ShortestPathRequestDto;
import com.analisis.backend.model.enums.DijkstraVersion;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PlaceAndNetworkIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /api/v1/places retorna los 42 lugares del catálogo")
    void testGetPlaces() throws Exception {
        mockMvc.perform(get("/api/v1/places"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(42)))
                .andExpect(jsonPath("$.data[0].id").isNotEmpty())
                .andExpect(jsonPath("$.data[0].nearestNodeId").isNumber());
    }

    @Test
    @DisplayName("GET /api/v1/places/mapping reporta lugares y casos > 500m")
    void testGetPlacesMapping() throws Exception {
        mockMvc.perform(get("/api/v1/places/mapping"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalPlaces").value(42))
                .andExpect(jsonPath("$.data.exceeding500mCount").value(greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.data.generalTreatmentStrategy").isNotEmpty());
    }

    @Test
    @DisplayName("GET /api/v1/network/stats y /memory-estimation")
    void testNetworkStatsAndMemory() throws Exception {
        mockMvc.perform(get("/api/v1/network/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalNodes").value(greaterThan(0)))
                .andExpect(jsonPath("$.data.sccNodes").value(greaterThan(0)));

        mockMvc.perform(get("/api/v1/network/memory-estimation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.matrixMegabytes").isNumber())
                .andExpect(jsonPath("$.data.listMegabytes").isNumber())
                .andExpect(jsonPath("$.data.ratioMatrixToList").value(greaterThan(1.0)));
    }

    @Test
    @DisplayName("POST /api/v1/routes/shortest-path calcula ruta entre Armenia y Parque del Café")
    void testShortestPath() throws Exception {
        ShortestPathRequestDto request = ShortestPathRequestDto.builder()
                .originPlaceId("ARM_CAB")
                .destinationPlaceId("PARQ_CAFE")
                .version(DijkstraVersion.COLA_PRIORIDAD)
                .build();

        mockMvc.perform(post("/api/v1/routes/shortest-path")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.travelTimeSeconds").value(greaterThan(0.0)))
                .andExpect(jsonPath("$.data.nodePath", not(empty())))
                .andExpect(jsonPath("$.data.polyline", not(empty())));
    }

    @Test
    @DisplayName("GET /api/v1/routes/time-matrix retorna matriz 42x42")
    void testTimeMatrix() throws Exception {
        mockMvc.perform(get("/api/v1/routes/time-matrix"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.size").value(42))
                .andExpect(jsonPath("$.data.matrixSeconds.ARM_CAB.CAL_CAB").value(greaterThan(0.0)));
    }
}
