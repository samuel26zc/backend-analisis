package com.analisis.backend.service;

import com.analisis.backend.dto.request.ConvexHullBenchmarkRequestDto;
import com.analisis.backend.dto.request.ConvexHullComputeRequestDto;
import com.analisis.backend.dto.response.ConvexHullBenchmarkResponseDto;
import com.analisis.backend.dto.response.ConvexHullResponseDto;

public interface ConvexHullService {

    ConvexHullResponseDto computeNetworkConvexHull();

    ConvexHullResponseDto computeCustomConvexHull(ConvexHullComputeRequestDto request);

    ConvexHullBenchmarkResponseDto runBenchmark(ConvexHullBenchmarkRequestDto request);
}
