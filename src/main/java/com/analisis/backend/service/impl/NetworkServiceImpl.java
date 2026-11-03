package com.analisis.backend.service.impl;

import com.analisis.backend.dto.response.MemoryEstimationResponseDto;
import com.analisis.backend.dto.response.NetworkStatsResponseDto;
import com.analisis.backend.dto.response.SccResponseDto;
import com.analisis.backend.model.domain.NetworkDataHolder;
import com.analisis.backend.model.domain.RoadNetwork;
import com.analisis.backend.model.domain.RoadNode;
import com.analisis.backend.model.domain.SccResult;
import com.analisis.backend.service.NetworkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NetworkServiceImpl implements NetworkService {

    private final NetworkDataHolder dataHolder;

    @Override
    public NetworkStatsResponseDto getNetworkStats() {
        RoadNetwork fullNet = dataHolder.getFullNetwork();
        RoadNetwork sccNet = dataHolder.getLargestSccNetwork();

        return NetworkStatsResponseDto.builder()
                .totalNodes(fullNet.getNodeCount())
                .totalEdges(fullNet.getEdgeCount())
                .sccNodes(sccNet.getNodeCount())
                .sccEdges(sccNet.getEdgeCount())
                .totalPlaces(dataHolder.getPlaceIds().size())
                .downloadSource("OpenStreetMap via OSMnx (Quindío, Colombia)")
                .networkType("drive")
                .coordinateSystem("WGS84 & UTM Zona 18N EPSG:32618 (metros enteros 64-bit)")
                .speedTreatmentNote("Velocidades obtenidas de la etiqueta maxspeed de OSM. En tramos sin velocidad registrada, " +
                        "se imputó velocidad estándar según el tipo de vía (motorway: 80 km/h, primary: 60 km/h, " +
                        "secondary: 50 km/h, tertiary: 40 km/h, residential/unclassified: 30 km/h).")
                .build();
    }

    @Override
    public MemoryEstimationResponseDto getMemoryEstimation() {
        RoadNetwork.MemoryEstimation est = dataHolder.getFullNetwork().estimateMemoryUsage();

        return MemoryEstimationResponseDto.builder()
                .nodeCount(est.nodeCount())
                .edgeCount(est.edgeCount())
                .matrixMegabytes(Math.round(est.matrixMegabytes() * 100.0) / 100.0)
                .matrixAssumptions(est.matrixAssumptions())
                .listMegabytes(Math.round(est.listMegabytes() * 100.0) / 100.0)
                .listAssumptions(est.listAssumptions())
                .ratioMatrixToList(Math.round(est.ratioMatrixToList() * 100.0) / 100.0)
                .justification(est.justification())
                .build();
    }

    @Override
    public SccResponseDto getSccReport() {
        SccResult scc = dataHolder.getSccResult();
        if (scc == null) {
            return SccResponseDto.builder().build();
        }

        return SccResponseDto.builder()
                .totalNodesBefore(scc.totalNodesBefore())
                .totalEdgesBefore(scc.totalEdgesBefore())
                .sccNodesCount(scc.sccNodesCount())
                .sccEdgesCount(scc.sccEdgesCount())
                .retentionPercentageNodes(Math.round(scc.retentionPercentageNodes() * 100.0) / 100.0)
                .retentionPercentageEdges(Math.round(scc.retentionPercentageEdges() * 100.0) / 100.0)
                .executionTimeMs(Math.round(scc.executionTimeMs() * 100.0) / 100.0)
                .totalComponents(scc.totalComponents())
                .algorithmName(scc.algorithmName())
                .complexity(scc.complexity())
                .justification(scc.justification())
                .build();
    }

    @Override
    public List<RoadNode> getSccNodes() {
        return new ArrayList<>(dataHolder.getLargestSccNetwork().getNodes().values());
    }
}
