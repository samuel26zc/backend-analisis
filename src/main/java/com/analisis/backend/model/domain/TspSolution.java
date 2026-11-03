package com.analisis.backend.model.domain;

import java.util.List;

/**
 * Resultado estructurado de una ejecución del problema del viajante asimétrico (ATSP).
 */
public record TspSolution(
        String originPlaceId,
        int k,
        List<String> visitedPlaceIds,
        List<String> fullTourPlaceIds,
        double totalTravelTimeSeconds,
        double totalTravelTimeMinutes,
        double totalTravelTimeHours,
        double executionTimeMs,
        String algorithm,
        double memoryEstimationMb,
        boolean timeoutExceeded,
        String recurrenceFormula,
        String theoreticalJustification
) {}
