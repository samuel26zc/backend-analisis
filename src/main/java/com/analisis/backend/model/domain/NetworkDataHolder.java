package com.analisis.backend.model.domain;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Contenedor en memoria del grafo de la red vial, su mayor componente conexa (SCC)
 * y la matriz asimétrica de tiempos calculada entre los 42 lugares.
 */
@Component
@Getter
@Setter
public class NetworkDataHolder {

    private RoadNetwork fullNetwork = new RoadNetwork();
    private RoadNetwork largestSccNetwork = new RoadNetwork();
    private SccResult sccResult;
    private List<String> placeIds = Collections.emptyList();
    private List<String> placeNames = Collections.emptyList();
    private Map<String, Map<String, Double>> timeMatrixSeconds = new HashMap<>();
    private Map<String, Map<String, Double>> distanceMatrixMeters = new HashMap<>();
    private Map<String, Long> placeNodeMapping = new HashMap<>();
    private Map<Long, String> nodePlaceMapping = new HashMap<>();

    public double getTravelTimeBetweenPlaces(String originPlaceId, String destinationPlaceId) {
        if (originPlaceId.equals(destinationPlaceId)) {
            return 0.0;
        }
        Map<String, Double> originRow = timeMatrixSeconds.get(originPlaceId);
        if (originRow != null && originRow.containsKey(destinationPlaceId)) {
            return originRow.get(destinationPlaceId);
        }
        return Double.POSITIVE_INFINITY;
    }

    public double getDistanceBetweenPlaces(String originPlaceId, String destinationPlaceId) {
        if (originPlaceId.equals(destinationPlaceId)) {
            return 0.0;
        }
        Map<String, Double> originRow = distanceMatrixMeters.get(originPlaceId);
        if (originRow != null && originRow.containsKey(destinationPlaceId)) {
            return originRow.get(destinationPlaceId);
        }
        return Double.POSITIVE_INFINITY;
    }
}
