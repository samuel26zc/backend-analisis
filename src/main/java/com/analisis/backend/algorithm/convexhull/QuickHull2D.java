package com.analisis.backend.algorithm.convexhull;

import com.analisis.backend.util.GeoUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Implementación propia del algoritmo QuickHull (Divide y Vencerás) para Envolvente Convexa 2D.
 * Utiliza coordenadas enteras de 64 bits (UTM Zona 18N) para evitar errores de redondeo
 * y desbordamientos numéricos. Elimina puntos duplicados y colineales sobre los lados del polígono.
 *
 * Complejidad:
 *   - Caso promedio: O(n log n)
 *   - Peor caso: O(n^2)
 *   - Recurrencia: T(n) = 2T(n/2) + O(n)
 */
public class QuickHull2D {

    public record Point(long x, long y, double latitude, double longitude) {
        public Point(long x, long y) {
            this(x, y, 0.0, 0.0);
        }
    }

    public record HullResult(
            List<Point> vertices,
            double perimeterMeters,
            double perimeterKilometers,
            double areaSquareMeters,
            double areaSquareKilometers,
            double executionTimeMs,
            String algorithmName,
            String recurrence
    ) {}

    /**
     * Calcula la envolvente convexa del conjunto de puntos dado.
     */
    public HullResult computeHull(List<Point> inputPoints) {
        long startTime = System.nanoTime();

        // 1. Eliminar duplicados usando Set basado en coordenadas enteras (x, y)
        Set<String> seen = new HashSet<>();
        List<Point> uniquePoints = new ArrayList<>();
        for (Point p : inputPoints) {
            String key = p.x() + ":" + p.y();
            if (seen.add(key)) {
                uniquePoints.add(p);
            }
        }

        int n = uniquePoints.size();
        if (n <= 3) {
            long elapsedNanos = System.nanoTime() - startTime;
            return buildResult(uniquePoints, elapsedNanos / 1_000_000.0);
        }

        // 2. Encontrar puntos extremos con abscisa mínima y máxima
        Point minXPoint = uniquePoints.stream().min(Comparator.comparingLong(Point::x).thenComparingLong(Point::y)).orElseThrow();
        Point maxXPoint = uniquePoints.stream().max(Comparator.comparingLong(Point::x).thenComparingLong(Point::y)).orElseThrow();

        // 3. Dividir los puntos a ambos lados del segmento minX -> maxX
        List<Point> leftSet = new ArrayList<>();
        List<Point> rightSet = new ArrayList<>();

        for (Point p : uniquePoints) {
            if (p.equals(minXPoint) || p.equals(maxXPoint)) continue;
            long location = pointLocation(minXPoint, maxXPoint, p);
            if (location > 0) {
                leftSet.add(p);
            } else if (location < 0) {
                rightSet.add(p);
            }
        }

        // 4. Conquistar recursivamente
        List<Point> hullUpper = new ArrayList<>();
        List<Point> hullLower = new ArrayList<>();

        hullUpper.add(minXPoint);
        findHull(minXPoint, maxXPoint, leftSet, hullUpper);
        hullUpper.add(maxXPoint);

        findHull(maxXPoint, minXPoint, rightSet, hullLower);

        // 5. Unir y ordenar vértices en sentido antihorario
        List<Point> allHull = new ArrayList<>(hullUpper);
        for (Point p : hullLower) {
            if (!allHull.contains(p)) {
                allHull.add(p);
            }
        }

        // 6. Filtrar puntos colineales en los bordes y ordenar angularmente
        List<Point> sortedHull = sortCounterClockwiseAndRemoveCollinear(allHull);

        long elapsedNanos = System.nanoTime() - startTime;
        return buildResult(sortedHull, elapsedNanos / 1_000_000.0);
    }

    private void findHull(Point p1, Point p2, List<Point> set, List<Point> hull) {
        if (set.isEmpty()) {
            return;
        }

        // Encontrar el punto más distante del segmento p1 -> p2 (mayor producto cruzado / área)
        long maxDistance = -1;
        Point furthestPoint = null;

        for (Point p : set) {
            long dist = crossProduct(p1, p2, p);
            if (dist > maxDistance) {
                maxDistance = dist;
                furthestPoint = p;
            }
        }

        if (furthestPoint == null) {
            return;
        }

        // Dividir el subconjunto restante respecto a p1 -> furthestPoint y furthestPoint -> p2
        List<Point> leftSet1 = new ArrayList<>();
        List<Point> leftSet2 = new ArrayList<>();

        for (Point p : set) {
            if (p.equals(furthestPoint)) continue;
            if (pointLocation(p1, furthestPoint, p) > 0) {
                leftSet1.add(p);
            } else if (pointLocation(furthestPoint, p2, p) > 0) {
                leftSet2.add(p);
            }
        }

        // Subproblemas recursivos
        findHull(p1, furthestPoint, leftSet1, hull);
        hull.add(furthestPoint);
        findHull(furthestPoint, p2, leftSet2, hull);
    }

    /**
     * Producto cruzado 2D de vectores (p2 - p1) x (p - p1).
     * Devuelve positivo si p está a la izquierda de la línea dirigida p1 -> p2,
     * negativo si está a la derecha, y cero si son colineales.
     */
    public static long crossProduct(Point p1, Point p2, Point p) {
        return (p2.x() - p1.x()) * (p.y() - p1.y()) - (p2.y() - p1.y()) * (p.x() - p1.x());
    }

    private static long pointLocation(Point p1, Point p2, Point p) {
        long cp = crossProduct(p1, p2, p);
        if (cp > 0) return 1;
        if (cp < 0) return -1;
        return 0;
    }

    private List<Point> sortCounterClockwiseAndRemoveCollinear(List<Point> hullPoints) {
        if (hullPoints.size() <= 3) {
            return hullPoints;
        }

        // Calcular centroide para ordenación polar
        double cx = 0;
        double cy = 0;
        for (Point p : hullPoints) {
            cx += p.x();
            cy += p.y();
        }
        cx /= hullPoints.size();
        cy /= hullPoints.size();

        final double centerX = cx;
        final double centerY = cy;

        List<Point> sorted = new ArrayList<>(hullPoints);
        sorted.sort((a, b) -> {
            double angleA = Math.atan2(a.y() - centerY, a.x() - centerX);
            double angleB = Math.atan2(b.y() - centerY, b.x() - centerX);
            return Double.compare(angleA, angleB);
        });

        // Eliminar puntos colineales estrictos sobre las aristas
        List<Point> filtered = new ArrayList<>();
        int m = sorted.size();
        for (int i = 0; i < m; i++) {
            Point prev = sorted.get((i - 1 + m) % m);
            Point curr = sorted.get(i);
            Point next = sorted.get((i + 1) % m);

            // Si el producto cruzado es cero, curr es colineal entre prev y next
            if (crossProduct(prev, next, curr) != 0) {
                filtered.add(curr);
            }
        }

        return filtered;
    }

    private HullResult buildResult(List<Point> vertices, double executionTimeMs) {
        int m = vertices.size();
        double perimeter = 0.0;
        double area = 0.0;

        for (int i = 0; i < m; i++) {
            Point curr = vertices.get(i);
            Point next = vertices.get((i + 1) % m);

            // Perímetro euclidiano
            perimeter += GeoUtils.euclideanDistance(curr.x(), curr.y(), next.x(), next.y());

            // Fórmula de Gauss (Shoelace) para el área
            area += (double) curr.x() * next.y() - (double) next.x() * curr.y();
        }

        area = Math.abs(area) / 2.0;
        double perimKm = Math.round((perimeter / 1000.0) * 100.0) / 100.0;
        double areaKm2 = Math.round((area / 1_000_000.0) * 100.0) / 100.0;

        String name = "Divide and Conquer Convex Hull (QuickHull)";
        String recurrence = "T(n) = 2T(n/2) + O(n) => O(n log n) caso promedio, O(n^2) peor caso";

        return new HullResult(
                vertices,
                Math.round(perimeter * 10.0) / 10.0,
                perimKm,
                Math.round(area * 10.0) / 10.0,
                areaKm2,
                Math.round(executionTimeMs * 100.0) / 100.0,
                name,
                recurrence
        );
    }
}
