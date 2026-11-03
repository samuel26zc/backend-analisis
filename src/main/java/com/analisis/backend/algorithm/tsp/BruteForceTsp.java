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

        List<String> bestTour = null;
        double bestTime = Double.POSITIVE_INFINITY;
        boolean timeout = false;

        // Copia mutable para permutaciones
        List<String> current = new ArrayList<>(targetIds);
        int[] p = new int[k];
        for (int i = 0; i < k; i++) p[i] = i;

        // Evaluar la primera permutación
        double firstCost = evaluateTour(originId, current);
        if (firstCost < bestTime) {
            bestTime = firstCost;
            bestTour = new ArrayList<>(current);
        }

        // Generar permutaciones usando el algoritmo de Heap
        int i = 0;
        int[] c = new int[k];
        while (i < k) {
            if (System.nanoTime() - startTime > MAX_EXECUTION_TIME_NANOS) {
                timeout = true;
                break;
            }

            if (c[i] < i) {
                if (i % 2 == 0) {
                    Collections.swap(current, 0, i);
                } else {
                    Collections.swap(current, c[i], i);
                }

                double cost = evaluateTour(originId, current);
                if (cost < bestTime) {
                    bestTime = cost;
                    bestTour = new ArrayList<>(current);
                }

                c[i]++;
                i = 0;
            } else {
                c[i] = 0;
                i++;
            }
        }

        long elapsedNanos = System.nanoTime() - startTime;
        double executionTimeMs = elapsedNanos / 1_000_000.0;

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
                "garantizando la cota global óptima absoluta pero resultando intratable para k > 11 bajo el límite de 5 minutos.";

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

    private double evaluateTour(String originId, List<String> targets) {
        double total = 0.0;
        String prev = originId;
        for (String target : targets) {
            double step = dataHolder.getTravelTimeBetweenPlaces(prev, target);
            if (step == Double.POSITIVE_INFINITY) return Double.POSITIVE_INFINITY;
            total += step;
            prev = target;
        }
        double returnStep = dataHolder.getTravelTimeBetweenPlaces(prev, originId);
        if (returnStep == Double.POSITIVE_INFINITY) return Double.POSITIVE_INFINITY;
        total += returnStep;
        return total;
    }
}
