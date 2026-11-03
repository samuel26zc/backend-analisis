package com.analisis.backend.controller;

import com.analisis.backend.dto.request.PlaceRequestDto;
import com.analisis.backend.dto.response.ApiResponse;
import com.analisis.backend.dto.response.PlaceMappingResponseDto;
import com.analisis.backend.dto.response.PlaceResponseDto;
import com.analisis.backend.model.enums.PlaceCategory;
import com.analisis.backend.service.PlaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/places")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PlaceController {

    private final PlaceService placeService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PlaceResponseDto>>> getAllPlaces(
            @RequestParam(required = false) PlaceCategory category,
            @RequestParam(required = false) String municipality
    ) {
        List<PlaceResponseDto> places = placeService.getAllPlaces(category, municipality);
        return ResponseEntity.ok(ApiResponse.success(places, "Lugares consultados exitosamente"));
    }

    @GetMapping("/mapping")
    public ResponseEntity<ApiResponse<PlaceMappingResponseDto>> getMappingReport() {
        PlaceMappingResponseDto report = placeService.getMappingReport();
        return ResponseEntity.ok(ApiResponse.success(report, "Reporte de mapeo a la red vial generado exitosamente"));
    }

    @GetMapping("/municipalities")
    public ResponseEntity<ApiResponse<List<String>>> getMunicipalities() {
        return ResponseEntity.ok(ApiResponse.success(placeService.getMunicipalities(), "Operación realizada con éxito"));
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<PlaceCategory>>> getCategories() {
        return ResponseEntity.ok(ApiResponse.success(placeService.getCategories(), "Operación realizada con éxito"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PlaceResponseDto>> getPlaceById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(placeService.getPlaceById(id), "Operación realizada con éxito"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PlaceResponseDto>> createPlace(@Valid @RequestBody PlaceRequestDto requestDto) {
        PlaceResponseDto created = placeService.createPlace(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Lugar creado y asociado al nodo vial exitosamente"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PlaceResponseDto>> updatePlace(
            @PathVariable String id,
            @Valid @RequestBody PlaceRequestDto requestDto
    ) {
        PlaceResponseDto updated = placeService.updatePlace(id, requestDto);
        return ResponseEntity.ok(ApiResponse.success(updated, "Lugar actualizado exitosamente"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePlace(@PathVariable String id) {
        placeService.deletePlace(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Lugar eliminado exitosamente"));
    }
}
