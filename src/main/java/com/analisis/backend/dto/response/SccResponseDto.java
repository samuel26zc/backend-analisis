package com.analisis.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SccResponseDto {
    private int totalNodesBefore;
    private int totalEdgesBefore;
    private int sccNodesCount;
    private int sccEdgesCount;
    private double retentionPercentageNodes;
    private double retentionPercentageEdges;
    private double executionTimeMs;
    private int totalComponents;
    private String algorithmName;
    private String complexity;
    private String justification;
}
