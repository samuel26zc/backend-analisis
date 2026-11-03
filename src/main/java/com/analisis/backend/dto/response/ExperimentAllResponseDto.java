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
public class ExperimentAllResponseDto {
    private SystemInfoResponseDto systemInfo;
    private List<Map<String, Object>> r1SccTimes;
    private List<Map<String, Object>> r2DijkstraQueries;
    private Map<String, Object> r2RatioSummary;
    private List<ConvexHullBenchmarkResponseDto.BenchmarkRowDto> r3ConvexHullBenchmarks;
    private List<Map<String, Object>> r4ExactTspTimes;
    private List<Map<String, Object>> r4HeuristicQuality;
    private DayItineraryResponseDto r4SampleItinerary;
}
