package com.analisis.backend.service;

import com.analisis.backend.dto.response.MemoryEstimationResponseDto;
import com.analisis.backend.dto.response.NetworkStatsResponseDto;
import com.analisis.backend.dto.response.SccResponseDto;
import com.analisis.backend.model.domain.RoadNode;

import java.util.List;

public interface NetworkService {

    NetworkStatsResponseDto getNetworkStats();

    MemoryEstimationResponseDto getMemoryEstimation();

    SccResponseDto getSccReport();

    List<RoadNode> getSccNodes();
}
