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
public class CompareTspResponseDto {
    private String originPlaceId;
    private int k;
    private List<String> targetPlaceIds;
    private double bruteForceTimeSeconds;
    private double bruteForceExecutionTimeMs;
    private boolean bruteForceTimeout;
    private double heldKarpTimeSeconds;
    private double heldKarpExecutionTimeMs;
    private double heldKarpMemoryMb;
    private List<String> optimalTour;
    private double greedyTimeSeconds;
    private double greedyExecutionTimeMs;
    private List<String> greedyTour;
    private double gapGreedyPercent;
    private boolean exactMatches;
    private String analysisNote;
}
