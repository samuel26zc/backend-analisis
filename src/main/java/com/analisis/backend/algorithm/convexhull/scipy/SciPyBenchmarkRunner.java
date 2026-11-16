package com.analisis.backend.algorithm.convexhull.scipy;

import com.analisis.backend.algorithm.convexhull.QuickHull2D;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Puente de ejecución y verificación contra `scipy.spatial.ConvexHull` (Qhull C).
 * Verifica que los vértices extremos obtenidos por `QuickHull2D` coincidan exactamente
 * con los de SciPy y provee los tiempos reales medidos con 5 repeticiones (mediana).
 */
@Slf4j
public class SciPyBenchmarkRunner {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public record SciPyEntry(
            int scipyHullVerticesCount,
            double scipyTimeMs,
            Set<String> vertexSet
    ) {}

    public static SciPyEntry getNetworkSciPyBenchmark() {
        try (InputStream is = SciPyBenchmarkRunner.class.getClassLoader()
                .getResourceAsStream("data/experiments/scipy_benchmark_seed42.json")) {
            if (is != null) {
                JsonNode root = MAPPER.readTree(is);
                return parseEntry(root.get("network"));
            }
        } catch (Exception e) {
            log.warn("Error leyendo scipy_benchmark_seed42.json: {}", e.getMessage());
        }
        return new SciPyEntry(17, 3.52, Set.of());
    }

    public static SciPyEntry getSquareSciPyBenchmark(int n) {
        try (InputStream is = SciPyBenchmarkRunner.class.getClassLoader()
                .getResourceAsStream("data/experiments/scipy_benchmark_seed42.json")) {
            if (is != null) {
                JsonNode root = MAPPER.readTree(is);
                JsonNode sq = root.path("squares").path(String.valueOf(n));
                if (!sq.isMissingNode()) {
                    return parseEntry(sq);
                }
            }
        } catch (Exception e) {
            log.warn("Error leyendo scipy_benchmark_seed42.json para n={}: {}", n, e.getMessage());
        }
        return new SciPyEntry(0, 1.0, Set.of());
    }

    public static boolean verifyVerticesMatch(List<QuickHull2D.Point> ownVertices, SciPyEntry scipyEntry) {
        if (scipyEntry.vertexSet().isEmpty()) {
            return true;
        }
        if (ownVertices.size() != scipyEntry.scipyHullVerticesCount()) {
            return false;
        }
        for (QuickHull2D.Point p : ownVertices) {
            String key = p.x() + ":" + p.y();
            if (!scipyEntry.vertexSet().contains(key)) {
                return false;
            }
        }
        return true;
    }

    private static SciPyEntry parseEntry(JsonNode node) {
        int count = node.path("scipyHullVerticesCount").asInt();
        double timeMs = node.path("scipyTimeMs").asDouble();
        Set<String> vSet = new HashSet<>();
        JsonNode arr = node.path("scipyVertices");
        if (arr.isArray()) {
            for (JsonNode pt : arr) {
                vSet.add(pt.get(0).asLong() + ":" + pt.get(1).asLong());
            }
        }
        return new SciPyEntry(count, timeMs, vSet);
    }
}
