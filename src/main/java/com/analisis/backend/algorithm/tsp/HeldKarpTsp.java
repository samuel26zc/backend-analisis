package com.analisis.backend.algorithm.tsp;

import com.analisis.backend.model.domain.NetworkDataHolder;
import com.analisis.backend.model.domain.TspSolution;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Implementación propia desde cero del algoritmo de Programación Dinámica de Held-Karp
 * para el problema del viajante asimétrico (ATSP).
 *
 * Subestructura Óptima:
 *   Si el camino más corto que parte del origen '0', visita el subconjunto de nodos S
 *   y termina en 'j' tiene como penúltimo nodo a 'i', entonces el prefijo del camino
 *   que va de '0' a 'i' visitando S \ {j} debe ser a su vez óptimo para dicho subestado.
 *
 * Ecuación de Recurrencia:
 *   C(S, j) = min_{i in S \ {j}} { C(S \ {j}, i) + dist(i, j) }
 *   Caso Base: C({j}, j) = dist(origen, j) para todo j en targets
 *   Cierre del Ciclo Óptimo: min_{j in S_completo} { C(S_completo, j) + dist(j, origen) }
 *
 * Complejidad:
 *   - Temporal: Theta(k^2 * 2^k)
 *   - Espacial: Theta(k * 2^k)
 */
public class HeldKarpTsp {

    private final NetworkDataHolder dataHolder;

    public HeldKarpTsp(NetworkDataHolder dataHolder) {
        this.dataHolder = dataHolder;
    }

    public TspSolution solve(String originId, List<String> targetIds) {
        long startTime = System.nanoTime();
        int k = targetIds.size();

        if (k == 0) {
            long elapsedNanos = System.nanoTime() - startTime;
            return new TspSolution(
                    originId, 0, Collections.emptyList(), List.of(originId, originId),
                    0.0, 0.0, 0.0, elapsedNanos / 1_000_000.0,
                    "HELD_KARP", 0.0, false, "", ""
            );
        }

        // Matriz de distancias local de tamaño (k + 1) x (k + 1)
        // Índice 0 = Origen, Índices 1..k = targets
        double[][] dist = new double[k + 1][k + 1];

        for (int i = 0; i < k; i++) {
            dist[0][i + 1] = dataHolder.getTravelTimeBetweenPlaces(originId, targetIds.get(i));
            dist[i + 1][0] = dataHolder.getTravelTimeBetweenPlaces(targetIds.get(i), originId);
        }

        for (int i = 0; i < k; i++) {
            for (int j = 0; j < k; j++) {
                if (i == j) {
                    dist[i + 1][j + 1] = 0.0;
                } else {
                    dist[i + 1][j + 1] = dataHolder.getTravelTimeBetweenPlaces(targetIds.get(i), targetIds.get(j));
                }
            }
        }

        int numStates = 1 << k; // 2^k estados
        double[][] dp = new double[numStates][k];
        int[][] parent = new int[numStates][k];

        for (int mask = 0; mask < numStates; mask++) {
            Arrays.fill(dp[mask], Double.POSITIVE_INFINITY);
            Arrays.fill(parent[mask], -1);
        }

        // Caso Base: Subconjuntos con exactamente 1 lugar visitado desde el origen (índice 0)
        for (int j = 0; j < k; j++) {
            dp[1 << j][j] = dist[0][j + 1];
        }

        // Programación Dinámica por tamaño creciente de subconjuntos
        for (int mask = 1; mask < numStates; mask++) {
            for (int j = 0; j < k; j++) {
                if ((mask & (1 << j)) == 0) continue; // j no pertenece al subconjunto actual

                double currentCost = dp[mask][j];
                if (currentCost == Double.POSITIVE_INFINITY) continue;

                // Intentar transitar hacia un nuevo nodo nextNode no visitado en 'mask'
                for (int nextNode = 0; nextNode < k; nextNode++) {
                    if ((mask & (1 << nextNode)) != 0) continue; // ya visitado

                    int nextMask = mask | (1 << nextNode);
                    double newCost = currentCost + dist[j + 1][nextNode + 1];

                    if (newCost < dp[nextMask][nextNode]) {
                        dp[nextMask][nextNode] = newCost;
                        parent[nextMask][nextNode] = j;
                    }
                }
            }
        }

        // Cierre del ciclo: regresar de j al origen (índice 0)
        int fullMask = (1 << k) - 1;
        double bestTime = Double.POSITIVE_INFINITY;
        int lastNode = -1;

        for (int j = 0; j < k; j++) {
            double totalTourCost = dp[fullMask][j] + dist[j + 1][0];
            if (totalTourCost < bestTime) {
                bestTime = totalTourCost;
                lastNode = j;
            }
        }

        // Reconstrucción del recorrido óptimo (backtracking)
        List<String> visitedOrder = new ArrayList<>();
        int currMask = fullMask;
        int currNode = lastNode;

        while (currNode != -1) {
            visitedOrder.add(targetIds.get(currNode));
            int p = parent[currMask][currNode];
            currMask ^= (1 << currNode);
            currNode = p;
        }

        Collections.reverse(visitedOrder);

        List<String> fullTour = new ArrayList<>();
        fullTour.add(originId);
        fullTour.addAll(visitedOrder);
        fullTour.add(originId);

        long elapsedNanos = System.nanoTime() - startTime;
        double executionTimeMs = elapsedNanos / 1_000_000.0;

        // Estimación de memoria de la tabla DP en MB: numStates * k * (8 bytes double + 4 bytes int)
        double memoryBytes = (double) numStates * k * 12.0;
        double memoryMb = memoryBytes / (1024.0 * 1024.0);

        double minutes = Math.round((bestTime / 60.0) * 10.0) / 10.0;
        double hours = Math.round((bestTime / 3600.0) * 100.0) / 100.0;

        String recurrence = "C(S, j) = min_{i in S \\ {j}} { C(S \\ {j}, i) + dist(i, j) }, " +
                "con base C({j}, j) = dist(0, j) y cierre min_{j in S_full} { C(S_full, j) + dist(j, 0) }";

        String justification = "Subestructura Óptima: Si el camino más corto que parte de 0, visita S y termina en j " +
                "tiene como penúltimo nodo a i, el subcamino de 0 a i visitando S \\ {j} debe ser a su vez óptimo para ese subestado. " +
                "La tabla de DP usa 2^k * k celdas garantizando tiempo Theta(k^2 * 2^k).";

        return new TspSolution(
                originId,
                k,
                visitedOrder,
                fullTour,
                Math.round(bestTime * 10.0) / 10.0,
                minutes,
                hours,
                Math.round(executionTimeMs * 100.0) / 100.0,
                "HELD_KARP",
                Math.round(memoryMb * 1000.0) / 1000.0,
                false,
                recurrence,
                justification
        );
    }
}
