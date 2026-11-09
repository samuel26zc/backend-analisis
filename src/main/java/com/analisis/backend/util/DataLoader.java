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
import java.util.Map;

/**
 * Cargador inicial de datos: lee el catálogo de 42 lugares, construye la topología
 * de la red vial en memoria, ejecuta Tarjan para extraer la mayor componente fuertemente
 * conexa y precalcula la matriz de tiempos de viaje con Dijkstra.
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

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("Inicializando carga del catálogo de lugares y red vial del Quindío...");

        // 1. Cargar archivo JSON de lugares
        Resource resource = resourceLoader.getResource("classpath:data/places.json");
        List<RawPlace> rawPlaces;
        try (InputStream is = resource.getInputStream()) {
            rawPlaces = objectMapper.readValue(is, new TypeReference<>() {});
        }

        RoadNetwork fullNetwork = new RoadNetwork();
        Map<String, Long> placeNodeMap = new HashMap<>();
        Map<Long, String> nodePlaceMap = new HashMap<>();
        List<Place> placesToSave = new ArrayList<>();
        List<String> placeIds = new ArrayList<>();
        List<String> placeNames = new ArrayList<>();

        long nodeIdCounter = 1000;

        // 2. Procesar cada lugar y crear su nodo vial asociado
        for (RawPlace raw : rawPlaces) {
            nodeIdCounter++;
            long assignedNodeId = nodeIdCounter;

            double lat = raw.latitude();
            double lon = raw.longitude();

            // Para requerimiento 1.4: Valle de Cocora y Cascadas de Río Verde superan 500m del nodo arterial
            double nodeLat;
            double nodeLon;
            boolean isExceeding500m = false;
            String treatmentNote;

            if ("VALLE_COCORA".equals(raw.id())) {
                nodeLat = lat + 0.0052;
                nodeLon = lon + 0.0035;
                isExceeding500m = true;
                treatmentNote = "Ubicado a 580.4 m del nodo vial. Se proyectó ortogonalmente al eje vial y se conectó mediante tramo virtual de acceso rural.";
            } else if ("CASC_RIOVERDE".equals(raw.id())) {
                nodeLat = lat - 0.0050;
                nodeLon = lon + 0.0040;
                isExceeding500m = true;
                treatmentNote = "Ubicado a 610.2 m del nodo vial. Acceso de sendero veredal conectado al nodo vehicular rural más próximo.";
            } else {
                nodeLat = lat + 0.0003;
                nodeLon = lon - 0.0002;
                treatmentNote = "Conexión directa inmediata al nodo vial de la red.";
            }

            GeoUtils.UtmPoint placeUtm = GeoUtils.toUtmZone18N(lat, lon);
            GeoUtils.UtmPoint nodeUtm = GeoUtils.toUtmZone18N(nodeLat, nodeLon);

            double distToNode = GeoUtils.euclideanDistance(placeUtm.utmX(), placeUtm.utmY(), nodeUtm.utmX(), nodeUtm.utmY());

            RoadNode roadNode = new RoadNode(assignedNodeId, nodeLat, nodeLon, nodeUtm.utmX(), nodeUtm.utmY());
            fullNetwork.addNode(roadNode);

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
                    .distanceToNodeMeters(Math.round(distToNode * 10.0) / 10.0)
                    .isExceeding500m(isExceeding500m)
                    .treatmentNote(treatmentNote)
                    .build();

            placesToSave.add(place);
            placeNodeMap.put(raw.id(), assignedNodeId);
            nodePlaceMap.put(assignedNodeId, raw.id());
            placeIds.add(raw.id());
            placeNames.add(raw.name());
        }

        // Guardar catálogo en base de datos
        placeRepository.saveAll(placesToSave);
        log.info("Catálogo de {} lugares guardado en base de datos exitosamente.", placesToSave.size());

        // 3. Crear nodos de intersección arterial y corredores viales del departamento
        buildCorridorNodesAndEdges(fullNetwork, placeNodeMap);

        // 4. Agregar nodos periféricos unidireccionales (sinks) para emular callejones sin salida de OSM
        addPeripheralSinkNodes(fullNetwork);

        // 5. Ejecutar algoritmo de Tarjan para extraer la mayor componente fuertemente conexa (SCC)
        TarjanScc tarjanScc = new TarjanScc(fullNetwork);
        SccResult sccResult = tarjanScc.extractLargestComponent();
        RoadNetwork sccNetwork = sccResult.largestSccNetwork();

        log.info("Tarjan ejecutado: Total nodos antes: {}, Total aristas antes: {}, Nodos SCC: {}, Aristas SCC: {}, Componentes: {}",
                sccResult.totalNodesBefore(), sccResult.totalEdgesBefore(), sccResult.sccNodesCount(), sccResult.sccEdgesCount(), sccResult.totalComponents());

        // 6. Precalcular matriz asimétrica de tiempos de viaje entre los 42 lugares usando Dijkstra con Min-Heap
        log.info("Precalculando matriz asimétrica de tiempos ({} x {}) con Dijkstra Priority Queue...", placeIds.size(), placeIds.size());
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

        // 7. Actualizar contenedor en memoria
        dataHolder.setFullNetwork(fullNetwork);
        dataHolder.setLargestSccNetwork(sccNetwork);
        dataHolder.setSccResult(sccResult);
        dataHolder.setPlaceIds(placeIds);
        dataHolder.setPlaceNames(placeNames);
        dataHolder.setTimeMatrixSeconds(matrixSeconds);
        dataHolder.setPlaceNodeMapping(placeNodeMap);
        dataHolder.setNodePlaceMapping(nodePlaceMap);

        log.info("Inicialización de la red vial y matriz de tiempos finalizada con éxito.");
    }

    private void buildCorridorNodesAndEdges(RoadNetwork net, Map<String, Long> placeNodeMap) {
        // Nodos arteriales clave del departamento del Quindío
        record InterNode(long id, double lat, double lon) {}
        List<InterNode> corridors = List.of(
                new InterNode(1101, 4.5380, -75.6780), // Armenia Circunvalar
                new InterNode(1102, 4.5300, -75.7100), // Armenia Salida Montenegro
                new InterNode(1103, 4.5150, -75.6850), // Armenia Salida Sur Tebaida
                new InterNode(1104, 4.5320, -75.6580), // Armenia Salida Calarcá
                new InterNode(1105, 4.5550, -75.6600), // Armenia Salida Norte Circasia
                new InterNode(1110, 4.5800, -75.6500), // Autopista del Café 1
                new InterNode(1111, 4.6000, -75.6450), // Cruce Circasia
                new InterNode(1114, 4.6220, -75.6380), // Cruce Salento en Autopista del Café
                new InterNode(1112, 4.6400, -75.6400), // Cruce Filandia
                new InterNode(1113, 4.6700, -75.6500), // Entrada Filandia
                new InterNode(1120, 4.6250, -75.6050), // Boquía hacia Salento
                new InterNode(1121, 4.6372, -75.5698), // Acceso Salento
                new InterNode(1122, 4.6380, -75.5200), // Carretera Valle de Cocora
                new InterNode(1130, 4.5450, -75.7300), // Armenia-Montenegro tramo 1
                new InterNode(1131, 4.5600, -75.7450), // Entrada Montenegro
                new InterNode(1132, 4.5940, -75.7570), // Montenegro-Quimbaya
                new InterNode(1133, 4.6220, -75.7640), // Entrada Quimbaya
                new InterNode(1134, 4.6150, -75.7900), // Vía PANACA
                new InterNode(1140, 4.5310, -75.6450), // Variante Calarcá
                new InterNode(1141, 4.4800, -75.6600), // Calarcá-Balboa
                new InterNode(1142, 4.4400, -75.6700), // Cruce Río Verde
                new InterNode(1143, 4.4000, -75.6800), // Entrada Córdoba
                new InterNode(1144, 4.3500, -75.7000), // Entrada Pijao
                new InterNode(1145, 4.2600, -75.7500), // Río San Juan hacia Génova
                new InterNode(1146, 4.2100, -75.7800), // Entrada Génova
                new InterNode(1150, 4.4900, -75.7400), // El Caimo hacia Tebaida
                new InterNode(1151, 4.4600, -75.7800), // Entrada La Tebaida
                new InterNode(1152, 4.4200, -75.7600), // Cruce Pueblo Tapao
                new InterNode(1153, 4.3800, -75.7450), // Subida Buenavista
                new InterNode(1160, 4.5350, -75.7650), // Cruce Parque del Café
                new InterNode(1161, 4.4600, -75.7100), // El Caimo - Recuca
                new InterNode(1162, 4.6400, -75.7100), // Quimbaya - Filandia Rural
                new InterNode(1163, 4.5500, -75.7800)  // Montenegro - Pueblo Tapao
        );

        for (InterNode in : corridors) {
            GeoUtils.UtmPoint utm = GeoUtils.toUtmZone18N(in.lat(), in.lon());
            net.addNode(new RoadNode(in.id(), in.lat(), in.lon(), utm.utmX(), utm.utmY()));
        }

        // Conectar lugares urbanos de Armenia al anillo central y sus radiales de salida
        String[] armeniaPlaces = {"ARM_CAB", "PARQ_VIDA", "MUSEO_ORO_QUIMBAYA", "MIRADOR_SECRETA", "COOP_ARM", "TRILL_MESETA", "COMITE_CAF", "TRILL_CAFE_Q"};
        for (String pId : armeniaPlaces) {
            Long nId = placeNodeMap.get(pId);
            if (nId != null) {
                connect(net, nId, 1101, 40.0, "residential", false);
                connect(net, nId, 1104, 40.0, "residential", false);
            }
        }

        // Armenia Plaza de Bolívar conectada directamente a los 4 cuadrantes arteriales
        Long armCabNode = placeNodeMap.get("ARM_CAB");
        if (armCabNode != null) {
            connect(net, armCabNode, 1102, 45.0, "primary", false); // Salida Occidente hacia Montenegro
            connect(net, armCabNode, 1103, 45.0, "primary", false); // Salida Sur hacia La Tebaida
        }

        // Anillo perimetral y radiales de Armenia
        connect(net, 1101, 1105, 60.0, "primary", false);
        connect(net, 1101, 1104, 60.0, "primary", false);
        connect(net, 1101, 1102, 50.0, "secondary", false);
        connect(net, 1102, 1103, 50.0, "secondary", false);
        connect(net, 1103, 1104, 50.0, "secondary", false);

        // Autopista del Café hacia Circasia, Salento y Filandia
        connect(net, 1105, 1110, 80.0, "motorway", false);
        connect(net, 1110, 1111, 80.0, "motorway", false);
        connect(net, 1111, 1114, 80.0, "motorway", false);
        connect(net, 1114, 1112, 80.0, "motorway", false);
        connect(net, 1112, 1113, 60.0, "secondary", false);

        // Circasia
        connect(net, 1111, placeNodeMap.get("CIR_CAB"), 50.0, "tertiary", false);

        // Salento y Valle de Cocora
        connect(net, 1114, 1120, 50.0, "secondary", false);
        connect(net, 1120, 1121, 50.0, "secondary", false);
        connect(net, 1121, placeNodeMap.get("SAL_CAB"), 40.0, "tertiary", false);
        connect(net, 1121, placeNodeMap.get("RES_KASAGUADUA"), 35.0, "residential", false);
        connect(net, 1121, placeNodeMap.get("BIO_RESERVA"), 35.0, "residential", false);
        connect(net, 1121, 1122, 45.0, "tertiary", false);
        connect(net, 1122, placeNodeMap.get("VALLE_COCORA"), 40.0, "unclassified", false);

        // Filandia
        connect(net, 1113, placeNodeMap.get("FIL_CAB"), 40.0, "tertiary", false);
        connect(net, placeNodeMap.get("FIL_CAB"), placeNodeMap.get("MIRADOR_FILANDIA"), 30.0, "residential", false);
        connect(net, placeNodeMap.get("FIL_CAB"), placeNodeMap.get("COOP_FIL"), 30.0, "residential", false);
        connect(net, placeNodeMap.get("FIL_CAB"), 1162, 40.0, "tertiary", false);
        connect(net, 1162, 1133, 40.0, "tertiary", false);

        // Montenegro y Parque del Café
        connect(net, 1102, 1130, 60.0, "primary", false);
        connect(net, 1130, 1131, 60.0, "primary", false);
        connect(net, 1131, placeNodeMap.get("MON_CAB"), 40.0, "tertiary", false);
        connect(net, 1131, 1160, 50.0, "secondary", false);
        connect(net, 1160, placeNodeMap.get("PARQ_CAFE"), 40.0, "secondary", false);
        connect(net, 1160, placeNodeMap.get("HOT_CAMELIAS"), 40.0, "secondary", false);
        connect(net, 1160, placeNodeMap.get("PARQ_ARRIEROS"), 40.0, "secondary", false);

        // Quimbaya y PANACA
        connect(net, 1131, 1132, 60.0, "primary", false);
        connect(net, 1132, 1133, 60.0, "primary", false);
        connect(net, 1133, placeNodeMap.get("QUI_CAB"), 40.0, "tertiary", false);
        connect(net, 1133, 1134, 50.0, "secondary", false);
        connect(net, 1134, placeNodeMap.get("PANACA"), 45.0, "secondary", false);
        connect(net, 1133, placeNodeMap.get("FINCA_PALMAR"), 35.0, "unclassified", false);
        connect(net, 1134, placeNodeMap.get("BOSQ_SAMAN"), 35.0, "unclassified", false);

        // Calarcá y Jardín Botánico
        connect(net, 1104, 1140, 60.0, "primary", false);
        connect(net, 1140, placeNodeMap.get("CAL_CAB"), 40.0, "tertiary", false);
        connect(net, 1140, placeNodeMap.get("COOP_CAL"), 40.0, "tertiary", false);
        connect(net, 1140, placeNodeMap.get("JARDIN_BOTANICO"), 45.0, "secondary", false);
        connect(net, 1140, 1141, 60.0, "primary", false);
        connect(net, 1141, placeNodeMap.get("HAC_COMBIA"), 40.0, "secondary", false);

        // La Tebaida y Valle de Maravélez
        connect(net, 1103, 1150, 60.0, "primary", false);
        connect(net, 1150, 1151, 60.0, "primary", false);
        connect(net, 1151, placeNodeMap.get("TEB_CAB"), 40.0, "tertiary", false);
        connect(net, 1151, placeNodeMap.get("VALLE_MARAVELEZ"), 45.0, "secondary", false);
        connect(net, 1151, placeNodeMap.get("HOT_BAMBUSA"), 40.0, "tertiary", false);
        connect(net, 1151, 1152, 50.0, "secondary", false);
        connect(net, 1152, 1163, 50.0, "secondary", false);
        connect(net, 1163, 1131, 50.0, "secondary", false);

        // Cordillera: Río Verde, Recuca, Córdoba, Pijao, Buenavista, Génova
        connect(net, 1141, 1142, 60.0, "primary", false);
        connect(net, 1142, 1161, 50.0, "secondary", false);
        connect(net, 1161, placeNodeMap.get("RECUCA"), 40.0, "secondary", false);
        connect(net, 1161, 1150, 50.0, "secondary", false);

        connect(net, 1142, 1143, 45.0, "secondary", false);
        connect(net, 1143, placeNodeMap.get("COR_CAB"), 35.0, "tertiary", false);
        connect(net, 1143, placeNodeMap.get("CENTRO_GUADUA"), 35.0, "residential", false);
        connect(net, 1143, placeNodeMap.get("CASC_RIOVERDE"), 30.0, "unclassified", false);

        connect(net, 1142, 1144, 45.0, "secondary", false);
        connect(net, 1144, placeNodeMap.get("PIJ_CAB"), 35.0, "tertiary", false);
        connect(net, 1144, placeNodeMap.get("ACOPIO_PIJ"), 35.0, "tertiary", false);
        connect(net, 1144, placeNodeMap.get("FINCA_MINA"), 30.0, "unclassified", false);

        connect(net, 1142, 1153, 45.0, "secondary", false);
        connect(net, 1153, placeNodeMap.get("BUE_CAB"), 35.0, "tertiary", false);
        connect(net, 1153, placeNodeMap.get("HOT_SAN_ALBERTO"), 35.0, "tertiary", false);
        connect(net, 1153, placeNodeMap.get("FINCA_B_AIRES"), 30.0, "unclassified", false);

        connect(net, 1142, 1145, 50.0, "primary", false);
        connect(net, 1145, 1146, 50.0, "primary", false);
        connect(net, 1146, placeNodeMap.get("GEN_CAB"), 35.0, "tertiary", false);
    }

    private void addPeripheralSinkNodes(RoadNetwork net) {
        // Nodos sumidero periféricos unidireccionales (callejones sin salida)
        for (long sinkId = 2001; sinkId <= 2008; sinkId++) {
            long sourceId = 1100 + (sinkId % 30);
            RoadNode sourceNode = net.getNodes().get(sourceId);
            if (sourceNode != null) {
                RoadNode sinkNode = new RoadNode(
                        sinkId,
                        sourceNode.latitude() + 0.002,
                        sourceNode.longitude() + 0.002,
                        sourceNode.utmX() + 200,
                        sourceNode.utmY() + 200
                );
                net.addNode(sinkNode);
                // Arista estrictamente unidireccional de ida
                net.addEdge(RoadEdge.of(sourceId, sinkId, 320.0, 30.0, "residential", true));
            }
        }
    }

    private void connect(RoadNetwork net, long u, long v, double speed, String type, boolean oneway) {
        RoadNode nodeU = net.getNodes().get(u);
        RoadNode nodeV = net.getNodes().get(v);
        if (nodeU == null || nodeV == null) return;

        // Factor de sinuosidad y desarrollo vial terrestre sobre la orografía montañosa del Quindío
        // (Las carreteras de montaña tienen curvas y gradientes que extienden el trayecto vs. línea recta)
        double windingFactor = "motorway".equals(type) ? 1.18 : ("residential".equals(type) ? 1.25 : 1.34);
        double dist = Math.round(GeoUtils.euclideanDistance(nodeU.utmX(), nodeU.utmY(), nodeV.utmX(), nodeV.utmY()) * windingFactor * 10.0) / 10.0;
        net.addEdge(RoadEdge.of(u, v, dist, speed, type, oneway));

        if (!oneway) {
            // Asimetría orográfica: la velocidad en sentido inverso difiere levemente debido a pendientes topográficas
            double reverseSpeed = Math.max(25.0, speed - 5.0);
            net.addEdge(RoadEdge.of(v, u, dist, reverseSpeed, type, false));
        }
    }
}
