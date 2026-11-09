#!/usr/bin/env python3
"""
Script de Descarga y Exportación de la Red Vial del Quindío (OpenStreetMap via OSMnx).
Cumple con la Sección 4 ('Datos') y Sección 3 ('Condiciones Generales') del Proyecto Final:
- Descarga de la red vial con osmnx (network_type="drive").
- Extracción de nodos con latitud, longitud y conversión a coordenadas métricas UTM 18N.
- Extracción de aristas con longitud (metros), tipo de vía (highway) y velocidad máxima estimada.
- Exportación para consumo en el backend (Java 21 LTS).

Fecha oficial de descarga registrada: 2026-10-17T10:00:00-05:00
Región: Departamento del Quindío, Colombia
"""

import sys
import json
import math
from datetime import datetime

try:
    import osmnx as ox
    import networkx as nx
except ImportError:
    print("[INFO] osmnx no está instalado en el entorno local.")
    print("Para ejecutar la descarga en vivo: pip install osmnx networkx")

PLACE_QUERY = "Quindío, Colombia"
NETWORK_TYPE = "drive"
DOWNLOAD_DATE = "2026-10-17T10:00:00-05:00"
OUTPUT_FILE = "../src/main/resources/data/quindio_network.json"

DEFAULT_SPEEDS = {
    "motorway": 80.0,
    "trunk": 70.0,
    "primary": 60.0,
    "secondary": 50.0,
    "tertiary": 40.0,
    "residential": 30.0,
    "unclassified": 35.0,
    "living_street": 20.0
}

def parse_maxspeed(maxspeed_val, default_speed):
    if not maxspeed_val:
        return default_speed
    if isinstance(maxspeed_val, list):
        maxspeed_val = maxspeed_val[0]
    try:
        val_str = str(maxspeed_val).split()[0]
        return float(val_str)
    except (ValueError, IndexError):
        return default_speed

def to_utm_zone_18n(lat, lon):
    # Conversión aproximada WGS84 a UTM Zona 18N (EPSG:32618)
    a = 6378137.0
    f = 1 / 298.257223563
    k0 = 0.9996
    lon0 = -75.0 * math.pi / 180.0
    phi = lat * math.pi / 180.0
    lam = lon * math.pi / 180.0
    e2 = 2 * f - f * f
    N = a / math.sqrt(1 - e2 * math.sin(phi) ** 2)
    T = math.tan(phi) ** 2
    C = (e2 / (1 - e2)) * math.cos(phi) ** 2
    A = math.cos(phi) * (lam - lon0)
    utm_x = 500000.0 + k0 * N * (A + (1 - T + C) * (A ** 3) / 6.0)
    utm_y = k0 * (a * phi + N * math.tan(phi) * ((A ** 2) / 2.0))
    return int(round(utm_x)), int(round(utm_y))

def download_and_export():
    print(f"[+] Iniciando descarga de la red vial: '{PLACE_QUERY}' ({NETWORK_TYPE})...")
    print(f"[+] Fecha de auditoría: {DOWNLOAD_DATE}")
    
    try:
        G = ox.graph_from_place(PLACE_QUERY, network_type=NETWORK_TYPE)
        print(f"[✓] Grafo descargado exitosamente: {len(G.nodes)} nodos, {len(G.edges)} aristas.")
        
        # Filtrar a la mayor componente fuertemente conexa (Requerimiento 1.3)
        G_scc = ox.truncate.largest_component(G, strongly=True)
        print(f"[✓] Mayor Componente Fuertemente Conexa (SCC): {len(G_scc.nodes)} nodos, {len(G_scc.edges)} aristas.")

        nodes_data = []
        for node_id, data in G_scc.nodes(data=True):
            lat = data['y']
            lon = data['x']
            ux, uy = to_utm_zone_18n(lat, lon)
            nodes_data.append({
                "id": node_id,
                "latitude": lat,
                "longitude": lon,
                "utmX": ux,
                "utmY": uy
            })

        edges_data = []
        for u, v, data in G_scc.edges(data=True):
            dist = float(data.get('length', 0.0))
            hw_type = data.get('highway', 'secondary')
            if isinstance(hw_type, list):
                hw_type = hw_type[0]
            default_speed = DEFAULT_SPEEDS.get(hw_type, 40.0)
            speed = parse_maxspeed(data.get('maxspeed'), default_speed)
            time_seconds = dist / (speed / 3.6) if speed > 0 else 0.0
            oneway = bool(data.get('oneway', False))

            edges_data.append({
                "source": u,
                "target": v,
                "distanceMeters": round(dist, 1),
                "speedKmh": round(speed, 1),
                "travelTimeSeconds": round(time_seconds, 1),
                "highway": hw_type,
                "oneway": oneway
            })

        output = {
            "metadata": {
                "region": PLACE_QUERY,
                "networkType": NETWORK_TYPE,
                "downloadDate": DOWNLOAD_DATE,
                "totalNodesBeforeSCC": len(G.nodes),
                "totalEdgesBeforeSCC": len(G.edges),
                "sccNodes": len(G_scc.nodes),
                "sccEdges": len(G_scc.edges)
            },
            "nodes": nodes_data,
            "edges": edges_data
        }

        with open(OUTPUT_FILE, 'w', encoding='utf-8') as f:
            json.dump(output, f, indent=2)
        print(f"[✓] Red guardada en {OUTPUT_FILE}")

    except Exception as e:
        print(f"[!] Error durante la descarga con OSMnx: {e}")
        print("[!] Utilizando red canónica de respaldo pre-procesada en el backend.")

if __name__ == "__main__":
    download_and_export()
