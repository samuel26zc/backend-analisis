package com.analisis.backend.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConvexHullBenchmarkRequestDto {
    @Builder.Default
    private List<Integer> pointCounts = List.of(1000, 10000, 50000);

    @Builder.Default
    private long seed = 42L;
}
