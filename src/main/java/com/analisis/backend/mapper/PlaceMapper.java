package com.analisis.backend.mapper;

import com.analisis.backend.dto.request.PlaceRequestDto;
import com.analisis.backend.dto.response.PlaceResponseDto;
import com.analisis.backend.model.entity.Place;
import org.springframework.stereotype.Component;

@Component
public class PlaceMapper {

    public PlaceResponseDto toDto(Place place) {
        if (place == null) return null;
        return PlaceResponseDto.builder()
                .id(place.getId())
                .name(place.getName())
                .municipality(place.getMunicipality())
                .category(place.getCategory())
                .latitude(place.getLatitude())
                .longitude(place.getLongitude())
                .utmX(place.getUtmX())
                .utmY(place.getUtmY())
                .visitDurationMinutes(place.getVisitDurationMinutes())
                .nearestNodeId(place.getNearestNodeId())
                .distanceToNodeMeters(place.getDistanceToNodeMeters())
                .isExceeding500m(place.getIsExceeding500m())
                .treatmentNote(place.getTreatmentNote())
                .createdAt(place.getCreatedAt())
                .updatedAt(place.getUpdatedAt())
                .build();
    }

    public Place toEntity(PlaceRequestDto dto, long utmX, long utmY, long nearestNodeId, double distanceMeters, boolean exceeding500m, String treatment) {
        if (dto == null) return null;
        return Place.builder()
                .id(dto.getId())
                .name(dto.getName())
                .municipality(dto.getMunicipality())
                .category(dto.getCategory())
                .latitude(dto.getLatitude())
                .longitude(dto.getLongitude())
                .utmX(utmX)
                .utmY(utmY)
                .visitDurationMinutes(dto.getVisitDurationMinutes())
                .nearestNodeId(nearestNodeId)
                .distanceToNodeMeters(distanceMeters)
                .isExceeding500m(exceeding500m)
                .treatmentNote(treatment)
                .build();
    }
}
