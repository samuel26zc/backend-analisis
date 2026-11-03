package com.analisis.backend.service;

import com.analisis.backend.dto.response.ConvexHullBenchmarkResponseDto;
import com.analisis.backend.dto.response.ExperimentAllResponseDto;
import com.analisis.backend.dto.response.SystemInfoResponseDto;

import java.util.List;
import java.util.Map;

public interface ExperimentService {

    ExperimentAllResponseDto getAllExperiments();

    SystemInfoResponseDto getSystemInfo();

    List<Map<String, Object>> getR1SccTimes();

    List<Map<String, Object>> getR2DijkstraQueries();

    Map<String, Object> getR2RatioSummary();

    List<ConvexHullBenchmarkResponseDto.BenchmarkRowDto> getR3ConvexHullBenchmarks();

    List<Map<String, Object>> getR4ExactTspTimes();

    List<Map<String, Object>> getR4HeuristicQuality();
}
