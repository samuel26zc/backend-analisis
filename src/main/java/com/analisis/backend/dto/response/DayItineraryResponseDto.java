package com.analisis.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DayItineraryResponseDto {
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
    private List<DayItineraryStopDto> stops;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DayItineraryStopDto {
        private String order;
        private String placeId;
        private String placeName;
        private double travelFromPreviousMinutes;
        private double visitDurationMinutes;
        private double accumulatedTimeHours;
    }
}
