package com.analisis.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemoryEstimationResponseDto {
    private int nodeCount;
    private int edgeCount;
    private double matrixMegabytes;
    private String matrixAssumptions;
    private double listMegabytes;
    private String listAssumptions;
    private double ratioMatrixToList;
    private String justification;
}
