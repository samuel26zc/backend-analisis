package com.analisis.backend.controller;

import com.analisis.backend.dto.request.CompareDijkstraRequestDto;
import com.analisis.backend.dto.request.ShortestPathRequestDto;
import com.analisis.backend.dto.response.ApiResponse;
import com.analisis.backend.dto.response.CompareDijkstraResponseDto;
import com.analisis.backend.dto.response.ShortestPathResponseDto;
import com.analisis.backend.dto.response.TimeMatrixResponseDto;
import com.analisis.backend.service.RouteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/routes")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RouteController {

    private final RouteService routeService;

    @PostMapping("/shortest-path")
    public ResponseEntity<ApiResponse<ShortestPathResponseDto>> computeShortestPath(
            @Valid @RequestBody ShortestPathRequestDto request
    ) {
        ShortestPathResponseDto response = routeService.computeShortestPath(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Ruta calculada exitosamente"));
    }

    @PostMapping("/compare-dijkstra")
    public ResponseEntity<ApiResponse<CompareDijkstraResponseDto>> compareDijkstra(
            @Valid @RequestBody CompareDijkstraRequestDto request
    ) {
        CompareDijkstraResponseDto response = routeService.compareDijkstraVersions(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Comparación de versiones de Dijkstra completada"));
    }

    @GetMapping("/time-matrix")
    public ResponseEntity<ApiResponse<TimeMatrixResponseDto>> getTimeMatrix() {
        return ResponseEntity.ok(ApiResponse.success(routeService.getTimeMatrix(), "Matriz de tiempos consultada exitosamente"));
    }

    @GetMapping(value = "/time-matrix/csv", produces = "text/csv")
    public ResponseEntity<String> getTimeMatrixCsv() {
        String csv = routeService.getTimeMatrixCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"matriz_tiempos_quindio.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }
}
