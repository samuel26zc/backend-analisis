package com.analisis.backend.dto.response;

import com.analisis.backend.model.enums.DijkstraVersion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShortestPathResponseDto {
    private String originPlaceId;
    private String originPlaceName;
    private String destinationPlaceId;
    private String destinationPlaceName;
    private Long originNodeId;
    private Long destinationNodeId;
    private double travelTimeSeconds;
    private double travelTimeMinutes;
    private double distanceMeters;
    private double distanceKilometers;
    private int exploredNodesCount;
    private double executionTimeMs;
    private DijkstraVersion version;
    private List<Long> nodePath;
    private List<CoordinateDto> polyline;
    private String invariant;
}
