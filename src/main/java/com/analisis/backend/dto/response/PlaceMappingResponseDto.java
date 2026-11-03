package com.analisis.backend.dto.response;

import com.analisis.backend.model.enums.PlaceCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaceMappingResponseDto {
    private int totalPlaces;
    private int exceeding500mCount;
    private String generalTreatmentStrategy;
    private List<PlaceMappingDetailDto> details;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlaceMappingDetailDto {
        private String placeId;
        private String placeName;
        private String municipality;
        private PlaceCategory category;
        private Long nearestNodeId;
        private Double distanceMeters;
        private Boolean isExceeding500m;
        private String treatment;
    }
}
