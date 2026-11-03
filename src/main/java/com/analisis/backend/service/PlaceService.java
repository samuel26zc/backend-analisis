package com.analisis.backend.service;

import com.analisis.backend.dto.request.PlaceRequestDto;
import com.analisis.backend.dto.response.PlaceMappingResponseDto;
import com.analisis.backend.dto.response.PlaceResponseDto;
import com.analisis.backend.model.enums.PlaceCategory;

import java.util.List;

public interface PlaceService {

    List<PlaceResponseDto> getAllPlaces(PlaceCategory category, String municipality);

    PlaceResponseDto getPlaceById(String id);

    PlaceMappingResponseDto getMappingReport();

    List<String> getMunicipalities();

    List<PlaceCategory> getCategories();

    PlaceResponseDto createPlace(PlaceRequestDto requestDto);

    PlaceResponseDto updatePlace(String id, PlaceRequestDto requestDto);

    void deletePlace(String id);
}
