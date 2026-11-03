package com.analisis.backend.controller;

import com.analisis.backend.dto.response.ApiResponse;
import com.analisis.backend.dto.response.ConvexHullBenchmarkResponseDto;
import com.analisis.backend.dto.response.ExperimentAllResponseDto;
import com.analisis.backend.dto.response.SystemInfoResponseDto;
import com.analisis.backend.service.ExperimentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/experiments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ExperimentController {

    private final ExperimentService experimentService;

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<ExperimentAllResponseDto>> getAllExperiments() {
        return ResponseEntity.ok(ApiResponse.success(experimentService.getAllExperiments(), "Resultados experimentales consolidados consultados exitosamente"));
    }

    @GetMapping("/system-info")
    public ResponseEntity<ApiResponse<SystemInfoResponseDto>> getSystemInfo() {
        return ResponseEntity.ok(ApiResponse.success(experimentService.getSystemInfo(), "Ficha técnica del sistema consultada exitosamente"));
    }

    @GetMapping("/r1-scc")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getR1Scc() {
        return ResponseEntity.ok(ApiResponse.success(experimentService.getR1SccTimes(), "Operación realizada con éxito"));
    }

    @GetMapping("/r2-dijkstra")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getR2Dijkstra() {
        return ResponseEntity.ok(ApiResponse.success(experimentService.getR2DijkstraQueries(), "Operación realizada con éxito"));
    }

    @GetMapping("/r2-ratio")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getR2Ratio() {
        return ResponseEntity.ok(ApiResponse.success(experimentService.getR2RatioSummary(), "Operación realizada con éxito"));
    }

    @GetMapping("/r3-convex-hull")
    public ResponseEntity<ApiResponse<List<ConvexHullBenchmarkResponseDto.BenchmarkRowDto>>> getR3ConvexHull() {
        return ResponseEntity.ok(ApiResponse.success(experimentService.getR3ConvexHullBenchmarks(), "Operación realizada con éxito"));
    }

    @GetMapping("/r4-exact-tsp")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getR4ExactTsp() {
        return ResponseEntity.ok(ApiResponse.success(experimentService.getR4ExactTspTimes(), "Operación realizada con éxito"));
    }

    @GetMapping("/r4-heuristic-quality")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getR4HeuristicQuality() {
        return ResponseEntity.ok(ApiResponse.success(experimentService.getR4HeuristicQuality(), "Operación realizada con éxito"));
    }
}
