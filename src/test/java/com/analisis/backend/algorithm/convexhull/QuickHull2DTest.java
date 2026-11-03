package com.analisis.backend.algorithm.convexhull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class QuickHull2DTest {

    @Test
    @DisplayName("QuickHull en cuadrado con puntos interiores y duplicados")
    void testSquareWithInteriorPoints() {
        QuickHull2D quickHull = new QuickHull2D();

        List<QuickHull2D.Point> points = new ArrayList<>();
        // 4 esquinas del cuadrado (1000 x 1000)
        points.add(new QuickHull2D.Point(0, 0));
        points.add(new QuickHull2D.Point(1000, 0));
        points.add(new QuickHull2D.Point(1000, 1000));
        points.add(new QuickHull2D.Point(0, 1000));

        // Puntos interiores (no deben estar en el casco)
        points.add(new QuickHull2D.Point(500, 500));
        points.add(new QuickHull2D.Point(200, 300));
        points.add(new QuickHull2D.Point(700, 800));

        // Puntos duplicados
        points.add(new QuickHull2D.Point(0, 0));
        points.add(new QuickHull2D.Point(1000, 1000));

        // Puntos colineales en los bordes (deben ser filtrados según requerimiento 3)
        points.add(new QuickHull2D.Point(500, 0)); // en el borde inferior
        points.add(new QuickHull2D.Point(1000, 500)); // en el borde derecho

        QuickHull2D.HullResult result = quickHull.computeHull(points);

        assertEquals(4, result.vertices().size(), "El casco convexo de un cuadrado debe contener exactamente 4 vértices");
        assertEquals(4000.0, result.perimeterMeters(), 1e-2, "El perímetro debe ser 4 x 1000 = 4000 m");
        assertEquals(1000000.0, result.areaSquareMeters(), 1e-2, "El área debe ser 1000 x 1000 = 1,000,000 m2");
    }

    @Test
    @DisplayName("Producto cruzado determina correctamente orientación de puntos")
    void testCrossProduct() {
        QuickHull2D.Point p1 = new QuickHull2D.Point(0, 0);
        QuickHull2D.Point p2 = new QuickHull2D.Point(10, 0);

        QuickHull2D.Point left = new QuickHull2D.Point(5, 5);
        QuickHull2D.Point right = new QuickHull2D.Point(5, -5);
        QuickHull2D.Point colin = new QuickHull2D.Point(15, 0);

        assertTrue(QuickHull2D.crossProduct(p1, p2, left) > 0, "Punto debe estar a la izquierda");
        assertTrue(QuickHull2D.crossProduct(p1, p2, right) < 0, "Punto debe estar a la derecha");
        assertEquals(0, QuickHull2D.crossProduct(p1, p2, colin), "Puntos colineales deben dar 0");
    }
}
