package com.analisis.backend.controller;

import com.analisis.backend.dto.response.ApiResponse;
import com.analisis.backend.dto.response.MemoryEstimationResponseDto;
import com.analisis.backend.dto.response.NetworkStatsResponseDto;
import com.analisis.backend.dto.response.SccResponseDto;
import com.analisis.backend.model.domain.RoadNode;
import com.analisis.backend.service.NetworkService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/network")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class NetworkController {

    private final NetworkService networkService;

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<NetworkStatsResponseDto>> getNetworkStats() {
        return ResponseEntity.ok(ApiResponse.success(networkService.getNetworkStats(), "Operación realizada con éxito"));
    }

    @GetMapping("/memory-estimation")
    public ResponseEntity<ApiResponse<MemoryEstimationResponseDto>> getMemoryEstimation() {
        return ResponseEntity.ok(ApiResponse.success(networkService.getMemoryEstimation(), "Operación realizada con éxito"));
    }

    @GetMapping("/scc")
    public ResponseEntity<ApiResponse<SccResponseDto>> getSccReport() {
        return ResponseEntity.ok(ApiResponse.success(networkService.getSccReport(), "Operación realizada con éxito"));
    }

    @GetMapping("/nodes")
    public ResponseEntity<ApiResponse<List<RoadNode>>> getSccNodes() {
        return ResponseEntity.ok(ApiResponse.success(networkService.getSccNodes(), "Operación realizada con éxito"));
    }
}
