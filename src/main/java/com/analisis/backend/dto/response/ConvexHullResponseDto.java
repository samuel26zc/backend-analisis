package com.analisis.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConvexHullResponseDto {
    private int inputPointsCount;
    private int hullVerticesCount;
    private List<CoordinateDto> polygonCoordinates;
    private double perimeterMeters;
    private double perimeterKilometers;
    private double areaSquareMeters;
    private double areaSquareKilometers;
    private double executionTimeMs;
    private String algorithmName;
    private String complexity;
}
