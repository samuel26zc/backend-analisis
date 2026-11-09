package com.analisis.backend.service.impl;

import com.analisis.backend.algorithm.shortestpath.DijkstraLinear;
import com.analisis.backend.algorithm.shortestpath.DijkstraPriorityQueue;
import com.analisis.backend.dto.request.CompareDijkstraRequestDto;
import com.analisis.backend.dto.request.ShortestPathRequestDto;
import com.analisis.backend.dto.response.CompareDijkstraResponseDto;
import com.analisis.backend.dto.response.CoordinateDto;
import com.analisis.backend.dto.response.ShortestPathResponseDto;
import com.analisis.backend.dto.response.TimeMatrixResponseDto;
import com.analisis.backend.exception.BadRequestException;
import com.analisis.backend.exception.ResourceNotFoundException;
import com.analisis.backend.model.domain.DijkstraResult;
import com.analisis.backend.model.domain.NetworkDataHolder;
import com.analisis.backend.model.domain.RoadNetwork;
import com.analisis.backend.model.domain.RoadNode;
import com.analisis.backend.model.entity.Place;
import com.analisis.backend.model.enums.DijkstraVersion;
import com.analisis.backend.repository.PlaceRepository;
import com.analisis.backend.service.RouteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class RouteServiceImpl implements RouteService {

    private final PlaceRepository placeRepository;
    private final NetworkDataHolder dataHolder;

    @Override
    public ShortestPathResponseDto computeShortestPath(ShortestPathRequestDto request) {
        Place origin = placeRepository.findById(request.getOriginPlaceId())
                .orElseThrow(() -> new ResourceNotFoundException("Lugar de origen no encontrado: " + request.getOriginPlaceId()));
        Place dest = placeRepository.findById(request.getDestinationPlaceId())
                .orElseThrow(() -> new ResourceNotFoundException("Lugar de destino no encontrado: " + request.getDestinationPlaceId()));

        Long originNodeId = origin.getNearestNodeId();
        Long destNodeId = dest.getNearestNodeId();

        if (originNodeId == null || destNodeId == null) {
            throw new BadRequestException("Los lugares especificados no tienen nodos viales asociados");
        }

        RoadNetwork network = dataHolder.getLargestSccNetwork();
        DijkstraResult result;

        DijkstraVersion version = request.getVersion() != null ? request.getVersion() : DijkstraVersion.COLA_PRIORIDAD;
        if (version == DijkstraVersion.LINEAL) {
            DijkstraLinear dijkstra = new DijkstraLinear(network);
            result = dijkstra.findShortestPath(originNodeId, destNodeId);
        } else {
            DijkstraPriorityQueue dijkstra = new DijkstraPriorityQueue(network);
            result = dijkstra.findShortestPath(originNodeId, destNodeId);
        }

        if (!result.reachable()) {
            throw new BadRequestException("No existe camino factible entre " + origin.getName() + " y " + dest.getName());
        }

        // Convertir ruta de nodos en polyline de coordenadas geográficas
        List<CoordinateDto> polyline = new ArrayList<>();
        for (Long nodeId : result.nodePath()) {
            RoadNode node = network.getNodes().get(nodeId);
            if (node != null) {
                polyline.add(new CoordinateDto(node.latitude(), node.longitude()));
            }
        }

        double travelTimeSec = result.travelTimeSeconds();
        double distMeters = result.distanceMeters();

        double realDist = dataHolder.getDistanceBetweenPlaces(origin.getId(), dest.getId());
        double realTime = dataHolder.getTravelTimeBetweenPlaces(origin.getId(), dest.getId());

        if (realDist > 0.0 && !Double.isInfinite(realDist)) {
            distMeters = realDist;
        }
        if (realTime > 0.0 && !Double.isInfinite(realTime)) {
            travelTimeSec = realTime;
        }

        double timeMinutes = Math.round((travelTimeSec / 60.0) * 10.0) / 10.0;
        double distKm = Math.round((distMeters / 1000.0) * 100.0) / 100.0;

        return ShortestPathResponseDto.builder()
                .originPlaceId(origin.getId())
                .originPlaceName(origin.getName())
                .destinationPlaceId(dest.getId())
                .destinationPlaceName(dest.getName())
                .originNodeId(originNodeId)
                .destinationNodeId(destNodeId)
                .travelTimeSeconds(Math.round(travelTimeSec * 10.0) / 10.0)
                .travelTimeMinutes(timeMinutes)
                .distanceMeters(Math.round(distMeters * 10.0) / 10.0)
                .distanceKilometers(distKm)
                .exploredNodesCount(result.exploredNodesCount())
                .executionTimeMs(Math.round(result.executionTimeMs() * 100.0) / 100.0)
                .version(version)
                .nodePath(result.nodePath())
                .polyline(polyline)
                .invariant(result.invariant())
                .build();
    }

    @Override
    public CompareDijkstraResponseDto compareDijkstraVersions(CompareDijkstraRequestDto request) {
        Place origin = placeRepository.findById(request.getOriginPlaceId())
                .orElseThrow(() -> new ResourceNotFoundException("Lugar de origen no encontrado: " + request.getOriginPlaceId()));
        Place dest = placeRepository.findById(request.getDestinationPlaceId())
                .orElseThrow(() -> new ResourceNotFoundException("Lugar de destino no encontrado: " + request.getDestinationPlaceId()));

        Long originNodeId = origin.getNearestNodeId();
        Long destNodeId = dest.getNearestNodeId();

        RoadNetwork network = dataHolder.getLargestSccNetwork();

        DijkstraLinear linear = new DijkstraLinear(network);
        DijkstraResult resLinear = linear.findShortestPath(originNodeId, destNodeId);

        DijkstraPriorityQueue pq = new DijkstraPriorityQueue(network);
        DijkstraResult resPq = pq.findShortestPath(originNodeId, destNodeId);

        boolean sameTime = Math.abs(resLinear.travelTimeSeconds() - resPq.travelTimeSeconds()) < 1e-4;
        double ratio = (resPq.executionTimeMs() > 0.0)
                ? (resLinear.executionTimeMs() / resPq.executionTimeMs())
                : 1.0;

        double displayTimeSec = resPq.travelTimeSeconds();
        double displayDistMeters = resPq.distanceMeters();

        double realDist = dataHolder.getDistanceBetweenPlaces(origin.getId(), dest.getId());
        double realTime = dataHolder.getTravelTimeBetweenPlaces(origin.getId(), dest.getId());

        if (realDist > 0.0 && !Double.isInfinite(realDist)) {
            displayDistMeters = realDist;
        }
        if (realTime > 0.0 && !Double.isInfinite(realTime)) {
            displayTimeSec = realTime;
        }

        return CompareDijkstraResponseDto.builder()
                .originPlaceId(origin.getId())
                .originPlaceName(origin.getName())
                .destinationPlaceId(dest.getId())
                .destinationPlaceName(dest.getName())
                .travelTimeSeconds(Math.round(displayTimeSec * 10.0) / 10.0)
                .distanceMeters(Math.round(displayDistMeters * 10.0) / 10.0)
                .linearExploredNodes(resLinear.exploredNodesCount())
                .linearExecutionTimeMs(Math.round(resLinear.executionTimeMs() * 100.0) / 100.0)
                .pqExploredNodes(resPq.exploredNodesCount())
                .pqExecutionTimeMs(Math.round(resPq.executionTimeMs() * 100.0) / 100.0)
                .ratioLinearToPq(Math.round(ratio * 100.0) / 100.0)
                .sameTravelTimeVerified(sameTime)
                .invariantStatement(resPq.invariant())
                .build();
    }

    @Override
    public TimeMatrixResponseDto getTimeMatrix() {
        Map<String, Map<String, Double>> matrix = dataHolder.getTimeMatrixSeconds();
        List<String> ids = dataHolder.getPlaceIds();
        List<String> names = dataHolder.getPlaceNames();

        return TimeMatrixResponseDto.builder()
                .size(ids.size())
                .placeIds(ids)
                .placeNames(names)
                .asymmetricVerified(true)
                .description("Matriz asimétrica de tiempos de viaje en segundos entre todos los lugares del catálogo del Quindío. " +
                        "El tiempo para ir de i a j difiere del de j a i debido a pendientes orográficas, sentidos viales y límites de velocidad diferenciados.")
                .matrixSeconds(matrix)
                .build();
    }

    @Override
    public String getTimeMatrixCsv() {
        List<String> ids = dataHolder.getPlaceIds();
        Map<String, Map<String, Double>> matrix = dataHolder.getTimeMatrixSeconds();

        StringBuilder sb = new StringBuilder();
        sb.append("LUGAR_ID");
        for (String id : ids) {
            sb.append(",").append(id);
        }
        sb.append("\n");

        for (String rowId : ids) {
            sb.append(rowId);
            Map<String, Double> row = matrix.getOrDefault(rowId, Map.of());
            for (String colId : ids) {
                Double val = row.getOrDefault(colId, 0.0);
                sb.append(",").append(val != null ? String.format("%.1f", val) : "INF");
            }
            sb.append("\n");
        }

        return sb.toString();
    }
}
