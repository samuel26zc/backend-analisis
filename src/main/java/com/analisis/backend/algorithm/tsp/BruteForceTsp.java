package com.analisis.backend.algorithm.tsp;

import com.analisis.backend.model.domain.NetworkDataHolder;
import com.analisis.backend.model.domain.TspSolution;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Implementación propia de Fuerza Bruta para el problema del viajante asimétrico (ATSP).
 * Evalúa todas las permutaciones posibles de los k lugares a visitar saliendo y regresando al origen.
 * Complejidad: O(k!) en tiempo, O(k) en memoria.
 * Incluye temporizador de corte (5 minutos) según la regla 7 del protocolo experimental.
 */
public class BruteForceTsp {

    private final NetworkDataHolder dataHolder;
    private static final long MAX_EXECUTION_TIME_NANOS = 5L * 60 * 1_000_000_000L; // 5 minutos

    public BruteForceTsp(NetworkDataHolder dataHolder) {
        this.dataHolder = dataHolder;
    }

    public TspSolution solve(String originId, List<String> targetIds) {
        long startTime = System.nanoTime();
        int k = targetIds.size();

        if (k == 0) {
            long elapsedNanos = System.nanoTime() - startTime;
            return new TspSolution(
                    originId, 0, Collections.emptyList(), List.of(originId, originId),
                    0.0, 0.0, 0.0, Math.round((elapsedNanos / 1_000_000.0) * 100.0) / 100.0,
                    "FUERZA_BRUTA", 0.0, false,
                    "T(k) = k * T(k - 1) + O(1) => Theta(k!) evaluando cada permutacion completa",
                    "Fuerza Bruta explora exhaustivamente el espacio de soluciones de cardinalidad k!."
            );
        }

        // Matriz local (k + 1) x (k + 1) equivalente a HeldKarpTsp: 0 = origen, 1..k = destinos
        double[][] dist = new double[k + 1][k + 1];
        for (int i = 0; i < k; i++) {
            dist[0][i + 1] = dataHolder.getTravelTimeBetweenPlaces(originId, targetIds.get(i));
            dist[i + 1][0] = dataHolder.getTravelTimeBetweenPlaces(targetIds.get(i), originId);
        }
        for (int i = 0; i < k; i++) {
            for (int j = 0; j < k; j++) {
                dist[i + 1][j + 1] = (i == j)
                        ? 0.0
                        : dataHolder.getTravelTimeBetweenPlaces(targetIds.get(i), targetIds.get(j));
            }
        }

        int[] current = new int[k];
        for (int i = 0; i < k; i++) {
            current[i] = i + 1;
        }
        int[] bestPerm = new int[k];
        boolean foundBest = false;
        double bestTime = Double.POSITIVE_INFINITY;
        boolean timeout = false;

        // Evaluar la primera permutación
        double firstCost = evaluateTour(dist, current, k);
        if (firstCost < bestTime) {
            bestTime = firstCost;
            System.arraycopy(current, 0, bestPerm, 0, k);
            foundBest = true;
        }

        // Generar las k! permutaciones usando el algoritmo de Heap sin asignaciones en el bucle crítico
        int i = 0;
        int[] c = new int[k];
        long evaluated = 1L;
        while (i < k) {
            if (c[i] < i) {
                if ((evaluated & 0xFFFFL) == 0L && (System.nanoTime() - startTime > MAX_EXECUTION_TIME_NANOS)) {
                    timeout = true;
                    break;
                }

                int swapIdx = (i % 2 == 0) ? 0 : c[i];
                int tmp = current[swapIdx];
                current[swapIdx] = current[i];
                current[i] = tmp;

                double cost = evaluateTour(dist, current, k);
                if (cost < bestTime) {
                    bestTime = cost;
                    System.arraycopy(current, 0, bestPerm, 0, k);
                    foundBest = true;
                }

                evaluated++;
                c[i]++;
                i = 0;
            } else {
                c[i] = 0;
                i++;
            }
        }

        long elapsedNanos = System.nanoTime() - startTime;
        double executionTimeMs = elapsedNanos / 1_000_000.0;

        List<String> bestTour = null;
        if (foundBest) {
            bestTour = new ArrayList<>(k);
            for (int idx : bestPerm) {
                bestTour.add(targetIds.get(idx - 1));
            }
        }

        List<String> fullTour = new ArrayList<>();
        fullTour.add(originId);
        if (bestTour != null) {
            fullTour.addAll(bestTour);
        }
        fullTour.add(originId);

        double minutes = Math.round((bestTime / 60.0) * 10.0) / 10.0;
        double hours = Math.round((bestTime / 3600.0) * 100.0) / 100.0;

        String recurrence = "T(k) = k * T(k - 1) + O(1) => Theta(k!) evaluando cada permutacion completa";
        String justification = "Fuerza Bruta explora exhaustivamente el espacio de soluciones de cardinalidad k!, " +
                "garantizando la cota global óptima absoluta pero volviéndose intratable al crecer k! (e.g. k >= 13-14 excede el límite de 5 minutos).";

        return new TspSolution(
                originId,
                k,
                bestTour != null ? bestTour : Collections.emptyList(),
                fullTour,
                Math.round(bestTime * 10.0) / 10.0,
                minutes,
                hours,
                Math.round(executionTimeMs * 100.0) / 100.0,
                "FUERZA_BRUTA",
                0.0001,
                timeout,
                recurrence,
                justification
        );
    }

    private double evaluateTour(double[][] dist, int[] perm, int k) {
        double total = dist[0][perm[0]];
        for (int idx = 1; idx < k; idx++) {
            total += dist[perm[idx - 1]][perm[idx]];
        }
        total += dist[perm[k - 1]][0];
        return total;
    }
}
