package com.analisis.backend.algorithm.tsp;

import com.analisis.backend.model.domain.NetworkDataHolder;
import com.analisis.backend.model.domain.TspSolution;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Implementación propia de la Heurística Voraz (Vecino más Cercano) para el problema del viajante asimétrico (ATSP).
 * En cada paso toma la decisión miope de visitar el lugar no visitado más próximo desde el nodo actual.
 * Complejidad: Theta(k^2) en tiempo, O(k) en memoria.
 */
public class GreedyTsp {

    private final NetworkDataHolder dataHolder;

    public GreedyTsp(NetworkDataHolder dataHolder) {
        this.dataHolder = dataHolder;
    }

    public TspSolution solve(String originId, List<String> targetIds) {
        long startTime = System.nanoTime();
        int k = targetIds.size();

        Set<String> unvisited = new HashSet<>(targetIds);
        List<String> visitedOrder = new ArrayList<>();
        String current = originId;
        double totalTime = 0.0;

        while (!unvisited.isEmpty()) {
            String bestNext = null;
            double minStepTime = Double.POSITIVE_INFINITY;

            for (String candidate : unvisited) {
                double time = dataHolder.getTravelTimeBetweenPlaces(current, candidate);
                if (time < minStepTime) {
                    minStepTime = time;
                    bestNext = candidate;
                }
            }

            if (bestNext == null || minStepTime == Double.POSITIVE_INFINITY) {
                // Si no hay conexión factible
                break;
            }

            visitedOrder.add(bestNext);
            unvisited.remove(bestNext);
            totalTime += minStepTime;
            current = bestNext;
        }

        // Regreso al origen
        double returnTime = dataHolder.getTravelTimeBetweenPlaces(current, originId);
        totalTime += returnTime;

        List<String> fullTour = new ArrayList<>();
        fullTour.add(originId);
        fullTour.addAll(visitedOrder);
        fullTour.add(originId);

        long elapsedNanos = System.nanoTime() - startTime;
        double executionTimeMs = elapsedNanos / 1_000_000.0;

        double minutes = Math.round((totalTime / 60.0) * 10.0) / 10.0;
        double hours = Math.round((totalTime / 3600.0) * 100.0) / 100.0;

        String recurrence = "T(k) = k + (k-1) + ... + 1 = k(k+1)/2 => Theta(k^2)";
        String justification = "Heurística Voraz del vecino más cercano: construye un ciclo factible de forma casi instantánea, " +
                "pero su carácter miope puede forzar aristas de alto costo al final del recorrido para cerrar el ciclo.";

        return new TspSolution(
                originId,
                k,
                visitedOrder,
                fullTour,
                Math.round(totalTime * 10.0) / 10.0,
                minutes,
                hours,
                Math.round(executionTimeMs * 100.0) / 100.0,
                "VORAZ",
                0.0001,
                false,
                recurrence,
                justification
        );
    }
}
