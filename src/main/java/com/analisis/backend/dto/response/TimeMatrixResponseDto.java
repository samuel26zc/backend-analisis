package com.analisis.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeMatrixResponseDto {
    private int size;
    private List<String> placeIds;
    private List<String> placeNames;
    private boolean asymmetricVerified;
    private String description;
    private Map<String, Map<String, Double>> matrixSeconds;
}
