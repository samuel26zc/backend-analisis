package com.analisis.backend.service.impl;

import com.analisis.backend.algorithm.convexhull.QuickHull2D;
import com.analisis.backend.algorithm.convexhull.scipy.SciPyBenchmarkRunner;
import com.analisis.backend.dto.request.ConvexHullBenchmarkRequestDto;
import com.analisis.backend.dto.request.ConvexHullComputeRequestDto;
import com.analisis.backend.dto.response.ConvexHullBenchmarkResponseDto;
import com.analisis.backend.dto.response.ConvexHullResponseDto;
import com.analisis.backend.dto.response.CoordinateDto;
import com.analisis.backend.model.domain.NetworkDataHolder;
import com.analisis.backend.model.domain.RoadNode;
import com.analisis.backend.service.ConvexHullService;
import com.analisis.backend.util.GeoUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConvexHullServiceImpl implements ConvexHullService {

    private final NetworkDataHolder dataHolder;

    @Override
    public ConvexHullResponseDto computeNetworkConvexHull() {
        List<RoadNode> nodes = new ArrayList<>(dataHolder.getLargestSccNetwork().getNodes().values());
        if (nodes.isEmpty()) {
            nodes = new ArrayList<>(dataHolder.getFullNetwork().getNodes().values());
        }

        List<QuickHull2D.Point> points = new ArrayList<>();
        for (RoadNode n : nodes) {
            points.add(new QuickHull2D.Point(n.utmX(), n.utmY(), n.latitude(), n.longitude()));
        }

        QuickHull2D quickHull = new QuickHull2D();
        QuickHull2D.HullResult result = quickHull.computeHull(points);

        List<CoordinateDto> polyCoordinates = new ArrayList<>();
        for (QuickHull2D.Point p : result.vertices()) {
            polyCoordinates.add(new CoordinateDto(p.latitude(), p.longitude()));
        }

        return ConvexHullResponseDto.builder()
                .inputPointsCount(points.size())
                .hullVerticesCount(result.vertices().size())
                .polygonCoordinates(polyCoordinates)
                .perimeterMeters(result.perimeterMeters())
                .perimeterKilometers(result.perimeterKilometers())
                .areaSquareMeters(result.areaSquareMeters())
                .areaSquareKilometers(result.areaSquareKilometers())
                .executionTimeMs(result.executionTimeMs())
                .algorithmName(result.algorithmName())
                .complexity(result.recurrence())
                .build();
    }

    @Override
    public ConvexHullResponseDto computeCustomConvexHull(ConvexHullComputeRequestDto request) {
        List<QuickHull2D.Point> points = new ArrayList<>();
        for (CoordinateDto coord : request.getCoordinates()) {
            GeoUtils.UtmPoint utm = GeoUtils.toUtmZone18N(coord.getLatitude(), coord.getLongitude());
            points.add(new QuickHull2D.Point(utm.utmX(), utm.utmY(), coord.getLatitude(), coord.getLongitude()));
        }

        QuickHull2D quickHull = new QuickHull2D();
        QuickHull2D.HullResult result = quickHull.computeHull(points);

        List<CoordinateDto> polyCoordinates = new ArrayList<>();
        for (QuickHull2D.Point p : result.vertices()) {
            polyCoordinates.add(new CoordinateDto(p.latitude(), p.longitude()));
        }

        return ConvexHullResponseDto.builder()
                .inputPointsCount(points.size())
                .hullVerticesCount(result.vertices().size())
                .polygonCoordinates(polyCoordinates)
                .perimeterMeters(result.perimeterMeters())
                .perimeterKilometers(result.perimeterKilometers())
                .areaSquareMeters(result.areaSquareMeters())
                .areaSquareKilometers(result.areaSquareKilometers())
                .executionTimeMs(result.executionTimeMs())
                .algorithmName(result.algorithmName())
                .complexity(result.recurrence())
                .build();
    }

    @Override
    public ConvexHullBenchmarkResponseDto runBenchmark(ConvexHullBenchmarkRequestDto request) {
        long seed = (request != null && request.getSeed() != 0) ? request.getSeed() : 42L;
        List<Integer> counts = (request != null && request.getPointCounts() != null && !request.getPointCounts().isEmpty())
                ? request.getPointCounts()
                : List.of(1000, 10000, 50000);

        List<ConvexHullBenchmarkResponseDto.BenchmarkRowDto> rows = new ArrayList<>();
        QuickHull2D quickHull = new QuickHull2D();

        // 1. Benchmark con los nodos reales de la SCC de la red vial del Quindío
        List<RoadNode> networkNodes = new ArrayList<>(dataHolder.getLargestSccNetwork().getNodes().values());
        List<QuickHull2D.Point> netPoints = new ArrayList<>(networkNodes.size());
        for (RoadNode n : networkNodes) {
            netPoints.add(new QuickHull2D.Point(n.utmX(), n.utmY(), n.latitude(), n.longitude()));
        }

        double netMedianOwnTime = measureMedianTime(quickHull, netPoints, 5);
        QuickHull2D.HullResult netHull = quickHull.computeHull(netPoints);
        SciPyBenchmarkRunner.SciPyEntry netScipy = SciPyBenchmarkRunner.getNetworkSciPyBenchmark();
        boolean netMatch = SciPyBenchmarkRunner.verifyVerticesMatch(netHull.vertices(), netScipy);
        double netRefTime = Math.round(netScipy.scipyTimeMs() * 100.0) / 100.0;
        double netRatio = (netRefTime > 0.0) ? Math.round((netMedianOwnTime / netRefTime) * 100.0) / 100.0 : 1.0;

        rows.add(ConvexHullBenchmarkResponseDto.BenchmarkRowDto.builder()
                .pointSet("Nodos de la red")
                .n(netPoints.size())
                .hullVertices(netHull.vertices().size())
                .ownTimeMs(netMedianOwnTime)
                .referenceTimeMs(netRefTime)
                .ratio(netRatio)
                .build());

        // 2. Benchmarks con puntos sintéticos aleatorios en un cuadrado (semilla fija = 42)
        boolean allSquaresMatch = true;
        for (int n : counts) {
            Random rng = new Random(seed);
            List<QuickHull2D.Point> randomPoints = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                long x = 400000L + rng.nextInt(100000);
                long y = 480000L + rng.nextInt(100000);
                randomPoints.add(new QuickHull2D.Point(x, y));
            }

            double medianOwnTime = measureMedianTime(quickHull, randomPoints, 5);
            QuickHull2D.HullResult hullRes = quickHull.computeHull(randomPoints);
            SciPyBenchmarkRunner.SciPyEntry sqScipy = SciPyBenchmarkRunner.getSquareSciPyBenchmark(n);
            boolean sqMatch = SciPyBenchmarkRunner.verifyVerticesMatch(hullRes.vertices(), sqScipy);
            allSquaresMatch = allSquaresMatch && sqMatch;

            double refTime = Math.round(sqScipy.scipyTimeMs() * 100.0) / 100.0;
            double ratio = (refTime > 0.0) ? Math.round((medianOwnTime / refTime) * 100.0) / 100.0 : 1.0;

            String label = String.format(Locale.US, "Cuadrado, %,d puntos", n);
            rows.add(ConvexHullBenchmarkResponseDto.BenchmarkRowDto.builder()
                    .pointSet(label)
                    .n(n)
                    .hullVertices(hullRes.vertices().size())
                    .ownTimeMs(medianOwnTime)
                    .referenceTimeMs(refTime)
                    .ratio(ratio)
                    .build());
        }

        String note = String.format(
                "Medición ejecutada con 5 repeticiones reportando la mediana (semilla=%d). " +
                "Verificación contra scipy.spatial.ConvexHull (Qhull C, SciPy 1.18.1): coincidencia exacta de vértices extremos (red=%s, cuadrados=%s).",
                seed, netMatch ? "100% idénticos" : "revisar", allSquaresMatch ? "100% idénticos" : "revisar"
        );

        return ConvexHullBenchmarkResponseDto.builder()
                .seed(seed)
                .note(note)
                .rows(rows)
                .build();
    }

    private double measureMedianTime(QuickHull2D quickHull, List<QuickHull2D.Point> points, int repetitions) {
        double[] times = new double[repetitions];
        for (int r = 0; r < repetitions; r++) {
            long t0 = System.nanoTime();
            quickHull.computeHull(points);
            long elapsed = System.nanoTime() - t0;
            times[r] = elapsed / 1_000_000.0;
        }
        Arrays.sort(times);
        return Math.round(times[repetitions / 2] * 100.0) / 100.0;
    }
}
