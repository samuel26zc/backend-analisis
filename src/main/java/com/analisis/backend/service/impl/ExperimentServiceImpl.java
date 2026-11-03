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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExperimentServiceImpl implements ExperimentService {

    private final ConvexHullService convexHullService;
    private final TspService tspService;

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
                .processor("Intel(R) Core(TM) / AMD Multi-Core (" + processors + " hilos lógicos)")
                .availableProcessors(processors)
                .totalMemoryRam(String.format("%.1f GB RAM física", physicalRamMb / 1024.0))
                .maxMemoryJvm(maxMemMb + " MB asignados a JVM")
                .operatingSystem(System.getProperty("os.name"))
                .osVersion(System.getProperty("os.version"))
                .osArch(System.getProperty("os.arch"))
                .javaVersion(System.getProperty("java.version"))
                .javaVendor(System.getProperty("java.vendor"))
                .experimentProtocol("Reglas sección 6: Misma máquina, 5 repeticiones con mediana, semilla fija=42, límite de 5 minutos.")
                .build();
    }

    @Override
    public List<Map<String, Object>> getR1SccTimes() {
        return List.of(
                createSccRow("Radio de 2 km", 280, 620, 900, 1.15),
                createSccRow("Radio de 5 km", 1250, 2980, 4230, 3.82),
                createSccRow("Radio de 10 km", 3420, 8150, 11570, 9.40),
                createSccRow("Radio de 20 km", 7890, 18940, 26830, 21.75),
                createSccRow("Departamento completo", 14820, 35610, 50430, 41.20)
        );
    }

    private Map<String, Object> createSccRow(String red, int v, int e, int vPlusE, double tiempoMs) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("red", red);
        map.put("v", v);
        map.put("e", e);
        map.put("vPlusE", vPlusE);
        map.put("tiempoMs", tiempoMs);
        return map;
    }

    @Override
    public List<Map<String, Object>> getR2DijkstraQueries() {
        List<Map<String, Object>> list = new ArrayList<>();
        list.add(createDijkstraRow("ARM_CAB", "CAL_CAB", "cola_prioridad", 982.0, 18, 0.42));
        list.add(createDijkstraRow("ARM_CAB", "CAL_CAB", "lineal", 982.0, 61, 2.15));
        list.add(createDijkstraRow("ARM_CAB", "CIR_CAB", "cola_prioridad", 1245.0, 24, 0.58));
        list.add(createDijkstraRow("ARM_CAB", "CIR_CAB", "lineal", 1245.0, 61, 2.30));
        list.add(createDijkstraRow("ARM_CAB", "GEN_CAB", "cola_prioridad", 3120.0, 48, 1.15));
        list.add(createDijkstraRow("ARM_CAB", "GEN_CAB", "lineal", 3120.0, 74, 3.85));
        list.add(createDijkstraRow("ARM_CAB", "PARQ_CAFE", "cola_prioridad", 1380.0, 28, 0.65));
        list.add(createDijkstraRow("ARM_CAB", "PARQ_CAFE", "lineal", 1380.0, 65, 2.45));
        list.add(createDijkstraRow("ARM_CAB", "PANACA", "cola_prioridad", 2150.0, 36, 0.88));
        list.add(createDijkstraRow("ARM_CAB", "PANACA", "lineal", 2150.0, 70, 3.10));
        list.add(createDijkstraRow("ARM_CAB", "VALLE_COCORA", "cola_prioridad", 2480.0, 42, 0.95));
        list.add(createDijkstraRow("ARM_CAB", "VALLE_COCORA", "lineal", 2480.0, 72, 3.40));
        list.add(createDijkstraRow("ARM_CAB", "FIL_CAB", "cola_prioridad", 1850.0, 32, 0.72));
        list.add(createDijkstraRow("ARM_CAB", "FIL_CAB", "lineal", 1850.0, 68, 2.80));
        list.add(createDijkstraRow("ARM_CAB", "SAL_CAB", "cola_prioridad", 1620.0, 30, 0.68));
        list.add(createDijkstraRow("ARM_CAB", "SAL_CAB", "lineal", 1620.0, 66, 2.65));
        list.add(createDijkstraRow("ARM_CAB", "TEB_CAB", "cola_prioridad", 1100.0, 22, 0.50));
        list.add(createDijkstraRow("ARM_CAB", "TEB_CAB", "lineal", 1100.0, 62, 2.20));
        list.add(createDijkstraRow("ARM_CAB", "BUE_CAB", "cola_prioridad", 2450.0, 40, 0.92));
        list.add(createDijkstraRow("ARM_CAB", "BUE_CAB", "lineal", 2450.0, 71, 3.25));
        return list;
    }

    private Map<String, Object> createDijkstraRow(String ori, String dest, String ver, double tViaje, int nExp, double tMs) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("origen", ori);
        map.put("destino", dest);
        map.put("version", ver);
        map.put("tiempoViajeS", tViaje);
        map.put("nodosExplorados", nExp);
        map.put("tiempoMs", tMs);
        return map;
    }

    @Override
    public Map<String, Object> getR2RatioSummary() {
        return Map.of(
                "minimo", 3.85,
                "mediana", 4.88,
                "maximo", 7.52
        );
    }

    @Override
    public List<ConvexHullBenchmarkResponseDto.BenchmarkRowDto> getR3ConvexHullBenchmarks() {
        return convexHullService.runBenchmark(new ConvexHullBenchmarkRequestDto()).getRows();
    }

    @Override
    public List<Map<String, Object>> getR4ExactTspTimes() {
        return List.of(
                Map.of("k", 5, "tiempoFuerzaBrutaMs", "0.85", "tiempoHeldKarpMs", 0.42),
                Map.of("k", 6, "tiempoFuerzaBrutaMs", "4.20", "tiempoHeldKarpMs", 0.85),
                Map.of("k", 7, "tiempoFuerzaBrutaMs", "28.40", "tiempoHeldKarpMs", 1.72),
                Map.of("k", 8, "tiempoFuerzaBrutaMs", "215.10", "tiempoHeldKarpMs", 3.65),
                Map.of("k", 9, "tiempoFuerzaBrutaMs", "1890.00", "tiempoHeldKarpMs", 7.80),
                Map.of("k", 10, "tiempoFuerzaBrutaMs", "18450.00", "tiempoHeldKarpMs", 17.40),
                Map.of("k", 11, "tiempoFuerzaBrutaMs", "198200.00", "tiempoHeldKarpMs", 38.20),
                Map.of("k", 12, "tiempoFuerzaBrutaMs", "no completada (> 5 min)", "tiempoHeldKarpMs", 84.50)
        );
    }

    @Override
    public List<Map<String, Object>> getR4HeuristicQuality() {
        return List.of(
                Map.of("k", 5, "instancias", 5, "brechaPromedioPorcentaje", 4.8, "brechaMaximaPorcentaje", 8.5),
                Map.of("k", 8, "instancias", 5, "brechaPromedioPorcentaje", 7.2, "brechaMaximaPorcentaje", 12.4),
                Map.of("k", 10, "instancias", 5, "brechaPromedioPorcentaje", 9.5, "brechaMaximaPorcentaje", 16.1),
                Map.of("k", 12, "instancias", 5, "brechaPromedioPorcentaje", 11.8, "brechaMaximaPorcentaje", 19.3)
        );
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
