package com.analisis.backend.controller;

import com.analisis.backend.dto.request.CompareTspRequestDto;
import com.analisis.backend.dto.request.DayItineraryRequestDto;
import com.analisis.backend.dto.request.TspSolveRequestDto;
import com.analisis.backend.dto.response.ApiResponse;
import com.analisis.backend.dto.response.CompareTspResponseDto;
import com.analisis.backend.dto.response.DayItineraryResponseDto;
import com.analisis.backend.dto.response.TspSolveResponseDto;
import com.analisis.backend.service.TspService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tsp")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TspController {

    private final TspService tspService;

    @PostMapping("/solve")
    public ResponseEntity<ApiResponse<TspSolveResponseDto>> solveTsp(
            @Valid @RequestBody TspSolveRequestDto request
    ) {
        TspSolveResponseDto response = tspService.solveTsp(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Ruta óptima calculada exitosamente"));
    }

    @PostMapping("/compare-methods")
    public ResponseEntity<ApiResponse<CompareTspResponseDto>> compareMethods(
            @Valid @RequestBody CompareTspRequestDto request
    ) {
        CompareTspResponseDto response = tspService.compareMethods(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Comparación de métodos ATSP completada"));
    }

    @PostMapping("/day-itinerary")
    public ResponseEntity<ApiResponse<DayItineraryResponseDto>> planDayItinerary(
            @Valid @RequestBody DayItineraryRequestDto request
    ) {
        DayItineraryResponseDto response = tspService.planDayItinerary(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Itinerario de un día generado exitosamente"));
    }
}
