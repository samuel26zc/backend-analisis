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
public class ConvexHullBenchmarkResponseDto {
    private long seed;
    private String note;
    private List<BenchmarkRowDto> rows;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BenchmarkRowDto {
        private String pointSet;
        private int n;
        private int hullVertices;
        private double ownTimeMs;
        private double referenceTimeMs;
        private double ratio;
    }
}
