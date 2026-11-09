package com.analisis.backend.algorithm.tsp;

import com.analisis.backend.model.domain.NetworkDataHolder;
import com.analisis.backend.model.domain.TspSolution;
import com.analisis.backend.model.entity.Place;
import com.analisis.backend.repository.PlaceRepository;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Planificador del Itinerario de un día (Jornada máxima de 11 horas / 660 minutos).
 * Resuelve el orden óptimo de visita mediante Held-Karp y, si la suma de desplazamientos
 * y duraciones de visita excede 11 horas, poda sistemáticamente los atractivos de mayor impacto
 * justificando formalmente cada exclusión.
 */
public class DayItineraryPlanner {

    private final NetworkDataHolder dataHolder;
    private final PlaceRepository placeRepository;
    private static final double MAX_DAY_MINUTES = 11.0 * 60.0; // 660 minutos

    public DayItineraryPlanner(NetworkDataHolder dataHolder, PlaceRepository placeRepository) {
        this.dataHolder = dataHolder;
        this.placeRepository = placeRepository;
    }

    @Data
    @Builder
    public static class ItineraryResult {
        private String originPlaceId;
        private String originPlaceName;
        private int initialAttractionsCount;
        private int finalAttractionsCount;
        private double totalTravelMinutes;
        private double totalVisitMinutes;
        private double totalDurationMinutes;
        private double totalDurationHours;
        private double maxAllowedHours;
        private boolean fitsIn11Hours;
        private List<String> prunedAttractions;
        private List<String> pruningJustifications;
        private boolean recalculatedWithHeldKarp;
        private double heldKarpTotalMinutes;
        private double greedyTotalMinutes;
        private double differenceMinutes;
        private double heldKarpTravelMinutes;
        private double greedyTravelMinutes;
        private List<String> greedyTourNames;
        private List<ItineraryStop> stops;
    }

    @Data
    @Builder
    public static class ItineraryStop {
        private String order;
        private String placeId;
        private String placeName;
        private double travelFromPreviousMinutes;
        private double visitDurationMinutes;
        private double accumulatedTimeHours;
    }

    public ItineraryResult planItinerary(String originId, List<String> attractionIds) {
        Place originPlace = placeRepository.findById(originId).orElse(null);
        String originName = (originPlace != null) ? originPlace.getName() : originId;

        // Cargar lugares de la base de datos
        Map<String, Place> placeMap = new HashMap<>();
        for (String id : attractionIds) {
            placeRepository.findById(id).ifPresent(p -> placeMap.put(id, p));
        }

        List<String> currentAttractions = new ArrayList<>(attractionIds);
        List<String> prunedAttractions = new ArrayList<>();
        List<String> pruningJustifications = new ArrayList<>();

        HeldKarpTsp heldKarp = new HeldKarpTsp(dataHolder);
        TspSolution solution = null;
        double totalTravelMin = 0.0;
        double totalVisitMin = 0.0;
        double totalDurationMin = 0.0;

        // Bucle de optimización y poda
        while (!currentAttractions.isEmpty()) {
            solution = heldKarp.solve(originId, currentAttractions);
            totalTravelMin = solution.totalTravelTimeMinutes();

            totalVisitMin = 0.0;
            for (String aId : currentAttractions) {
                Place p = placeMap.get(aId);
                if (p != null) {
                    totalVisitMin += p.getVisitDurationMinutes();
                }
            }

            totalDurationMin = totalTravelMin + totalVisitMin;

            if (totalDurationMin <= MAX_DAY_MINUTES || currentAttractions.size() <= 2) {
                // Cabe en 11 horas o se llegó al mínimo viable
                break;
            }

            // Poda: seleccionar el atractivo con mayor duración de visita que causa el exceso
            String candidateToPrune = currentAttractions.stream()
                    .max(Comparator.comparingInt(id -> {
                        Place p = placeMap.get(id);
                        return p != null ? p.getVisitDurationMinutes() : 0;
                    }))
                    .orElse(currentAttractions.get(currentAttractions.size() - 1));

            Place pPruned = placeMap.get(candidateToPrune);
            String prunedName = (pPruned != null) ? pPruned.getName() : candidateToPrune;
            String municipality = (pPruned != null) ? pPruned.getMunicipality() : "";
            int visitMin = (pPruned != null) ? pPruned.getVisitDurationMinutes() : 0;

            double durationHoursBeforePrune = Math.round((totalDurationMin / 60.0) * 100.0) / 100.0;
            prunedAttractions.add(prunedName);
            pruningJustifications.add(String.format(
                    "Se descarta '%s' (%s) con duración de visita estimada de %d min (%.1f h). " +
                            "La duración combinada total alcanzaba %.2f h, superando la jornada máxima de 11.0 h.",
                    prunedName, municipality, visitMin, visitMin / 60.0, durationHoursBeforePrune
            ));

            currentAttractions.remove(candidateToPrune);
        }

        // Construir tabla paso a paso del itinerario
        List<ItineraryStop> stops = new ArrayList<>();
        double accumulatedMinutes = 0.0;
        String previousPlaceId = originId;

        if (solution != null && !solution.visitedPlaceIds().isEmpty()) {
            int stepOrder = 1;
            for (String currentPlaceId : solution.visitedPlaceIds()) {
                Place p = placeMap.get(currentPlaceId);
                String name = (p != null) ? p.getName() : currentPlaceId;
                double visitMin = (p != null) ? p.getVisitDurationMinutes() : 0.0;

                double travelSec = dataHolder.getTravelTimeBetweenPlaces(previousPlaceId, currentPlaceId);
                double travelMin = Math.round((travelSec / 60.0) * 10.0) / 10.0;

                accumulatedMinutes += travelMin + visitMin;
                double accHours = Math.round((accumulatedMinutes / 60.0) * 100.0) / 100.0;

                stops.add(ItineraryStop.builder()
                        .order(String.valueOf(stepOrder++))
                        .placeId(currentPlaceId)
                        .placeName(name)
                        .travelFromPreviousMinutes(travelMin)
                        .visitDurationMinutes(visitMin)
                        .accumulatedTimeHours(accHours)
                        .build());

                previousPlaceId = currentPlaceId;
            }

            // Regreso al origen
            double returnSec = dataHolder.getTravelTimeBetweenPlaces(previousPlaceId, originId);
            double returnMin = Math.round((returnSec / 60.0) * 10.0) / 10.0;
            accumulatedMinutes += returnMin;
            double finalAccHours = Math.round((accumulatedMinutes / 60.0) * 100.0) / 100.0;

            stops.add(ItineraryStop.builder()
                    .order("Regreso")
                    .placeId(originId)
                    .placeName(originName)
                    .travelFromPreviousMinutes(returnMin)
                    .visitDurationMinutes(0.0)
                    .accumulatedTimeHours(finalAccHours)
                    .build());
        }

        // Comparación con la Heurística Voraz sobre el conjunto final de atractivos seleccionados
        GreedyTsp greedyTsp = new GreedyTsp(dataHolder);
        TspSolution greedySol = greedyTsp.solve(originId, currentAttractions);
        double greedyTravelMin = greedySol.totalTravelTimeMinutes();
        double greedyTotalMin = greedyTravelMin + totalVisitMin;
        double diffMin = Math.max(0.0, greedyTotalMin - totalDurationMin);

        List<String> greedyTourNames = new ArrayList<>();
        for (String gId : greedySol.fullTourPlaceIds()) {
            Place gp = placeMap.get(gId);
            if (gp != null) {
                greedyTourNames.add(gp.getName());
            } else if (gId.equals(originId)) {
                greedyTourNames.add(originName);
            } else {
                greedyTourNames.add(gId);
            }
        }

        double totalHours = Math.round((totalDurationMin / 60.0) * 100.0) / 100.0;

        return ItineraryResult.builder()
                .originPlaceId(originId)
                .originPlaceName(originName)
                .initialAttractionsCount(attractionIds.size())
                .finalAttractionsCount(currentAttractions.size())
                .totalTravelMinutes(Math.round(totalTravelMin * 10.0) / 10.0)
                .totalVisitMinutes(Math.round(totalVisitMin * 10.0) / 10.0)
                .totalDurationMinutes(Math.round(totalDurationMin * 10.0) / 10.0)
                .totalDurationHours(totalHours)
                .maxAllowedHours(11.0)
                .fitsIn11Hours(totalDurationMin <= MAX_DAY_MINUTES)
                .prunedAttractions(prunedAttractions)
                .pruningJustifications(pruningJustifications)
                .recalculatedWithHeldKarp(true)
                .heldKarpTotalMinutes(Math.round(totalDurationMin * 10.0) / 10.0)
                .greedyTotalMinutes(Math.round(greedyTotalMin * 10.0) / 10.0)
                .differenceMinutes(Math.round(diffMin * 10.0) / 10.0)
                .heldKarpTravelMinutes(Math.round(totalTravelMin * 10.0) / 10.0)
                .greedyTravelMinutes(Math.round(greedyTravelMin * 10.0) / 10.0)
                .greedyTourNames(greedyTourNames)
                .stops(stops)
                .build();
    }
}
