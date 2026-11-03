package com.analisis.backend.service;

import com.analisis.backend.dto.request.CompareTspRequestDto;
import com.analisis.backend.dto.request.DayItineraryRequestDto;
import com.analisis.backend.dto.request.TspSolveRequestDto;
import com.analisis.backend.dto.response.CompareTspResponseDto;
import com.analisis.backend.dto.response.DayItineraryResponseDto;
import com.analisis.backend.dto.response.TspSolveResponseDto;

public interface TspService {

    TspSolveResponseDto solveTsp(TspSolveRequestDto request);

    CompareTspResponseDto compareMethods(CompareTspRequestDto request);

    DayItineraryResponseDto planDayItinerary(DayItineraryRequestDto request);
}
