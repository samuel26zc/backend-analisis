# Especificación SDD — Requerimiento 3: Envolvente Convexa de la Red Vial

## 1. Descripción y Comportamiento Esperado
Calcular la envolvente convexa (menor polígono convexo) de los nodos de la componente fuertemente conexa de la red vial del Quindío proyectados a coordenadas enteras de 64 bits en metros (UTM Zona 18N), eliminando puntos duplicados y descartando puntos colineales intermedios sobre los lados del polígono.

## 2. Algoritmo y Complejidad
- **Algoritmo propio (`QuickHull2D`)**: Enfoque Divide y Vencerás con recurrencia $T(n) = 2T(n/2) + O(n) \implies O(n \log n)$ en caso promedio y $O(n^2)$ en peor caso, memoria $O(n)$.
- **Orientación exacta en 64 bits**: Producto cruz entero `long` $(x_2 - x_1)(y_3 - y_1) - (y_2 - y_1)(x_3 - x_1)$ sin error de punto flotante ni desbordamiento.
- **Referencia (`scipy.spatial.ConvexHull`)**: Librería Qhull compilada en C nativo (`scripts/scipy_convexhull_benchmark.py` y `SciPyBenchmarkRunner.java`). Tanto en `QuickHull2D` como en el script de SciPy se incluye la deduplicación de coordenadas enteras dentro del bloque cronometrado para garantizar condiciones de medición idénticas.

## 3. Protocolo Experimental y Casos de Prueba
- **Conjuntos evaluados (`results/r3_convex_hull.csv`)**:
  1. Nodos de la SCC de la red vial del Quindío ($n = 35,442$, $17$ vértices extremos).
  2. Puntos aleatorios en un cuadrado de $100\text{ km} \times 100\text{ km}$ con semilla fija `42`: $n \in \{1\,000, 10\,000, 50\,000\}$.
- **Verificación (`QuickHull2DTest.testVerifyAgainstSciPy`)**: Comprueba que el conjunto de vértices extremos de `QuickHull2D` coincida al 100 % en cantidad y coordenadas `(utmX, utmY)` con `scipy.spatial.ConvexHull` en las cuatro pruebas.
