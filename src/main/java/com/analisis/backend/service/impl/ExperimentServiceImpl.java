package com.analisis.backend.service.impl;

import com.analisis.backend.dto.request.ConvexHullBenchmarkRequestDto;
import com.analisis.backend.dto.request.DayItineraryRequestDto;
import com.analisis.backend.dto.response.ConvexHullBenchmarkResponseDto;
import com.analisis.backend.dto.response.DayItineraryResponseDto;
import com.analisis.backend.dto.response.ExperimentAllResponseDto;
import com.analisis.backend.dto.response.SystemInfoResponseDto;
import com.analisis.backend.service.ConvexHullService;
import com.analisis.backend.service.ExperimentService;
import com.analisis.backend.service.TspService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExperimentServiceImpl implements ExperimentService {

    private final ConvexHullService convexHullService;
    private final TspService tspService;
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;

    private JsonNode loadExperimentsJson() {
        try {
            Resource res = resourceLoader.getResource("classpath:data/experiments/experiments_results.json");
            if (res.exists()) {
                try (InputStream is = res.getInputStream()) {
                    return objectMapper.readTree(is);
                }
            }
        } catch (Exception e) {
            log.error("Error leyendo experiments_results.json: {}", e.getMessage());
        }
        return objectMapper.createObjectNode();
    }

    @Override
    public SystemInfoResponseDto getSystemInfo() {
        JsonNode root = loadExperimentsJson();
        JsonNode sysNode = root.get("systemInfo");
        if (sysNode != null && sysNode.isObject()) {
            return objectMapper.convertValue(sysNode, SystemInfoResponseDto.class);
        }
        return SystemInfoResponseDto.builder()
                .processor("12th Gen Intel(R) Core(TM) i5-12500H (16 hilos lógicos)")
                .availableProcessors(16)
                .totalMemoryRam("16.0 GB RAM física")
                .maxMemoryJvm("4032 MB asignados a JVM")
                .operatingSystem("Windows 11")
                .osVersion("10.0.26300")
                .osArch("amd64")
                .javaVersion("Java 21.0.12.1 LTS | Python 3.12.10 (OSMnx 2.1.2, SciPy 1.18.1)")
                .javaVendor("Eclipse Adoptium")
                .experimentProtocol("Reglas sección 6: Misma máquina registrada en experiments_results.json, 5 repeticiones con mediana (1 repetición si > 1 min), semilla fija=42, límite de 5 minutos.")
                .build();
    }

    @Override
    public List<Map<String, Object>> getR1SccTimes() {
        JsonNode root = loadExperimentsJson();
        JsonNode node = root.get("r1SccTimes");
        if (node != null && node.isArray()) {
            return objectMapper.convertValue(node, new TypeReference<>() {});
        }
        return Collections.emptyList();
    }

    @Override
    public List<Map<String, Object>> getR2DijkstraQueries() {
        JsonNode root = loadExperimentsJson();
        JsonNode node = root.get("r2DijkstraQueries");
        if (node != null && node.isArray()) {
            return objectMapper.convertValue(node, new TypeReference<>() {});
        }
        return Collections.emptyList();
    }

    @Override
    public Map<String, Object> getR2RatioSummary() {
        JsonNode root = loadExperimentsJson();
        JsonNode node = root.get("r2RatioSummary");
        if (node != null && node.isObject()) {
            return objectMapper.convertValue(node, new TypeReference<>() {});
        }
        return Collections.emptyMap();
    }

    @Override
    public List<ConvexHullBenchmarkResponseDto.BenchmarkRowDto> getR3ConvexHullBenchmarks() {
        JsonNode root = loadExperimentsJson();
        JsonNode node = root.get("r3ConvexHullBenchmarks");
        if (node != null && node.isArray()) {
            return objectMapper.convertValue(node, new TypeReference<>() {});
        }
        return convexHullService.runBenchmark(new ConvexHullBenchmarkRequestDto()).getRows();
    }

    @Override
    public List<Map<String, Object>> getR4ExactTspTimes() {
        JsonNode root = loadExperimentsJson();
        JsonNode node = root.get("r4ExactTspTimes");
        if (node != null && node.isArray()) {
            return objectMapper.convertValue(node, new TypeReference<>() {});
        }
        return Collections.emptyList();
    }

    @Override
    public List<Map<String, Object>> getR4HeuristicQuality() {
        JsonNode root = loadExperimentsJson();
        JsonNode node = root.get("r4HeuristicQuality");
        if (node != null && node.isArray()) {
            return objectMapper.convertValue(node, new TypeReference<>() {});
        }
        return Collections.emptyList();
    }

    @Override
    public ExperimentAllResponseDto getAllExperiments() {
        DayItineraryResponseDto sampleItin = tspService.planDayItinerary(
                DayItineraryRequestDto.builder()
                        .originPlaceId("ARM_CAB")
                        .attractionIds(List.of("PARQ_CAFE", "PANACA", "VALLE_COCORA", "JARDIN_BOTANICO", "MUSEO_ORO_QUIMBAYA", "RECUCA", "MIRADOR_FILANDIA"))
                        .build()
        );

        return ExperimentAllResponseDto.builder()
                .systemInfo(getSystemInfo())
                .r1SccTimes(getR1SccTimes())
                .r2DijkstraQueries(getR2DijkstraQueries())
                .r2RatioSummary(getR2RatioSummary())
                .r3ConvexHullBenchmarks(getR3ConvexHullBenchmarks())
                .r4ExactTspTimes(getR4ExactTspTimes())
                .r4HeuristicQuality(getR4HeuristicQuality())
                .r4SampleItinerary(sampleItin)
                .build();
    }
}
