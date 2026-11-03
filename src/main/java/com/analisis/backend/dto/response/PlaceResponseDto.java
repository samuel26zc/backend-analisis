package com.analisis.backend.dto.response;

import com.analisis.backend.model.enums.PlaceCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaceResponseDto {
    private String id;
    private String name;
    private String municipality;
    private PlaceCategory category;
    private double latitude;
    private double longitude;
    private long utmX;
    private long utmY;
    private int visitDurationMinutes;
    private Long nearestNodeId;
    private Double distanceToNodeMeters;
    private Boolean isExceeding500m;
    private String treatmentNote;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
