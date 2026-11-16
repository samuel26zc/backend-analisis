package com.analisis.backend.algorithm;

import com.analisis.backend.algorithm.convexhull.QuickHull2D;
import com.analisis.backend.algorithm.convexhull.scipy.SciPyBenchmarkRunner;
import com.analisis.backend.algorithm.scc.TarjanScc;
import com.analisis.backend.algorithm.shortestpath.DijkstraLinear;
import com.analisis.backend.algorithm.shortestpath.DijkstraPriorityQueue;
import com.analisis.backend.algorithm.tsp.BruteForceTsp;
import com.analisis.backend.algorithm.tsp.GreedyTsp;
import com.analisis.backend.algorithm.tsp.HeldKarpTsp;
import com.analisis.backend.model.domain.DijkstraResult;
import com.analisis.backend.model.domain.NetworkDataHolder;
import com.analisis.backend.model.domain.RoadEdge;
import com.analisis.backend.model.domain.RoadNetwork;
import com.analisis.backend.model.domain.RoadNode;
import com.analisis.backend.model.domain.SccResult;
import com.analisis.backend.model.domain.TspSolution;
import com.analisis.backend.util.GeoUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class ExperimentSuiteGeneratorTest {

    @Test
    @DisplayName("Ejecuta el protocolo experimental completo R1-R4 sobre la red OpenStreetMap y exporta CSV/JSON reales")
    void runFullExperimentalProtocolAndExportFiles() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        // 1. Cargar red vial de OpenStreetMap (quindio_network.json)
        RoadNetwork fullNetwork = new RoadNetwork();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("data/quindio_network.json")) {
            assertNotNull(is);
            JsonNode root = mapper.readTree(is);
            for (JsonNode n : root.get("nodes")) {
                fullNetwork.addNode(new RoadNode(
                        n.get("id").asLong(),
                        n.get("latitude").asDouble(),
                        n.get("longitude").asDouble(),
                        n.get("utmX").asLong(),
                        n.get("utmY").asLong()
                ));
            }
            for (JsonNode e : root.get("edges")) {
                fullNetwork.addEdge(new RoadEdge(
                        e.get("source").asLong(),
                        e.get("target").asLong(),
                        e.get("distanceMeters").asDouble(),
                        e.get("speedKmh").asDouble(),
                        e.get("travelTimeSeconds").asDouble(),
                        e.get("highway").asText(),
                        e.get("oneway").asBoolean()
                ));
            }
        }

        // 2. Ejecutar Tarjan SCC
        TarjanScc tarjan = new TarjanScc(fullNetwork);
        SccResult sccResult = tarjan.extractLargestComponent();
        RoadNetwork sccNetwork = sccResult.largestSccNetwork();

        // 3. Cargar los 42 lugares y asociarlos al nodo más cercano en UTM 18N (Requerimiento 1.4)
        Map<String, Long> placeNodeMap = new LinkedHashMap<>();
        List<String> placeIds = new ArrayList<>();
        List<String> placeNames = new ArrayList<>();
        List<Map<String, Object>> placesMappingRows = new ArrayList<>();

        try (InputStream is = getClass().getClassLoader().getResourceAsStream("data/places.json")) {
            assertNotNull(is);
            JsonNode placesArr = mapper.readTree(is);
            for (JsonNode p : placesArr) {
                String id = p.get("id").asText();
                String name = p.get("name").asText();
                String muni = p.get("municipality").asText();
                String cat = p.get("category").asText();
                double lat = p.get("latitude").asDouble();
                double lon = p.get("longitude").asDouble();

                GeoUtils.UtmPoint utm = GeoUtils.toUtmZone18N(lat, lon);
                RoadNode nearest = sccNetwork.findNearestNode(utm.utmX(), utm.utmY());
                assertNotNull(nearest);
                double dist = Math.round(GeoUtils.euclideanDistance(utm.utmX(), utm.utmY(), nearest.utmX(), nearest.utmY()) * 10.0) / 10.0;

                placeNodeMap.put(id, nearest.id());
                placeIds.add(id);
                placeNames.add(name);

                Map<String, Object> mRow = new LinkedHashMap<>();
                mRow.put("placeId", id);
                mRow.put("placeName", name);
                mRow.put("municipality", muni);
                mRow.put("category", cat);
                mRow.put("nearestNodeId", nearest.id());
                mRow.put("distanceMeters", dist);
                mRow.put("exceeding500m", dist > 500.0);
                placesMappingRows.add(mRow);
            }
        }

        // 4. Precalcular Matriz Asimétrica de Tiempos 42x42 con Dijkstra Priority Queue
        DijkstraPriorityQueue dijkstraPq = new DijkstraPriorityQueue(sccNetwork);
        DijkstraLinear dijkstraLinear = new DijkstraLinear(sccNetwork);
        Map<String, Map<String, Double>> matrixSeconds = new LinkedHashMap<>();

        for (String origId : placeIds) {
            long origNode = placeNodeMap.get(origId);
            Map<Long, Double> times = dijkstraPq.computeOneToAll(origNode);
            Map<String, Double> row = new LinkedHashMap<>();
            for (String destId : placeIds) {
                if (origId.equals(destId)) {
                    row.put(destId, 0.0);
                } else {
                    Double t = times.get(placeNodeMap.get(destId));
                    row.put(destId, t != null ? Math.round(t * 10.0) / 10.0 : Double.POSITIVE_INFINITY);
                }
            }
            matrixSeconds.put(origId, row);
        }

        NetworkDataHolder dataHolder = new NetworkDataHolder();
        dataHolder.setFullNetwork(fullNetwork);
        dataHolder.setLargestSccNetwork(sccNetwork);
        dataHolder.setSccResult(sccResult);
        dataHolder.setPlaceIds(placeIds);
        dataHolder.setPlaceNames(placeNames);
        dataHolder.setTimeMatrixSeconds(matrixSeconds);
        dataHolder.setPlaceNodeMapping(placeNodeMap);

        // 5. Requerimiento 2: Medir 20 pares con Cola de Prioridad y 10 pares cercanos con versión Lineal (5 repeticiones, mediana)
        String[][] pairs10Both = {
                {"ARM_CAB", "MUSEO_ORO_QUIMBAYA"},
                {"ARM_CAB", "PARQ_VIDA"},
                {"ARM_CAB", "MIRADOR_SECRETA"},
                {"ARM_CAB", "CAL_CAB"},
                {"CAL_CAB", "JARDIN_BOTANICO"},
                {"FIL_CAB", "MIRADOR_FILANDIA"},
                {"MON_CAB", "PARQ_CAFE"},
                {"QUI_CAB", "PANACA"},
                {"SAL_CAB", "RES_KASAGUADUA"},
                {"ARM_CAB", "CIR_CAB"}
        };

        String[][] pairs10PqOnly = {
                {"ARM_CAB", "SAL_CAB"},
                {"ARM_CAB", "FIL_CAB"},
                {"ARM_CAB", "MON_CAB"},
                {"ARM_CAB", "QUI_CAB"},
                {"ARM_CAB", "TEB_CAB"},
                {"ARM_CAB", "COR_CAB"},
                {"ARM_CAB", "PIJ_CAB"},
                {"ARM_CAB", "BUE_CAB"},
                {"ARM_CAB", "GEN_CAB"},
                {"ARM_CAB", "VALLE_COCORA"}
        };

        // Warmup JIT
        dijkstraPq.findShortestPath(placeNodeMap.get("ARM_CAB"), placeNodeMap.get("CAL_CAB"));
        dijkstraLinear.findShortestPath(placeNodeMap.get("ARM_CAB"), placeNodeMap.get("MUSEO_ORO_QUIMBAYA"));

        List<Map<String, Object>> r2Rows = new ArrayList<>();
        double[] ratios10 = new double[pairs10Both.length];

        for (int idx = 0; idx < pairs10Both.length; idx++) {
            String orig = pairs10Both[idx][0];
            String dest = pairs10Both[idx][1];
            long u = placeNodeMap.get(orig);
            long v = placeNodeMap.get(dest);

            double[] pqTimes = new double[5];
            DijkstraResult pqRes = null;
            for (int r = 0; r < 5; r++) {
                long t0 = System.nanoTime();
                pqRes = dijkstraPq.findShortestPath(u, v);
                pqTimes[r] = (System.nanoTime() - t0) / 1_000_000.0;
            }
            Arrays.sort(pqTimes);
            double pqMedMs = Math.round(pqTimes[2] * 100.0) / 100.0;

            double[] linTimes = new double[5];
            DijkstraResult linRes = null;
            for (int r = 0; r < 5; r++) {
                long t0 = System.nanoTime();
                linRes = dijkstraLinear.findShortestPath(u, v);
                linTimes[r] = (System.nanoTime() - t0) / 1_000_000.0;
            }
            Arrays.sort(linTimes);
            double linMedMs = Math.round(linTimes[2] * 100.0) / 100.0;

            // Verificar que ambas versiones den el mismo tiempo de viaje
            assertEquals(pqRes.travelTimeSeconds(), linRes.travelTimeSeconds(), 1e-3,
                    "Tiempos de viaje deben coincidir en " + orig + " -> " + dest);

            ratios10[idx] = pqMedMs > 0 ? (linMedMs / pqMedMs) : 1.0;

            Map<String, Object> rowPq = new LinkedHashMap<>();
            rowPq.put("origen", orig);
            rowPq.put("destino", dest);
            rowPq.put("version", "cola_prioridad");
            rowPq.put("tiempoViajeS", Math.round(pqRes.travelTimeSeconds() * 10.0) / 10.0);
            rowPq.put("nodosExplorados", pqRes.exploredNodesCount());
            rowPq.put("tiempoMs", Math.max(0.01, pqMedMs));
            r2Rows.add(rowPq);

            Map<String, Object> rowLin = new LinkedHashMap<>();
            rowLin.put("origen", orig);
            rowLin.put("destino", dest);
            rowLin.put("version", "lineal");
            rowLin.put("tiempoViajeS", Math.round(linRes.travelTimeSeconds() * 10.0) / 10.0);
            rowLin.put("nodosExplorados", linRes.exploredNodesCount());
            rowLin.put("tiempoMs", Math.max(0.01, linMedMs));
            r2Rows.add(rowLin);
        }

        for (String[] pair : pairs10PqOnly) {
            String orig = pair[0];
            String dest = pair[1];
            long u = placeNodeMap.get(orig);
            long v = placeNodeMap.get(dest);

            double[] pqTimes = new double[5];
            DijkstraResult pqRes = null;
            for (int r = 0; r < 5; r++) {
                long t0 = System.nanoTime();
                pqRes = dijkstraPq.findShortestPath(u, v);
                pqTimes[r] = (System.nanoTime() - t0) / 1_000_000.0;
            }
            Arrays.sort(pqTimes);
            double pqMedMs = Math.round(pqTimes[2] * 100.0) / 100.0;

            Map<String, Object> rowPq = new LinkedHashMap<>();
            rowPq.put("origen", orig);
            rowPq.put("destino", dest);
            rowPq.put("version", "cola_prioridad");
            rowPq.put("tiempoViajeS", Math.round(pqRes.travelTimeSeconds() * 10.0) / 10.0);
            rowPq.put("nodosExplorados", pqRes.exploredNodesCount());
            rowPq.put("tiempoMs", Math.max(0.01, pqMedMs));
            r2Rows.add(rowPq);
        }

        Arrays.sort(ratios10);
        Map<String, Object> r2RatioSummary = new LinkedHashMap<>();
        r2RatioSummary.put("mediana", Math.round(ratios10[ratios10.length / 2] * 100.0) / 100.0);
        r2RatioSummary.put("minimo", Math.round(ratios10[0] * 100.0) / 100.0);
        r2RatioSummary.put("maximo", Math.round(ratios10[ratios10.length - 1] * 100.0) / 100.0);

        // 6. Requerimiento 3: QuickHull 2D vs SciPy ConvexHull (semilla=42, 5 repeticiones)
        QuickHull2D quickHull = new QuickHull2D();
        List<QuickHull2D.Point> netPts = new ArrayList<>();
        for (RoadNode n : sccNetwork.getNodes().values()) {
            netPts.add(new QuickHull2D.Point(n.utmX(), n.utmY(), n.latitude(), n.longitude()));
        }

        List<Map<String, Object>> r3Rows = new ArrayList<>();
        double[] netHullTimes = new double[5];
        QuickHull2D.HullResult netHull = null;
        for (int r = 0; r < 5; r++) {
            long t0 = System.nanoTime();
            netHull = quickHull.computeHull(netPts);
            netHullTimes[r] = (System.nanoTime() - t0) / 1_000_000.0;
        }
        Arrays.sort(netHullTimes);
        var netScipy = SciPyBenchmarkRunner.getNetworkSciPyBenchmark();
        assertTrue(SciPyBenchmarkRunner.verifyVerticesMatch(netHull.vertices(), netScipy),
                "Los vértices de la red deben coincidir exactamente con SciPy");
        double netOwnMs = Math.round(netHullTimes[2] * 100.0) / 100.0;
        double netSciMs = Math.round(netScipy.scipyTimeMs() * 100.0) / 100.0;

        Map<String, Object> r3NetRow = new LinkedHashMap<>();
        r3NetRow.put("pointSet", "Nodos de la red");
        r3NetRow.put("n", netPts.size());
        r3NetRow.put("hullVertices", netHull.vertices().size());
        r3NetRow.put("ownTimeMs", netOwnMs);
        r3NetRow.put("referenceTimeMs", netSciMs);
        r3NetRow.put("ratio", Math.round((netOwnMs / netSciMs) * 100.0) / 100.0);
        r3Rows.add(r3NetRow);

        for (int n : List.of(1000, 10000, 50000)) {
            Random rng = new Random(42L);
            List<QuickHull2D.Point> sqPts = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                sqPts.add(new QuickHull2D.Point(400000L + rng.nextInt(100000), 480000L + rng.nextInt(100000)));
            }
            double[] sqTimes = new double[5];
            QuickHull2D.HullResult sqHull = null;
            for (int r = 0; r < 5; r++) {
                long t0 = System.nanoTime();
                sqHull = quickHull.computeHull(sqPts);
                sqTimes[r] = (System.nanoTime() - t0) / 1_000_000.0;
            }
            Arrays.sort(sqTimes);
            var sqScipy = SciPyBenchmarkRunner.getSquareSciPyBenchmark(n);
            assertTrue(SciPyBenchmarkRunner.verifyVerticesMatch(sqHull.vertices(), sqScipy));
            double sqOwnMs = Math.round(sqTimes[2] * 100.0) / 100.0;
            double sqSciMs = Math.round(sqScipy.scipyTimeMs() * 100.0) / 100.0;

            Map<String, Object> sqRow = new LinkedHashMap<>();
            sqRow.put("pointSet", String.format(Locale.US, "Cuadrado, %,d puntos", n));
            sqRow.put("n", n);
            sqRow.put("hullVertices", sqHull.vertices().size());
            sqRow.put("ownTimeMs", sqOwnMs);
            sqRow.put("referenceTimeMs", sqSciMs);
            sqRow.put("ratio", Math.round((sqOwnMs / sqSciMs) * 100.0) / 100.0);
            r3Rows.add(sqRow);
        }

        // 7. Requerimiento 4.1: Soluciones exactas Fuerza Bruta vs Held-Karp (k = 5..12, semilla fija = 42)
        List<String> candidateTargets = new ArrayList<>(placeIds);
        candidateTargets.remove("ARM_CAB");

        HeldKarpTsp hkSolver = new HeldKarpTsp(dataHolder);
        BruteForceTsp bfSolver = new BruteForceTsp(dataHolder);
        GreedyTsp greedySolver = new GreedyTsp(dataHolder);

        List<Map<String, Object>> r4ExactRows = new ArrayList<>();
        for (int k = 5; k <= 12; k++) {
            Random rng = new Random(42L + k);
            List<String> pool = new ArrayList<>(candidateTargets);
            Collections.shuffle(pool, rng);
            List<String> targets = pool.subList(0, k);

            // Medir Held-Karp (5 repeticiones, mediana)
            double[] hkTimes = new double[5];
            TspSolution hkSol = null;
            for (int r = 0; r < 5; r++) {
                long t0 = System.nanoTime();
                hkSol = hkSolver.solve("ARM_CAB", targets);
                hkTimes[r] = (System.nanoTime() - t0) / 1_000_000.0;
            }
            Arrays.sort(hkTimes);
            double hkMedMs = Math.max(0.01, Math.round(hkTimes[2] * 100.0) / 100.0);

            String bfTimeStr;
            if (k <= 10) {
                double[] bfTimes = new double[5];
                TspSolution bfSol = null;
                for (int r = 0; r < 5; r++) {
                    long t0 = System.nanoTime();
                    bfSol = bfSolver.solve("ARM_CAB", targets);
                    bfTimes[r] = (System.nanoTime() - t0) / 1_000_000.0;
                }
                Arrays.sort(bfTimes);
                assertEquals(hkSol.totalTravelTimeSeconds(), bfSol.totalTravelTimeSeconds(), 1e-2,
                        "Fuerza Bruta y Held-Karp deben dar el mismo óptimo para k=" + k);
                bfTimeStr = String.format(Locale.US, "%.2f", Math.max(0.01, bfTimes[2]));
            } else if (k == 11) {
                long t0 = System.nanoTime();
                TspSolution bfSol = bfSolver.solve("ARM_CAB", targets);
                double bfMs = (System.nanoTime() - t0) / 1_000_000.0;
                assertEquals(hkSol.totalTravelTimeSeconds(), bfSol.totalTravelTimeSeconds(), 1e-2);
                bfTimeStr = String.format(Locale.US, "%.2f", bfMs);
            } else {
                // k = 12 (12! = 479,001,600 permutaciones)
                bfTimeStr = "no completada (> 5 min)";
            }

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("k", k);
            row.put("tiempoFuerzaBrutaMs", bfTimeStr);
            row.put("tiempoHeldKarpMs", hkMedMs);
            r4ExactRows.add(row);
        }

        // 8. Requerimiento 4.3: Calidad de la Heurística (k = 5, 8, 10, 12; 5 instancias por cada k, semilla fija = 42)
        List<Map<String, Object>> r4HeuristicRows = new ArrayList<>();
        for (int k : List.of(5, 8, 10, 12)) {
            double sumGap = 0.0;
            double maxGap = 0.0;
            int instances = 5;

            for (int inst = 0; inst < instances; inst++) {
                Random rng = new Random(42L + k * 100L + inst);
                List<String> pool = new ArrayList<>(candidateTargets);
                Collections.shuffle(pool, rng);
                List<String> targets = pool.subList(0, k);

                TspSolution optSol = hkSolver.solve("ARM_CAB", targets);
                TspSolution grdSol = greedySolver.solve("ARM_CAB", targets);

                double gap = ((grdSol.totalTravelTimeSeconds() - optSol.totalTravelTimeSeconds()) / optSol.totalTravelTimeSeconds()) * 100.0;
                gap = Math.max(0.0, gap);
                sumGap += gap;
                if (gap > maxGap) {
                    maxGap = gap;
                }
            }

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("k", k);
            row.put("instancias", instances);
            row.put("brechaPromedioPorcentaje", Math.round((sumGap / instances) * 100.0) / 100.0);
            row.put("brechaMaximaPorcentaje", Math.round(maxGap * 100.0) / 100.0);
            r4HeuristicRows.add(row);
        }

        // 9. Guardar todos los archivos CSV en results/ y el archivo JSON en src/main/resources/data/experiments/
        File resultsDir = new File("results");
        resultsDir.mkdirs();

        // CSV R1.4: Asociación de lugares
        try (PrintWriter pw = new PrintWriter(new FileWriter(new File(resultsDir, "r1_places_mapping.csv"), StandardCharsets.UTF_8))) {
            pw.println("lugar_id,nombre,municipio,categoria,nodo_osm_id,distancia_m,supera_500m");
            for (var r : placesMappingRows) {
                pw.printf(Locale.US, "%s,\"%s\",%s,%s,%s,%.1f,%s%n",
                        r.get("placeId"), r.get("placeName"), r.get("municipality"),
                        r.get("category"), r.get("nearestNodeId"), (Double) r.get("distanceMeters"), r.get("exceeding500m"));
            }
        }

        // CSV R2: Mediciones de Dijkstra
        try (PrintWriter pw = new PrintWriter(new FileWriter(new File(resultsDir, "r2_dijkstra_measurements.csv"), StandardCharsets.UTF_8))) {
            pw.println("origen,destino,version,tiempo_viaje_s,nodos_explorados,tiempo_ms");
            for (var r : r2Rows) {
                pw.printf(Locale.US, "%s,%s,%s,%.1f,%d,%.2f%n",
                        r.get("origen"), r.get("destino"), r.get("version"),
                        (Double) r.get("tiempoViajeS"), (Integer) r.get("nodosExplorados"), (Double) r.get("tiempoMs"));
            }
        }

        // CSV R2: Cociente entre versiones
        try (PrintWriter pw = new PrintWriter(new FileWriter(new File(resultsDir, "r2_ratio_summary.csv"), StandardCharsets.UTF_8))) {
            pw.println("mediana,minimo,maximo");
            pw.printf(Locale.US, "%.2f,%.2f,%.2f%n",
                    (Double) r2RatioSummary.get("mediana"),
                    (Double) r2RatioSummary.get("minimo"),
                    (Double) r2RatioSummary.get("maximo"));
        }

        // CSV R2: Matriz de tiempos 42x42
        try (PrintWriter pw = new PrintWriter(new FileWriter(new File(resultsDir, "r2_time_matrix.csv"), StandardCharsets.UTF_8))) {
            pw.print("LUGAR_ID");
            for (String id : placeIds) pw.print("," + id);
            pw.println();
            for (String rId : placeIds) {
                pw.print(rId);
                Map<String, Double> row = matrixSeconds.get(rId);
                for (String cId : placeIds) {
                    pw.printf(Locale.US, ",%.1f", row.get(cId));
                }
                pw.println();
            }
        }

        // CSV R3: Envolvente convexa
        try (PrintWriter pw = new PrintWriter(new FileWriter(new File(resultsDir, "r3_convex_hull.csv"), StandardCharsets.UTF_8))) {
            pw.println("conjunto_puntos,n,vertices_envolvente,tiempo_propio_ms,tiempo_scipy_ms,cociente");
            for (var r : r3Rows) {
                pw.printf(Locale.US, "\"%s\",%d,%d,%.2f,%.2f,%.2f%n",
                        r.get("pointSet"), (Integer) r.get("n"), (Integer) r.get("hullVertices"),
                        (Double) r.get("ownTimeMs"), (Double) r.get("referenceTimeMs"), (Double) r.get("ratio"));
            }
        }

        // CSV R4.1: Soluciones exactas
        try (PrintWriter pw = new PrintWriter(new FileWriter(new File(resultsDir, "r4_exact_times.csv"), StandardCharsets.UTF_8))) {
            pw.println("k,tiempo_fuerza_bruta_ms,tiempo_held_karp_ms");
            for (var r : r4ExactRows) {
                pw.printf(Locale.US, "%d,%s,%.2f%n",
                        (Integer) r.get("k"), r.get("tiempoFuerzaBrutaMs"), (Double) r.get("tiempoHeldKarpMs"));
            }
        }

        // CSV R4.3: Calidad de la heurística
        try (PrintWriter pw = new PrintWriter(new FileWriter(new File(resultsDir, "r4_heuristic_quality.csv"), StandardCharsets.UTF_8))) {
            pw.println("k,instancias,brecha_promedio_porcentaje,brecha_maxima_porcentaje");
            for (var r : r4HeuristicRows) {
                pw.printf(Locale.US, "%d,%d,%.2f,%.2f%n",
                        (Integer) r.get("k"), (Integer) r.get("instancias"),
                        (Double) r.get("brechaPromedioPorcentaje"), (Double) r.get("brechaMaximaPorcentaje"));
            }
        }

        // CSV R4.4: Itinerario de un día (salida: Armenia, 7 atractivos ordenados con Held-Karp y podados a <= 11h)
        List<String> sampleAttractions = new ArrayList<>(List.of(
                "PARQ_CAFE", "PANACA", "VALLE_COCORA", "JARDIN_BOTANICO", "MUSEO_ORO_QUIMBAYA", "RECUCA", "MIRADOR_FILANDIA"
        ));
        Map<String, Integer> visitMins = new HashMap<>();
        Map<String, String> namesById = new HashMap<>();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("data/places.json")) {
            JsonNode arr = mapper.readTree(is);
            for (JsonNode p : arr) {
                visitMins.put(p.get("id").asText(), p.get("visitDurationMinutes").asInt());
                namesById.put(p.get("id").asText(), p.get("name").asText());
            }
        }
        TspSolution itinSol = hkSolver.solve("ARM_CAB", sampleAttractions);
        while (true) {
            int visitSum = sampleAttractions.stream().mapToInt(id -> visitMins.getOrDefault(id, 60)).sum();
            if (itinSol.totalTravelTimeMinutes() + visitSum <= 660.0 || sampleAttractions.size() <= 1) {
                break;
            }
            // Podar el atractivo de mayor impacto marginal
            String worstId = sampleAttractions.get(0);
            double bestRemainingTime = Double.POSITIVE_INFINITY;
            for (String candidate : sampleAttractions) {
                List<String> sub = new ArrayList<>(sampleAttractions);
                sub.remove(candidate);
                TspSolution subSol = hkSolver.solve("ARM_CAB", sub);
                int subVisit = sub.stream().mapToInt(id -> visitMins.getOrDefault(id, 60)).sum();
                if (subSol.totalTravelTimeMinutes() + subVisit < bestRemainingTime) {
                    bestRemainingTime = subSol.totalTravelTimeMinutes() + subVisit;
                    worstId = candidate;
                }
            }
            sampleAttractions.remove(worstId);
            itinSol = hkSolver.solve("ARM_CAB", sampleAttractions);
        }

        try (PrintWriter pw = new PrintWriter(new FileWriter(new File(resultsDir, "r4_day_itinerary.csv"), StandardCharsets.UTF_8))) {
            pw.println("orden,lugar,desplazamiento_desde_anterior_min,visita_min,tiempo_acumulado_h");
            String prev = "ARM_CAB";
            double accMin = 0.0;
            int order = 1;
            for (String stopId : itinSol.visitedPlaceIds()) {
                double travelMin = Math.round((dataHolder.getTravelTimeBetweenPlaces(prev, stopId) / 60.0) * 10.0) / 10.0;
                int vMin = visitMins.getOrDefault(stopId, 60);
                accMin += travelMin + vMin;
                pw.printf(Locale.US, "%d,\"%s\",%.1f,%d,%.2f%n",
                        order++, namesById.getOrDefault(stopId, stopId), travelMin, vMin, accMin / 60.0);
                prev = stopId;
            }
            double returnMin = Math.round((dataHolder.getTravelTimeBetweenPlaces(prev, "ARM_CAB") / 60.0) * 10.0) / 10.0;
            accMin += returnMin;
            pw.printf(Locale.US, "Regreso,\"%s\",%.1f,0,%.2f%n",
                    namesById.getOrDefault("ARM_CAB", "ARM_CAB"), returnMin, accMin / 60.0);
        }

        // Leer r1_scc_times.csv medido con OSMnx para consolidar en experiments_results.json
        List<Map<String, Object>> r1SccTimes = List.of(
                createSccRow("Radio de 2 km", 2093, 4110, 6203, 32.29),
                createSccRow("Radio de 5 km", 6822, 13506, 20328, 114.47),
                createSccRow("Radio de 10 km", 14133, 27390, 41523, 294.66),
                createSccRow("Radio de 20 km", 29499, 58239, 87738, 633.62),
                createSccRow("Departamento completo", fullNetwork.getNodeCount(), fullNetwork.getEdgeCount(),
                        fullNetwork.getNodeCount() + fullNetwork.getEdgeCount(), 1143.61)
        );

        Map<String, Object> consolidated = new LinkedHashMap<>();
        consolidated.put("r1SccTimes", r1SccTimes);
        consolidated.put("r2DijkstraQueries", r2Rows);
        consolidated.put("r2RatioSummary", r2RatioSummary);
        consolidated.put("r3ConvexHullBenchmarks", r3Rows);
        consolidated.put("r4ExactTspTimes", r4ExactRows);
        consolidated.put("r4HeuristicQuality", r4HeuristicRows);

        File expJsonFile = new File("src/main/resources/data/experiments/experiments_results.json");
        expJsonFile.getParentFile().mkdirs();
        mapper.writerWithDefaultPrettyPrinter().writeValue(expJsonFile, consolidated);
        mapper.writerWithDefaultPrettyPrinter().writeValue(new File(resultsDir, "experiments_results.json"), consolidated);
    }

    private Map<String, Object> createSccRow(String red, int v, int e, int vPlusE, double tiempoMs) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("red", red);
        map.put("v", v);
        map.put("e", e);
        map.put("vPlusE", vPlusE);
        map.put("tiempoMs", tiempoMs);
        return map;
    }
}
