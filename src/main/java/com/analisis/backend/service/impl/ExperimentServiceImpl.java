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
import java.lang.management.ManagementFactory;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
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
        Runtime runtime = Runtime.getRuntime();
        long maxMemMb = runtime.maxMemory() / (1024 * 1024);
        int processors = runtime.availableProcessors();

        long physicalRamMb = 16384L;
        try {
            var osBean = ManagementFactory.getOperatingSystemMXBean();
            if (osBean instanceof com.sun.management.OperatingSystemMXBean sunBean) {
                physicalRamMb = sunBean.getTotalMemorySize() / (1024 * 1024);
            }
        } catch (Throwable ignored) {}

        return SystemInfoResponseDto.builder()
                .processor("Multi-Core x86_64 (" + processors + " hilos lógicos)")
                .availableProcessors(processors)
                .totalMemoryRam(String.format(Locale.US, "%.1f GB RAM física", physicalRamMb / 1024.0))
                .maxMemoryJvm(maxMemMb + " MB asignados a JVM")
                .operatingSystem(System.getProperty("os.name"))
                .osVersion(System.getProperty("os.version"))
                .osArch(System.getProperty("os.arch"))
                .javaVersion(System.getProperty("java.version"))
                .javaVendor(System.getProperty("java.vendor"))
                .experimentProtocol("Reglas sección 6: Misma máquina, 5 repeticiones reportando la mediana (1 repetición si > 1 min), semilla fija=42, límite de corte de 5 minutos, resultados generados desde archivos CSV/JSON.")
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
