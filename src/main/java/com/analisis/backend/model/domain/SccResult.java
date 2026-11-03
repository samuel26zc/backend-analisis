package com.analisis.backend.model.domain;

/**
 * Resultado del análisis de Componentes Fuertemente Conexas (SCC) con el algoritmo de Tarjan.
 */
public record SccResult(
        int totalNodesBefore,
        int totalEdgesBefore,
        RoadNetwork largestSccNetwork,
        int sccNodesCount,
        int sccEdgesCount,
        double retentionPercentageNodes,
        double retentionPercentageEdges,
        double executionTimeMs,
        int totalComponents,
        String algorithmName,
        String complexity,
        String justification
) {}
