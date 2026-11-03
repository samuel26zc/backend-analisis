package com.analisis.backend.controller;

import com.analisis.backend.dto.request.ConvexHullBenchmarkRequestDto;
import com.analisis.backend.dto.request.ConvexHullComputeRequestDto;
import com.analisis.backend.dto.response.ApiResponse;
import com.analisis.backend.dto.response.ConvexHullBenchmarkResponseDto;
import com.analisis.backend.dto.response.ConvexHullResponseDto;
import com.analisis.backend.service.ConvexHullService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/convex-hull")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ConvexHullController {

    private final ConvexHullService convexHullService;

    @GetMapping("/network")
    public ResponseEntity<ApiResponse<ConvexHullResponseDto>> getNetworkConvexHull() {
        ConvexHullResponseDto response = convexHullService.computeNetworkConvexHull();
        return ResponseEntity.ok(ApiResponse.success(response, "Envolvente convexa calculada exitosamente"));
    }

    @PostMapping("/compute")
    public ResponseEntity<ApiResponse<ConvexHullResponseDto>> computeCustomConvexHull(
            @Valid @RequestBody ConvexHullComputeRequestDto request
    ) {
        ConvexHullResponseDto response = convexHullService.computeCustomConvexHull(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Envolvente convexa calculada exitosamente"));
    }

    @PostMapping("/benchmark")
    public ResponseEntity<ApiResponse<ConvexHullBenchmarkResponseDto>> runBenchmark(
            @RequestBody(required = false) ConvexHullBenchmarkRequestDto request
    ) {
        ConvexHullBenchmarkResponseDto response = convexHullService.runBenchmark(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Benchmark de envolvente convexa ejecutado exitosamente"));
    }
}
