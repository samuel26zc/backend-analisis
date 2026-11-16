#!/usr/bin/env python3
"""
Benchmark Experimental y Verificación del Requerimiento 3:
Envolvente Convexa con `scipy.spatial.ConvexHull` (Qhull en C nativo) frente a `QuickHull2D` (Java 21).

Cumple con el Requerimiento 3.1 y 3.2 y el Protocolo Experimental (Sección 6):
- Convierte/lee coordenadas en metros enteros (UTM Zona 18N) eliminando puntos repetidos.
- Ejecuta `scipy.spatial.ConvexHull` sobre:
  1. Los nodos de la componente fuertemente conexa (SCC) de la red vial del Quindío.
  2. Conjuntos de puntos al azar dentro de un cuadrado (1,000, 10,000 y 50,000 puntos)
     generados con el mismo LCG de 48 bits de `java.util.Random(seed=42)`.
- Filtra puntos colineales intermedios sobre las aristas del polígono convexo para comparar
  exactamente los vértices extremos contra `QuickHull2D`.
- Repite cada medición 5 veces y reporta la mediana en milisegundos (ms).
"""

import json
import os
import statistics
import sys
import time
import numpy as np
import scipy
from scipy.spatial import ConvexHull
import networkx as nx
import osmnx as ox

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
NETWORK_JSON = os.path.normpath(os.path.join(SCRIPT_DIR, "../src/main/resources/data/quindio_network.json"))


class JavaRandom:
    """Replicación exacta del generador LCG de 48 bits de `java.util.Random`."""
    def __init__(self, seed: int):
        self.seed = (seed ^ 0x5DEECE66D) & ((1 << 48) - 1)

    def _next(self, bits: int) -> int:
        self.seed = (self.seed * 0x5DEECE66D + 0xB) & ((1 << 48) - 1)
        return self.seed >> (48 - bits)

    def next_int(self, bound: int) -> int:
        if (bound & -bound) == bound:
            return (bound * self._next(31)) >> 31
        while True:
            bits = self._next(31)
            val = bits % bound
            if bits - val + (bound - 1) < (1 << 31):
                return val


def filter_collinear_ccw(pts_hull: np.ndarray) -> list[list[int]]:
    """
    Ordena los vértices del hull en sentido antihorario (CCW) comenzando por el menor (x, y)
    y elimina cualquier punto colineal intermedio sobre los lados del polígono (Requerimiento 3).
    """
    pts = [tuple(map(int, p)) for p in pts_hull]
    pts = list(dict.fromkeys(pts))
    if len(pts) <= 2:
        return [list(p) for p in sorted(pts)]

    cx = sum(p[0] for p in pts) / len(pts)
    cy = sum(p[1] for p in pts) / len(pts)
    pts.sort(key=lambda p: np.arctan2(p[1] - cy, p[0] - cx))

    def cross(o, a, b) -> int:
        return (a[0] - o[0]) * (b[1] - o[1]) - (a[1] - o[1]) * (b[0] - o[0])

    # Eliminar puntos colineales intermedios en el ciclo cerrado
    changed = True
    while changed and len(pts) >= 3:
        changed = False
        new_pts = []
        m = len(pts)
        for i in range(m):
            prev_p = pts[(i - 1) % m]
            curr_p = pts[i]
            next_p = pts[(i + 1) % m]
            if cross(prev_p, curr_p, next_p) != 0:
                new_pts.append(curr_p)
            else:
                changed = True
        pts = new_pts

    # Rotar para iniciar en el punto con menor (x, y)
    min_idx = min(range(len(pts)), key=lambda i: (pts[i][0], pts[i][1]))
    pts = pts[min_idx:] + pts[:min_idx]
    return [[p[0], p[1]] for p in pts]


def measure_scipy_hull(points_xy: np.ndarray, repetitions: int = 5) -> dict:
    # Deduplicar puntos enteros como exige el Requerimiento 3
    unique_pts = np.unique(points_xy, axis=0)
    times_ms = []
    hull = None
    for _ in range(repetitions):
        t0 = time.perf_counter_ns()
        hull = ConvexHull(unique_pts)
        elapsed_ms = (time.perf_counter_ns() - t0) / 1_000_000.0
        times_ms.append(elapsed_ms)

    hull_pts = unique_pts[hull.vertices]
    strict_vertices = filter_collinear_ccw(hull_pts)
    return {
        "inputN": int(len(points_xy)),
        "uniqueN": int(len(unique_pts)),
        "scipyHullVerticesCount": len(strict_vertices),
        "scipyVertices": strict_vertices,
        "scipyTimeMs": round(statistics.median(times_ms), 3),
    }


def load_scc_network_points() -> np.ndarray:
    with open(NETWORK_JSON, "r", encoding="utf-8") as f:
        data = json.load(f)
    g = nx.MultiDiGraph(crs="epsg:4326")
    for n in data["nodes"]:
        g.add_node(n["id"], y=n["latitude"], x=n["longitude"], utmX=n["utmX"], utmY=n["utmY"])
    for e in data["edges"]:
        g.add_edge(e["source"], e["target"])
    g_scc = ox.truncate.largest_component(g, strongly=True)
    coords = [[int(attr["utmX"]), int(attr["utmY"])] for _, attr in g_scc.nodes(data=True)]
    return np.array(coords, dtype=np.int64)


def generate_random_square_points(n: int, seed: int = 42) -> np.ndarray:
    rng = JavaRandom(seed)
    pts = np.empty((n, 2), dtype=np.int64)
    for i in range(n):
        pts[i, 0] = 400000 + rng.next_int(100000)
        pts[i, 1] = 480000 + rng.next_int(100000)
    return pts


def run_all(seed: int = 42, counts: list[int] = [1000, 10000, 50000]) -> dict:
    net_pts = load_scc_network_points()
    results = {
        "scipyVersion": scipy.__version__,
        "seed": seed,
        "network": measure_scipy_hull(net_pts, 5),
        "squares": {},
    }
    for c in counts:
        sq_pts = generate_random_square_points(c, seed)
        results["squares"][str(c)] = measure_scipy_hull(sq_pts, 5)
    return results


if __name__ == "__main__":
    seed_arg = int(sys.argv[1]) if len(sys.argv) > 1 else 42
    res = run_all(seed_arg)
    out_json = os.path.normpath(os.path.join(SCRIPT_DIR, "../src/main/resources/data/experiments/scipy_benchmark_seed42.json"))
    os.makedirs(os.path.dirname(out_json), exist_ok=True)
    with open(out_json, "w", encoding="utf-8") as f:
        json.dump(res, f, indent=2)
    print(f"[✓] Benchmark SciPy guardado en {out_json}")
