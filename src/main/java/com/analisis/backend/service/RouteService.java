package com.analisis.backend.service;

import com.analisis.backend.dto.request.CompareDijkstraRequestDto;
import com.analisis.backend.dto.request.ShortestPathRequestDto;
import com.analisis.backend.dto.response.CompareDijkstraResponseDto;
import com.analisis.backend.dto.response.ShortestPathResponseDto;
import com.analisis.backend.dto.response.TimeMatrixResponseDto;

public interface RouteService {

    ShortestPathResponseDto computeShortestPath(ShortestPathRequestDto request);

    CompareDijkstraResponseDto compareDijkstraVersions(CompareDijkstraRequestDto request);

    TimeMatrixResponseDto getTimeMatrix();

    String getTimeMatrixCsv();
}
