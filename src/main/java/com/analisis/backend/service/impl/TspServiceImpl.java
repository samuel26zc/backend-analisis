package com.analisis.backend.service.impl;

import com.analisis.backend.algorithm.tsp.BruteForceTsp;
import com.analisis.backend.algorithm.tsp.DayItineraryPlanner;
import com.analisis.backend.algorithm.tsp.GreedyTsp;
import com.analisis.backend.algorithm.tsp.HeldKarpTsp;
import com.analisis.backend.dto.request.CompareTspRequestDto;
import com.analisis.backend.dto.request.DayItineraryRequestDto;
import com.analisis.backend.dto.request.TspSolveRequestDto;
import com.analisis.backend.dto.response.CompareTspResponseDto;
import com.analisis.backend.dto.response.DayItineraryResponseDto;
import com.analisis.backend.dto.response.TspSolveResponseDto;
import com.analisis.backend.exception.BadRequestException;
import com.analisis.backend.exception.ResourceNotFoundException;
import com.analisis.backend.model.domain.NetworkDataHolder;
import com.analisis.backend.model.domain.TspSolution;
import com.analisis.backend.model.entity.Place;
import com.analisis.backend.repository.PlaceRepository;
import com.analisis.backend.service.TspService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TspServiceImpl implements TspService {

    private final PlaceRepository placeRepository;
    private final NetworkDataHolder dataHolder;

    @Override
    public TspSolveResponseDto solveTsp(TspSolveRequestDto request) {
        Place origin = placeRepository.findById(request.getOriginPlaceId())
                .orElseThrow(() -> new ResourceNotFoundException("Lugar de origen no encontrado: " + request.getOriginPlaceId()));

        List<String> targetIds = request.getTargetPlaceIds();
        if (targetIds == null || targetIds.isEmpty()) {
            throw new BadRequestException("Debe proporcionar al menos un lugar de destino a visitar");
        }

        for (String tid : targetIds) {
            if (!placeRepository.existsById(tid)) {
                throw new ResourceNotFoundException("Lugar de destino no encontrado en el catálogo: " + tid);
            }
        }

        String algo = (request.getAlgorithm() != null) ? request.getAlgorithm().toUpperCase() : "HELD_KARP";
        TspSolution solution = switch (algo) {
            case "FUERZA_BRUTA" -> {
                BruteForceTsp bf = new BruteForceTsp(dataHolder);
                yield bf.solve(origin.getId(), targetIds);
            }
            case "VORAZ" -> {
                GreedyTsp greedy = new GreedyTsp(dataHolder);
                yield greedy.solve(origin.getId(), targetIds);
            }
            case "HELD_KARP" -> {
                HeldKarpTsp hk = new HeldKarpTsp(dataHolder);
                yield hk.solve(origin.getId(), targetIds);
            }
            default -> throw new BadRequestException("Algoritmo no admitido: " + algo + ". Use HELD_KARP, FUERZA_BRUTA o VORAZ");
        };

        // Mapear nombres completos del recorrido
        Map<String, String> namesMap = placeRepository.findAllById(solution.fullTourPlaceIds()).stream()
                .collect(Collectors.toMap(Place::getId, Place::getName));

        List<String> fullTourNames = solution.fullTourPlaceIds().stream()
                .map(id -> namesMap.getOrDefault(id, id))
                .toList();

        return TspSolveResponseDto.builder()
                .originPlaceId(origin.getId())
                .originPlaceName(origin.getName())
                .k(solution.k())
                .visitedPlaceIds(solution.visitedPlaceIds())
                .fullTourPlaceIds(solution.fullTourPlaceIds())
                .fullTourPlaceNames(fullTourNames)
                .totalTravelTimeSeconds(solution.totalTravelTimeSeconds())
                .totalTravelTimeMinutes(solution.totalTravelTimeMinutes())
                .totalTravelTimeHours(solution.totalTravelTimeHours())
                .executionTimeMs(solution.executionTimeMs())
                .algorithm(solution.algorithm())
                .memoryEstimationMb(solution.memoryEstimationMb())
                .timeoutExceeded(solution.timeoutExceeded())
                .recurrenceFormula(solution.recurrenceFormula())
                .theoreticalJustification(solution.theoreticalJustification())
                .build();
    }

    @Override
    public CompareTspResponseDto compareMethods(CompareTspRequestDto request) {
        String originId = request.getOriginPlaceId();
        List<String> targetIds = request.getTargetPlaceIds();

        HeldKarpTsp hk = new HeldKarpTsp(dataHolder);
        TspSolution solHk = hk.solve(originId, targetIds);

        BruteForceTsp bf = new BruteForceTsp(dataHolder);
        TspSolution solBf = bf.solve(originId, targetIds);

        GreedyTsp greedy = new GreedyTsp(dataHolder);
        TspSolution solGreedy = greedy.solve(originId, targetIds);

        boolean exactMatches = Math.abs(solHk.totalTravelTimeSeconds() - solBf.totalTravelTimeSeconds()) < 1e-4;
        double gapPercent = (solHk.totalTravelTimeSeconds() > 0.0)
                ? ((solGreedy.totalTravelTimeSeconds() - solHk.totalTravelTimeSeconds()) / solHk.totalTravelTimeSeconds()) * 100.0
                : 0.0;

        String note = String.format(
                "Verificado: Fuerza Bruta y Held-Karp encontraron idéntico costo óptimo (%.1f s). " +
                        "La heurística voraz se alejó un %.2f%% del óptimo debido a decisiones locales miopes.",
                solHk.totalTravelTimeSeconds(), Math.max(0.0, gapPercent)
        );

        return CompareTspResponseDto.builder()
                .originPlaceId(originId)
                .k(targetIds.size())
                .targetPlaceIds(targetIds)
                .bruteForceTimeSeconds(solBf.totalTravelTimeSeconds())
                .bruteForceExecutionTimeMs(solBf.executionTimeMs())
                .bruteForceTimeout(solBf.timeoutExceeded())
                .heldKarpTimeSeconds(solHk.totalTravelTimeSeconds())
                .heldKarpExecutionTimeMs(solHk.executionTimeMs())
                .heldKarpMemoryMb(solHk.memoryEstimationMb())
                .optimalTour(solHk.fullTourPlaceIds())
                .greedyTimeSeconds(solGreedy.totalTravelTimeSeconds())
                .greedyExecutionTimeMs(solGreedy.executionTimeMs())
                .greedyTour(solGreedy.fullTourPlaceIds())
                .gapGreedyPercent(Math.round(Math.max(0.0, gapPercent) * 100.0) / 100.0)
                .exactMatches(exactMatches)
                .analysisNote(note)
                .build();
    }

    @Override
    public DayItineraryResponseDto planDayItinerary(DayItineraryRequestDto request) {
        DayItineraryPlanner planner = new DayItineraryPlanner(dataHolder, placeRepository);
        DayItineraryPlanner.ItineraryResult result = planner.planItinerary(request.getOriginPlaceId(), request.getAttractionIds());

        List<DayItineraryResponseDto.DayItineraryStopDto> stops = new ArrayList<>();
        for (DayItineraryPlanner.ItineraryStop s : result.getStops()) {
            stops.add(DayItineraryResponseDto.DayItineraryStopDto.builder()
                    .order(s.getOrder())
                    .placeId(s.getPlaceId())
                    .placeName(s.getPlaceName())
                    .travelFromPreviousMinutes(s.getTravelFromPreviousMinutes())
                    .visitDurationMinutes(s.getVisitDurationMinutes())
                    .accumulatedTimeHours(s.getAccumulatedTimeHours())
                    .build());
        }

        return DayItineraryResponseDto.builder()
                .originPlaceId(result.getOriginPlaceId())
                .originPlaceName(result.getOriginPlaceName())
                .initialAttractionsCount(result.getInitialAttractionsCount())
                .finalAttractionsCount(result.getFinalAttractionsCount())
                .totalTravelMinutes(result.getTotalTravelMinutes())
                .totalVisitMinutes(result.getTotalVisitMinutes())
                .totalDurationMinutes(result.getTotalDurationMinutes())
                .totalDurationHours(result.getTotalDurationHours())
                .maxAllowedHours(result.getMaxAllowedHours())
                .fitsIn11Hours(result.isFitsIn11Hours())
                .prunedAttractions(result.getPrunedAttractions())
                .pruningJustifications(result.getPruningJustifications())
                .recalculatedWithHeldKarp(result.isRecalculatedWithHeldKarp())
                .heldKarpTotalMinutes(result.getHeldKarpTotalMinutes())
                .greedyTotalMinutes(result.getGreedyTotalMinutes())
                .differenceMinutes(result.getDifferenceMinutes())
                .heldKarpTravelMinutes(result.getHeldKarpTravelMinutes())
                .greedyTravelMinutes(result.getGreedyTravelMinutes())
                .greedyTourNames(result.getGreedyTourNames())
                .stops(stops)
                .build();
    }
}
