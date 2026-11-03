package com.analisis.backend.service.impl;

import com.analisis.backend.dto.request.PlaceRequestDto;
import com.analisis.backend.dto.response.PlaceMappingResponseDto;
import com.analisis.backend.dto.response.PlaceResponseDto;
import com.analisis.backend.exception.BadRequestException;
import com.analisis.backend.exception.ResourceNotFoundException;
import com.analisis.backend.mapper.PlaceMapper;
import com.analisis.backend.model.domain.NetworkDataHolder;
import com.analisis.backend.model.domain.RoadNode;
import com.analisis.backend.model.entity.Place;
import com.analisis.backend.model.enums.PlaceCategory;
import com.analisis.backend.repository.PlaceRepository;
import com.analisis.backend.service.PlaceService;
import com.analisis.backend.util.GeoUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlaceServiceImpl implements PlaceService {

    private final PlaceRepository placeRepository;
    private final PlaceMapper placeMapper;
    private final NetworkDataHolder dataHolder;

    @Override
    @Transactional(readOnly = true)
    public List<PlaceResponseDto> getAllPlaces(PlaceCategory category, String municipality) {
        List<Place> places;
        if (category != null && municipality != null && !municipality.isBlank()) {
            places = placeRepository.findByCategoryAndMunicipalityIgnoreCase(category, municipality);
        } else if (category != null) {
            places = placeRepository.findByCategory(category);
        } else if (municipality != null && !municipality.isBlank()) {
            places = placeRepository.findByMunicipalityIgnoreCase(municipality);
        } else {
            places = placeRepository.findAll();
        }
        return places.stream().map(placeMapper::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PlaceResponseDto getPlaceById(String id) {
        Place place = placeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lugar no encontrado en el catálogo con id: " + id));
        return placeMapper.toDto(place);
    }

    @Override
    @Transactional(readOnly = true)
    public PlaceMappingResponseDto getMappingReport() {
        List<Place> places = placeRepository.findAll();
        long exceedingCount = places.stream().filter(p -> Boolean.TRUE.equals(p.getIsExceeding500m())).count();

        List<PlaceMappingResponseDto.PlaceMappingDetailDto> details = places.stream()
                .map(p -> PlaceMappingResponseDto.PlaceMappingDetailDto.builder()
                        .placeId(p.getId())
                        .placeName(p.getName())
                        .municipality(p.getMunicipality())
                        .category(p.getCategory())
                        .nearestNodeId(p.getNearestNodeId())
                        .distanceMeters(p.getDistanceToNodeMeters())
                        .isExceeding500m(p.getIsExceeding500m())
                        .treatment(p.getTreatmentNote())
                        .build())
                .toList();

        String strategy = "Para lugares situados a más de 500 metros del nodo más cercano de la red vial principal " +
                "(e.g., senderos naturales o fincas en zonas de ladera profunda), se proyecta ortogonalmente su acceso " +
                "al nodo vehicular de la red y se adiciona una arista virtual peatonal o de acceso con velocidad reducida.";

        return PlaceMappingResponseDto.builder()
                .totalPlaces(places.size())
                .exceeding500mCount((int) exceedingCount)
                .generalTreatmentStrategy(strategy)
                .details(details)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getMunicipalities() {
        return placeRepository.findDistinctMunicipalities();
    }

    @Override
    public List<PlaceCategory> getCategories() {
        return Arrays.asList(PlaceCategory.values());
    }

    @Override
    @Transactional
    public PlaceResponseDto createPlace(PlaceRequestDto dto) {
        if (placeRepository.existsById(dto.getId())) {
            throw new BadRequestException("Ya existe un lugar registrado con el identificador: " + dto.getId());
        }

        GeoUtils.UtmPoint utm = GeoUtils.toUtmZone18N(dto.getLatitude(), dto.getLongitude());
        RoadNode nearestNode = dataHolder.getLargestSccNetwork().findNearestNode(utm.utmX(), utm.utmY());
        if (nearestNode == null) {
            nearestNode = dataHolder.getFullNetwork().findNearestNode(utm.utmX(), utm.utmY());
        }

        long nearestId = (nearestNode != null) ? nearestNode.id() : 1001L;
        double distance = (nearestNode != null)
                ? Math.round(GeoUtils.euclideanDistance(utm.utmX(), utm.utmY(), nearestNode.utmX(), nearestNode.utmY()) * 10.0) / 10.0
                : 0.0;
        boolean exceeding = distance > 500.0;
        String treatment = exceeding
                ? String.format("Ubicado a %.1f m del nodo vial. Proyección ortogonal y enlace virtual rural.", distance)
                : "Conexión directa inmediata al nodo vial de la red.";

        Place place = placeMapper.toEntity(dto, utm.utmX(), utm.utmY(), nearestId, distance, exceeding, treatment);
        Place saved = placeRepository.save(place);
        return placeMapper.toDto(saved);
    }

    @Override
    @Transactional
    public PlaceResponseDto updatePlace(String id, PlaceRequestDto dto) {
        Place existing = placeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lugar no encontrado en el catálogo con id: " + id));

        GeoUtils.UtmPoint utm = GeoUtils.toUtmZone18N(dto.getLatitude(), dto.getLongitude());
        RoadNode nearestNode = dataHolder.getLargestSccNetwork().findNearestNode(utm.utmX(), utm.utmY());
        long nearestId = (nearestNode != null) ? nearestNode.id() : existing.getNearestNodeId();
        double distance = (nearestNode != null)
                ? Math.round(GeoUtils.euclideanDistance(utm.utmX(), utm.utmY(), nearestNode.utmX(), nearestNode.utmY()) * 10.0) / 10.0
                : existing.getDistanceToNodeMeters();
        boolean exceeding = distance > 500.0;
        String treatment = exceeding
                ? String.format("Ubicado a %.1f m del nodo vial. Proyección ortogonal y enlace virtual rural.", distance)
                : "Conexión directa inmediata al nodo vial de la red.";

        existing.setName(dto.getName());
        existing.setMunicipality(dto.getMunicipality());
        existing.setCategory(dto.getCategory());
        existing.setLatitude(dto.getLatitude());
        existing.setLongitude(dto.getLongitude());
        existing.setUtmX(utm.utmX());
        existing.setUtmY(utm.utmY());
        existing.setVisitDurationMinutes(dto.getVisitDurationMinutes());
        existing.setNearestNodeId(nearestId);
        existing.setDistanceToNodeMeters(distance);
        existing.setIsExceeding500m(exceeding);
        existing.setTreatmentNote(treatment);

        Place updated = placeRepository.save(existing);
        return placeMapper.toDto(updated);
    }

    @Override
    @Transactional
    public void deletePlace(String id) {
        if (!placeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Lugar no encontrado en el catálogo con id: " + id);
        }
        placeRepository.deleteById(id);
    }
}
