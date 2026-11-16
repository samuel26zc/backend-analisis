#!/usr/bin/env python3
"""
Medición Experimental del Requerimiento 1.3:
Componente Fuertemente Conexa con `ox.truncate.largest_component(G, strongly=True)` (OSMnx 2.1.2 / NetworkX 3.7).

Protocolo Experimental (Sección 6):
- Redes de tamaño creciente alrededor de la Plaza de Bolívar de Armenia (4.53252, -75.67304)
  con radios de 2 km, 5 km, 10 km y 20 km, más la red completa del departamento del Quindío.
- Se mide exclusivamente el tiempo de ejecución de `ox.truncate.largest_component(G, strongly=True)`.
- 5 repeticiones por cada red, reportando la mediana en milisegundos (ms).
- Exporta los resultados a `results/r1_scc_times.csv` y `results/r1_network_sizes.csv`.

Algoritmo Interno Investigado (Código Fuente OSMnx 2.1.2 & NetworkX 3.7):
- `ox.truncate.largest_component(G, strongly=True)` invoca `nx.strongly_connected_components(G)`,
  selecciona el conjunto de nodos de mayor cardinalidad `max(..., key=len)` e induce el subgrafo `G.subgraph(nodes).copy()`.
- `nx.strongly_connected_components(G)` implementa la variante de Nuutila y Soisalon-Soininen (1994)
  del Algoritmo de Tarjan mediante recorrido DFS iterativo con pila y arreglo `lowlink`.
- Complejidad en tiempo: Theta(V + E).
- Complejidad en memoria auxiliar: O(V).
"""

import csv
import inspect
import json
import math
import os
import statistics
import sys
import time

import networkx as nx
import osmnx as ox

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
NETWORK_JSON = os.path.normpath(os.path.join(SCRIPT_DIR, "../src/main/resources/data/quindio_network.json"))
RESULTS_DIR = os.path.normpath(os.path.join(SCRIPT_DIR, "../results"))

ARMENIA_PLAZA = (4.53252, -75.67304)
RADII_KM = [2, 5, 10, 20]
REPETITIONS = 5


def haversine_m(lat1: float, lon1: float, lat2: float, lon2: float) -> float:
    r = 6371000.0
    p1, p2 = math.radians(lat1), math.radians(lat2)
    dp = math.radians(lat2 - lat1)
    dl = math.radians(lon2 - lon1)
    a = math.sin(dp / 2) ** 2 + math.cos(p1) * math.cos(p2) * math.sin(dl / 2) ** 2
    return 2 * r * math.atan2(math.sqrt(a), math.sqrt(1 - a))


def load_full_multidigraph() -> nx.MultiDiGraph:
    with open(NETWORK_JSON, "r", encoding="utf-8") as f:
        data = json.load(f)
    g = nx.MultiDiGraph(crs="epsg:4326")
    for n in data["nodes"]:
        g.add_node(
            n["id"],
            y=n["latitude"],
            x=n["longitude"],
            utmX=n["utmX"],
            utmY=n["utmY"],
        )
    for e in data["edges"]:
        g.add_edge(
            e["source"],
            e["target"],
            length=e["distanceMeters"],
            speed_kph=e["speedKmh"],
            travel_time=e["travelTimeSeconds"],
            highway=e["highway"],
            oneway=e["oneway"],
        )
    return g


def build_radius_subgraph(full_g: nx.MultiDiGraph, radius_m: float) -> nx.MultiDiGraph:
    lat0, lon0 = ARMENIA_PLAZA
    keep = [
        nid
        for nid, attr in full_g.nodes(data=True)
        if haversine_m(lat0, lon0, attr["y"], attr["x"]) <= radius_m
    ]
    sub = full_g.subgraph(keep).copy()
    sub.graph["crs"] = "epsg:4326"
    return sub


def measure_largest_component(g: nx.MultiDiGraph, repetitions: int = REPETITIONS) -> tuple[float, int, int]:
    times_ms = []
    g_scc = None
    for _ in range(repetitions):
        t0 = time.perf_counter_ns()
        g_scc = ox.truncate.largest_component(g, strongly=True)
        elapsed_ms = (time.perf_counter_ns() - t0) / 1_000_000.0
        times_ms.append(elapsed_ms)
    median_ms = round(statistics.median(times_ms), 2)
    return median_ms, g_scc.number_of_nodes(), g_scc.number_of_edges()


OVERPASS_MIRRORS = [
    "https://overpass.private.coffee/api",
    "https://z.overpass-api.de/api",
    "https://overpass-api.de/api",
    "https://lz4.overpass-api.de/api",
]


def fetch_graph_from_point_with_mirrors(center_point: tuple[float, float], dist_m: int, fallback_full: nx.MultiDiGraph) -> tuple[nx.MultiDiGraph, str]:
    ox.settings.use_cache = True
    ox.settings.requests_timeout = 60
    for mirror in OVERPASS_MIRRORS:
        try:
            ox.settings.overpass_url = mirror
            g = ox.graph_from_point(center_point, dist=dist_m, network_type="drive")
            return g, f"ox.graph_from_point ({mirror})"
        except Exception as e:
            print(f"  [!] Espejo {mirror} ocupado para dist={dist_m}m: {e.__class__.__name__}")
    print(f"  [!] Todos los espejos Overpass respondieron 502/timeout; usando recorte radial sobre el grafo OSM del Quindío.")
    return build_radius_subgraph(fallback_full, float(dist_m)), "subgrafo radial local (fallback Overpass 502)"


def main():
    os.makedirs(RESULTS_DIR, exist_ok=True)
    use_online = "--offline" not in sys.argv

    print(f"[+] OSMnx version: {ox.__version__} | NetworkX version: {nx.__version__}")
    print("[+] Fuente de ox.truncate.largest_component:")
    print(inspect.getsource(ox.truncate.largest_component))

    full_g = load_full_multidigraph()

    rows = []
    for r_km in RADII_KM:
        label = f"Radio de {r_km} km"
        if use_online:
            g_rad, mode_used = fetch_graph_from_point_with_mirrors(ARMENIA_PLAZA, r_km * 1000, full_g)
        else:
            g_rad = build_radius_subgraph(full_g, r_km * 1000.0)
            mode_used = "subgrafo radial local (--offline)"

        v = g_rad.number_of_nodes()
        e = g_rad.number_of_edges()
        med_ms, v_scc, e_scc = measure_largest_component(g_rad, REPETITIONS)
        rows.append({
            "red": label,
            "v": v,
            "e": e,
            "vPlusE": v + e,
            "sccV": v_scc,
            "sccE": e_scc,
            "tiempoMs": med_ms,
        })
        print(f"  {label:22s} | V={v:6d} | E={e:6d} | V+E={v+e:7d} | SCC_V={v_scc:6d} | Mediana={med_ms:6.2f} ms")

    v_full = full_g.number_of_nodes()
    e_full = full_g.number_of_edges()
    med_full_ms, v_full_scc, e_full_scc = measure_largest_component(full_g, REPETITIONS)
    rows.append({
        "red": "Departamento completo",
        "v": v_full,
        "e": e_full,
        "vPlusE": v_full + e_full,
        "sccV": v_full_scc,
        "sccE": e_full_scc,
        "tiempoMs": med_full_ms,
    })
    print(f"  {'Departamento completo':22s} | V={v_full:6d} | E={e_full:6d} | V+E={v_full+e_full:7d} | SCC_V={v_full_scc:6d} | Mediana={med_full_ms:6.2f} ms")

    # Guardar tabla CSV 1: Tamaño antes y después de la SCC (Anexo R1)
    sizes_csv = os.path.join(RESULTS_DIR, "r1_network_sizes.csv")
    with open(sizes_csv, "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["red", "nodos", "aristas"])
        w.writerow(["Red descargada", v_full, e_full])
        w.writerow(["Componente fuertemente conexa", v_full_scc, e_full_scc])

    # Guardar tabla CSV 2: Tiempos de obtener la componente (Anexo R1)
    scc_csv = os.path.join(RESULTS_DIR, "r1_scc_times.csv")
    with open(scc_csv, "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["red", "v", "e", "v_mas_e", "tiempo_ms"])
        for r in rows:
            w.writerow([r["red"], r["v"], r["e"], r["vPlusE"], r["tiempoMs"]])

    print(f"[✓] Archivos generados: {sizes_csv} y {scc_csv}")


if __name__ == "__main__":
    main()
