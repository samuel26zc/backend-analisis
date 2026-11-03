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
public class TspSolveResponseDto {
    private String originPlaceId;
    private String originPlaceName;
    private int k;
    private List<String> visitedPlaceIds;
    private List<String> fullTourPlaceIds;
    private List<String> fullTourPlaceNames;
    private double totalTravelTimeSeconds;
    private double totalTravelTimeMinutes;
    private double totalTravelTimeHours;
    private double executionTimeMs;
    private String algorithm;
    private double memoryEstimationMb;
    private boolean timeoutExceeded;
    private String recurrenceFormula;
    private String theoreticalJustification;
}
