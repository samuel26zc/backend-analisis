package com.analisis.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompareDijkstraResponseDto {
    private String originPlaceId;
    private String originPlaceName;
    private String destinationPlaceId;
    private String destinationPlaceName;
    private double travelTimeSeconds;
    private double distanceMeters;
    private int linearExploredNodes;
    private double linearExecutionTimeMs;
    private int pqExploredNodes;
    private double pqExecutionTimeMs;
    private double ratioLinearToPq;
    private boolean sameTravelTimeVerified;
    private String invariantStatement;
}
