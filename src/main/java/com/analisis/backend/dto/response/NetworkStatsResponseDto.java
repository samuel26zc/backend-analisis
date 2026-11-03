package com.analisis.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NetworkStatsResponseDto {
    private int totalNodes;
    private int totalEdges;
    private int sccNodes;
    private int sccEdges;
    private int totalPlaces;
    private String downloadSource;
    private String networkType;
    private String coordinateSystem;
    private String speedTreatmentNote;
}
