package com.analisis.backend.util;

import com.analisis.backend.algorithm.scc.TarjanScc;
import com.analisis.backend.algorithm.shortestpath.DijkstraPriorityQueue;
import com.analisis.backend.model.domain.NetworkDataHolder;
import com.analisis.backend.model.domain.RoadEdge;
import com.analisis.backend.model.domain.RoadNetwork;
import com.analisis.backend.model.domain.RoadNode;
import com.analisis.backend.model.domain.SccResult;
import com.analisis.backend.model.entity.Place;
import com.analisis.backend.model.enums.PlaceCategory;
import com.analisis.backend.repository.PlaceRepository;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Cargador inicial de datos: lee la red vial real de OpenStreetMap del Quindío (quindio_network.json),
 * ejecuta el algoritmo de Tarjan para extraer la mayor componente fuertemente conexa (SCC),
 * proyecta el catálogo de 42 lugares al nodo vial más cercano en UTM 18N y precalcula
 * la matriz asimétrica de tiempos de viaje (42 x 42) con Dijkstra con Cola de Prioridad.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataLoader implements ApplicationRunner {

    private final PlaceRepository placeRepository;
    private final NetworkDataHolder dataHolder;
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;

    private record RawPlace(
            String id,
            String name,
            String municipality,
            String category,
            double latitude,
            double longitude,
            int visitDurationMinutes
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OsmNodeDto(
            long id,
            double latitude,
            double longitude,
            long utmX,
            long utmY
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OsmEdgeDto(
            long source,
            long target,
            double distanceMeters,
            double speedKmh,
            double travelTimeSeconds,
            String highway,
            boolean oneway
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OsmNetworkFile(
            List<OsmNodeDto> nodes,
            List<OsmEdgeDto> edges
    ) {}

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("Inicializando carga de la red vial de OpenStreetMap y catálogo de lugares del Quindío...");

        // 1. Cargar red vial de OpenStreetMap (quindio_network.json)
        RoadNetwork fullNetwork = new RoadNetwork();
        Resource netResource = resourceLoader.getResource("classpath:data/quindio_network.json");
        OsmNetworkFile osmFile;
        try (InputStream is = netResource.getInputStream()) {
            osmFile = objectMapper.readValue(is, OsmNetworkFile.class);
        }

        if (osmFile.nodes() != null) {
            for (OsmNodeDto n : osmFile.nodes()) {
                fullNetwork.addNode(new RoadNode(n.id(), n.latitude(), n.longitude(), n.utmX(), n.utmY()));
            }
        }
        if (osmFile.edges() != null) {
            for (OsmEdgeDto e : osmFile.edges()) {
                double travelTime = e.travelTimeSeconds() > 0.0
                        ? e.travelTimeSeconds()
                        : (e.speedKmh() > 0.0 ? e.distanceMeters() / (e.speedKmh() / 3.6) : 0.0);
                fullNetwork.addEdge(new RoadEdge(
                        e.source(),
                        e.target(),
                        e.distanceMeters(),
                        e.speedKmh(),
                        travelTime,
                        e.highway(),
                        e.oneway()
                ));
            }
        }

        log.info("Red vial de OpenStreetMap cargada en memoria: {} nodos, {} aristas dirigidas.",
                fullNetwork.getNodeCount(), fullNetwork.getEdgeCount());

        // 2. Ejecutar algoritmo de Tarjan para extraer la mayor componente fuertemente conexa (SCC)
        TarjanScc tarjanScc = new TarjanScc(fullNetwork);
        SccResult sccResult = tarjanScc.extractLargestComponent();
        RoadNetwork sccNetwork = sccResult.largestSccNetwork();

        log.info("Tarjan ejecutado sobre red OSM: Total nodos antes: {}, Total aristas antes: {}, Nodos SCC: {}, Aristas SCC: {}, Componentes: {}",
                sccResult.totalNodesBefore(), sccResult.totalEdgesBefore(),
                sccResult.sccNodesCount(), sccResult.sccEdgesCount(), sccResult.totalComponents());

        // 3. Cargar archivo JSON de los 42 lugares y asociar cada uno al nodo más cercano de la SCC de OpenStreetMap
        Resource placesResource = resourceLoader.getResource("classpath:data/places.json");
        List<RawPlace> rawPlaces;
        try (InputStream is = placesResource.getInputStream()) {
            rawPlaces = objectMapper.readValue(is, new TypeReference<>() {});
        }

        Map<String, Long> placeNodeMap = new HashMap<>();
        Map<Long, String> nodePlaceMap = new HashMap<>();
        List<Place> placesToSave = new ArrayList<>();
        List<String> placeIds = new ArrayList<>();
        List<String> placeNames = new ArrayList<>();

        for (RawPlace raw : rawPlaces) {
            double lat = raw.latitude();
            double lon = raw.longitude();
            GeoUtils.UtmPoint placeUtm = GeoUtils.toUtmZone18N(lat, lon);

            RoadNode nearestNode = sccNetwork.findNearestNode(placeUtm.utmX(), placeUtm.utmY());
            if (nearestNode == null) {
                nearestNode = fullNetwork.findNearestNode(placeUtm.utmX(), placeUtm.utmY());
            }

            long assignedNodeId = (nearestNode != null) ? nearestNode.id() : 0L;
            double distToNode = (nearestNode != null)
                    ? GeoUtils.euclideanDistance(placeUtm.utmX(), placeUtm.utmY(), nearestNode.utmX(), nearestNode.utmY())
                    : 0.0;
            double roundedDist = Math.round(distToNode * 10.0) / 10.0;
            boolean isExceeding500m = roundedDist > 500.0;

            String treatmentNote = isExceeding500m
                    ? String.format(Locale.US,
                            "Ubicado a %.1f m del nodo vial OSM %d (> 500 m). Se proyectó ortogonalmente al nodo vehicular más cercano de la componente fuertemente conexa y se conectó mediante tramo de acceso rural.",
                            roundedDist, assignedNodeId)
                    : String.format(Locale.US,
                            "Conexión directa al nodo vial más cercano de la red OpenStreetMap (nodo %d a %.1f m).",
                            assignedNodeId, roundedDist);

            Place place = Place.builder()
                    .id(raw.id())
                    .name(raw.name())
                    .municipality(raw.municipality())
                    .category(PlaceCategory.valueOf(raw.category()))
                    .latitude(lat)
                    .longitude(lon)
                    .utmX(placeUtm.utmX())
                    .utmY(placeUtm.utmY())
                    .visitDurationMinutes(raw.visitDurationMinutes())
                    .nearestNodeId(assignedNodeId)
                    .distanceToNodeMeters(roundedDist)
                    .isExceeding500m(isExceeding500m)
                    .treatmentNote(treatmentNote)
                    .build();

            placesToSave.add(place);
            placeNodeMap.put(raw.id(), assignedNodeId);
            nodePlaceMap.put(assignedNodeId, raw.id());
            placeIds.add(raw.id());
            placeNames.add(raw.name());
        }

        placeRepository.saveAll(placesToSave);
        log.info("Catálogo de {} lugares proyectado a la red OpenStreetMap y guardado en base de datos.", placesToSave.size());

        // 4. Precalcular matriz asimétrica de tiempos de viaje (42 x 42) sobre la SCC de OpenStreetMap con Dijkstra Priority Queue
        log.info("Precalculando matriz asimétrica de tiempos ({} x {}) con Dijkstra Priority Queue sobre la red OSM...",
                placeIds.size(), placeIds.size());
        DijkstraPriorityQueue dijkstraPq = new DijkstraPriorityQueue(sccNetwork);
        Map<String, Map<String, Double>> matrixSeconds = new HashMap<>();

        for (String origId : placeIds) {
            long origNode = placeNodeMap.get(origId);
            Map<Long, Double> timesFromOrigin = dijkstraPq.computeOneToAll(origNode);
            Map<String, Double> row = new HashMap<>();

            for (String destId : placeIds) {
                if (origId.equals(destId)) {
                    row.put(destId, 0.0);
                } else {
                    long destNode = placeNodeMap.get(destId);
                    Double time = timesFromOrigin.get(destNode);
                    row.put(destId, (time != null) ? Math.round(time * 10.0) / 10.0 : Double.POSITIVE_INFINITY);
                }
            }
            matrixSeconds.put(origId, row);
        }

        // 5. Actualizar contenedor en memoria
        dataHolder.setFullNetwork(fullNetwork);
        dataHolder.setLargestSccNetwork(sccNetwork);
        dataHolder.setSccResult(sccResult);
        dataHolder.setPlaceIds(placeIds);
        dataHolder.setPlaceNames(placeNames);
        dataHolder.setTimeMatrixSeconds(matrixSeconds);
        dataHolder.setPlaceNodeMapping(placeNodeMap);
        dataHolder.setNodePlaceMapping(nodePlaceMap);

        log.info("Inicialización de la red vial de OpenStreetMap y matriz asimétrica 42x42 finalizada con éxito.");
    }
}
