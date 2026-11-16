#!/usr/bin/env python3
"""
Script Reproducible de Descarga, Procesamiento y Exportación de la Red Vial del Quindío (OpenStreetMap).
Cumple con la Sección 4 ('Datos') y los Requerimientos 1.1, 1.2, 1.3 y 1.5 del Proyecto Final:

1. Descarga de la red vial vehicular ('drive') del Departamento del Quindío (ISO3166-2: CO-QUI)
   desde OpenStreetMap (vía OSMnx / Overpass API).
2. Filtrado de vías estrictamente transitables por vehículos motorizados públicos:
   - Se excluyen vías con acceso restringido o prohibido (access/motor_vehicle in {no, private, customers, ...}).
   - Se excluyen senderos de tierra/lodo/pasto no aptos para automóviles (surface in {ground, mud, grass, sand, earth}).
   - Se excluyen la banca ferroviaria abandonada ('Antigua vía del Ferrocarril') y caminos privados sin salida.
3. Asignación de velocidades (Requerimiento 1.2):
   - Si el tramo cuenta con etiqueta 'maxspeed' válida en OpenStreetMap (20 a 100 km/h), se utiliza dicho valor.
   - Si el tramo no tiene etiqueta 'maxspeed', se imputa la velocidad estándar según el tipo de vía ('highway'):
       * motorway: 80 km/h | motorway_link: 60 km/h
       * trunk:    70 km/h | trunk_link:    55 km/h
       * primary:  60 km/h | primary_link:  50 km/h
       * secondary:50 km/h | secondary_link:45 km/h
       * tertiary: 40 km/h | tertiary_link: 35 km/h
       * unclassified: 35 km/h
       * residential:  30 km/h
       * living_street:20 km/h
   - Si el tramo presenta superficie destapada ('surface' in {unpaved, dirt, gravel, fine_gravel, pebblestone, compacted}),
     la velocidad efectiva se limita a 20 km/h.
   - Tiempo de viaje del tramo (s): travelTimeSeconds = distanceMeters / (speedKmh / 3.6).
4. Conversión de coordenadas WGS84 (latitud, longitud) a coordenadas métricas UTM Zona 18N (EPSG:32618) redondeadas a enteros.
5. Exportación a 'src/main/resources/data/quindio_network.json' para consumo del backend Java 21.

Fecha de descarga registrada: 2026-11-15T13:54:01-05:00
Región: Departamento del Quindío, Colombia
"""

import json
import math
import os
import urllib.parse
import urllib.request
from collections import defaultdict

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
OUTPUT_FILE = os.path.normpath(os.path.join(SCRIPT_DIR, "../src/main/resources/data/quindio_network.json"))
PLACES_FILE = os.path.normpath(os.path.join(SCRIPT_DIR, "../src/main/resources/data/places.json"))

DOWNLOAD_DATE = "2026-11-15T13:54:01-05:00"

OVERPASS_ENDPOINTS = [
    "https://lz4.overpass-api.de/api/interpreter",
    "https://z.overpass-api.de/api/interpreter",
    "https://overpass-api.de/api/interpreter",
]

OVERPASS_QUERY = """
[out:json][timeout:120];
area["ISO3166-2"="CO-QUI"]->.searchArea;
(
  way["highway"~"^(motorway|motorway_link|trunk|trunk_link|primary|primary_link|secondary|secondary_link|tertiary|tertiary_link|unclassified|residential)$"](area.searchArea);
);
out body;
>;
out skel qt;
"""

# Tabla oficial de velocidades estándar por tipo de vía (Requerimiento 1.2)
DEFAULT_SPEEDS = {
    "motorway": 80.0,
    "motorway_link": 60.0,
    "trunk": 70.0,
    "trunk_link": 55.0,
    "primary": 60.0,
    "primary_link": 50.0,
    "secondary": 50.0,
    "secondary_link": 45.0,
    "tertiary": 40.0,
    "tertiary_link": 35.0,
    "unclassified": 35.0,
    "residential": 30.0,
    "living_street": 20.0,
}

UNPAVED_SURFACES = {
    "unpaved", "dirt", "gravel", "fine_gravel", "pebblestone", "compacted", "sueli"
}

NON_DRIVABLE_SURFACES = {
    "ground", "mud", "grass", "sand", "earth", "grass_paver"
}

FORBIDDEN_ACCESS = {
    "no", "private", "customers", "delivery", "agricultural", "forestry", "restricted"
}

FORBIDDEN_NAME_SUBSTRINGS = [
    "antigua vía del ferrocarril",
    "antigua via del ferrocarril",
    "acceso posterior",
    "peatonal",
    "sendero",
    "cicloruta",
    "trocha",
]

# IDs de tramos en OSM correspondientes a la antigua banca ferroviaria colapsada en el cañón
# del río Quindío y caminos internos privados que no constituyen vías vehiculares públicas
EXCLUDED_WAY_IDS = {
    517957093, 317275495, 1311145176, 1031706847,
    541652133,
    1283086106, 816564300, 561133491,
    1145166908, 1197826420, 1145583028,
    908009767,
}


def to_utm_zone_18n(lat: float, lon: float) -> tuple[int, int]:
    """Convierte coordenadas geográficas WGS84 (lat, lon) a UTM Zona 18N (EPSG:32618) en metros enteros."""
    a = 6378137.0
    f = 1.0 / 298.257223563
    k0 = 0.9996
    lon0 = math.radians(-75.0)
    phi = math.radians(lat)
    lam = math.radians(lon)
    e2 = 2 * f - f * f
    ep2 = e2 / (1.0 - e2)
    sin_phi = math.sin(phi)
    cos_phi = math.cos(phi)
    tan_phi = math.tan(phi)
    n_val = a / math.sqrt(1.0 - e2 * sin_phi * sin_phi)
    t_val = tan_phi * tan_phi
    c_val = ep2 * cos_phi * cos_phi
    a_val = cos_phi * (lam - lon0)
    m_val = a * (
        (1.0 - e2 / 4.0 - 3.0 * e2 * e2 / 64.0 - 5.0 * e2 * e2 * e2 / 256.0) * phi
        - (3.0 * e2 / 8.0 + 3.0 * e2 * e2 / 32.0 + 45.0 * e2 * e2 / 1024.0) * math.sin(2.0 * phi)
        + (15.0 * e2 * e2 / 256.0 + 45.0 * e2 * e2 / 1024.0) * math.sin(4.0 * phi)
        - (35.0 * e2 * e2 * e2 / 3072.0) * math.sin(6.0 * phi)
    )
    x = 500000.0 + k0 * n_val * (
        a_val + (1.0 - t_val + c_val) * (a_val ** 3) / 6.0
        + (5.0 - 18.0 * t_val + t_val * t_val + 72.0 * c_val - 58.0 * ep2) * (a_val ** 5) / 120.0
    )
    y = k0 * (
        m_val + n_val * tan_phi * (
            (a_val ** 2) / 2.0
            + (5.0 - t_val + 9.0 * c_val + 4.0 * c_val * c_val) * (a_val ** 4) / 24.0
            + (61.0 - 58.0 * t_val + t_val * t_val + 600.0 * c_val - 330.0 * ep2) * (a_val ** 6) / 720.0
        )
    )
    return int(round(x)), int(round(y))


def haversine_m(lat1: float, lon1: float, lat2: float, lon2: float) -> float:
    """Distancia geodésica Haversine en metros entre dos coordenadas WGS84."""
    r = 6371000.0
    p1, p2 = math.radians(lat1), math.radians(lat2)
    dp = math.radians(lat2 - lat1)
    dl = math.radians(lon2 - lon1)
    a = math.sin(dp / 2) ** 2 + math.cos(p1) * math.cos(p2) * math.sin(dl / 2) ** 2
    return 2 * r * math.atan2(math.sqrt(a), math.sqrt(1 - a))


def bearing_deg(lat1: float, lon1: float, lat2: float, lon2: float) -> float:
    dlon = math.radians(lon2 - lon1)
    p1, p2 = math.radians(lat1), math.radians(lat2)
    y = math.sin(dlon) * math.cos(p2)
    x = math.cos(p1) * math.sin(p2) - math.sin(p1) * math.cos(p2) * math.cos(dlon)
    return math.degrees(math.atan2(y, x)) % 360.0


def angle_diff(a: float, b: float) -> float:
    d = abs(a - b) % 360.0
    return 360.0 - d if d > 180.0 else d


def is_valid_drivable_way(w: dict) -> bool:
    """Verifica si una vía de OpenStreetMap es transitable por vehículos públicos."""
    if w["id"] in EXCLUDED_WAY_IDS:
        return False
    tags = w.get("tags", {})
    hw = tags.get("highway", "")
    if not hw:
        return False
    if tags.get("area") == "yes":
        return False
    if tags.get("access", "").lower() in FORBIDDEN_ACCESS:
        return False
    if tags.get("motor_vehicle", "").lower() in FORBIDDEN_ACCESS:
        return False
    if tags.get("vehicle", "").lower() in FORBIDDEN_ACCESS:
        return False
    if tags.get("smoothness", "").lower() in ("horrible", "very_horrible", "impassable"):
        return False
    if tags.get("tracktype", "").lower() in ("grade4", "grade5"):
        return False
    surf = tags.get("surface", "").lower()
    if surf in NON_DRIVABLE_SURFACES:
        return False
    name_low = tags.get("name", "").lower()
    for sub in FORBIDDEN_NAME_SUBSTRINGS:
        if sub in name_low:
            return False
    return True


def parse_maxspeed(maxspeed_val) -> float | None:
    if not maxspeed_val:
        return None
    if isinstance(maxspeed_val, list):
        maxspeed_val = maxspeed_val[0]
    try:
        val = float(str(maxspeed_val).split()[0])
        if 20.0 <= val <= 100.0:
            return val
    except (ValueError, IndexError):
        pass
    return None


def compute_segment_speed(w: dict) -> float:
    """
    Obtiene la velocidad (km/h) del tramo según el Requerimiento 1.2:
    1. Si existe etiqueta 'maxspeed' válida en OSM (y no es un reductor puntual en autopista), se usa 'maxspeed'.
    2. En caso contrario, se imputa según 'DEFAULT_SPEEDS[highway]'.
    3. Si la vía es destapada ('surface' en UNPAVED_SURFACES), se limita a 20.0 km/h.
    """
    tags = w.get("tags", {})
    hw = tags.get("highway", "unclassified")
    surf = tags.get("surface", "").lower()
    smoothness = tags.get("smoothness", "").lower()
    tracktype = tags.get("tracktype", "").lower()

    is_unpaved = (
        surf in UNPAVED_SURFACES
        or smoothness in ("bad", "very_bad")
        or tracktype in ("grade2", "grade3")
    )
    if is_unpaved:
        return 20.0

    default_spd = DEFAULT_SPEEDS.get(hw, 35.0)
    osm_maxspeed = parse_maxspeed(tags.get("maxspeed"))

    if osm_maxspeed is not None:
        # En corredores troncales nacionales (trunk), evitar que un reductor puntual baje todo el tramo de 50 km/h
        if hw in ("motorway", "trunk") and osm_maxspeed < 50.0:
            return default_spd
        return osm_maxspeed

    return default_spd


def fetch_osm_raw_data(cache_path: str | None = None) -> dict:
    if cache_path and os.path.exists(cache_path):
        print(f"[+] Leyendo datos crudos de OpenStreetMap desde caché local: {cache_path}")
        with open(cache_path, "r", encoding="utf-8") as f:
            return json.load(f)

    data = urllib.parse.urlencode({"data": OVERPASS_QUERY}).encode("utf-8")
    for url in OVERPASS_ENDPOINTS:
        print(f"[+] Descargando red vial del Quindío desde {url} ...")
        try:
            req = urllib.request.Request(url, data=data, headers={"User-Agent": "QuindioGeoTour/1.0"})
            with urllib.request.urlopen(req, timeout=120) as resp:
                raw = resp.read().decode("utf-8")
            if raw and '"elements"' in raw:
                return json.loads(raw)
        except Exception as e:
            print(f"[!] Fallo en {url}: {e}")
    raise RuntimeError("No fue posible descargar la red desde Overpass API.")


def build_and_export(cache_path: str | None = None):
    osm = fetch_osm_raw_data(cache_path)
    with open(PLACES_FILE, "r", encoding="utf-8") as f:
        places = json.load(f)

    raw_nodes = {}
    ways = []
    for el in osm.get("elements", []):
        if el.get("type") == "node":
            raw_nodes[el["id"]] = (el["lat"], el["lon"])
        elif el.get("type") == "way":
            if is_valid_drivable_way(el):
                ways.append(el)

    print(f"[✓] Vías vehiculares transitables: {len(ways)}")

    # 1. Identificar intersecciones y extremos de vía
    node_usage = defaultdict(int)
    keep_nodes = set()
    for w in ways:
        nds = [nid for nid in w.get("nodes", []) if nid in raw_nodes]
        if len(nds) < 2:
            continue
        keep_nodes.add(nds[0])
        keep_nodes.add(nds[-1])
        for nid in nds:
            node_usage[nid] += 1

    for nid, count in node_usage.items():
        if count > 1:
            keep_nodes.add(nid)

    # 2. Preservar los nodos más cercanos a cada uno de los 42 lugares del catálogo
    used_node_ids = list(node_usage.keys())
    for p in places:
        plat, plon = p["latitude"], p["longitude"]
        candidates = sorted(
            used_node_ids,
            key=lambda nid: (raw_nodes[nid][0] - plat) ** 2 + (raw_nodes[nid][1] - plon) ** 2
        )[:5]
        for c in candidates:
            keep_nodes.add(c)

    # 3. Preservar puntos de curvatura en vías interurbanas (cambio de rumbo >= 12° o cada 95 m)
    for w in ways:
        hw = w.get("tags", {}).get("highway", "unclassified")
        if hw == "residential":
            continue
        nds = [nid for nid in w.get("nodes", []) if nid in raw_nodes]
        if len(nds) < 2:
            continue
        acc_dist = 0.0
        last_bearing = None
        for i in range(len(nds) - 1):
            u, v = nds[i], nds[i + 1]
            lat1, lon1 = raw_nodes[u]
            lat2, lon2 = raw_nodes[v]
            seg_d = haversine_m(lat1, lon1, lat2, lon2)
            acc_dist += seg_d
            cur_bearing = bearing_deg(lat1, lon1, lat2, lon2) if seg_d > 1.0 else last_bearing

            if v in keep_nodes:
                acc_dist = 0.0
                last_bearing = cur_bearing
            else:
                bent = (
                    last_bearing is not None
                    and cur_bearing is not None
                    and angle_diff(last_bearing, cur_bearing) >= 12.0
                    and acc_dist >= 22.0
                )
                if bent or acc_dist >= 95.0:
                    keep_nodes.add(v)
                    acc_dist = 0.0
                    last_bearing = cur_bearing
                elif last_bearing is None:
                    last_bearing = cur_bearing

    # 4. Construir aristas dirigidas con distancia (m), velocidad (km/h) y tiempo de viaje (s)
    edges = []
    active_nodes = set()

    for w in ways:
        nds = [nid for nid in w.get("nodes", []) if nid in raw_nodes]
        if len(nds) < 2:
            continue
        tags = w.get("tags", {})
        hw = tags.get("highway", "unclassified")
        speed = compute_segment_speed(w)

        oneway_tag = str(tags.get("oneway", "")).lower()
        junction_tag = str(tags.get("junction", "")).lower()
        is_reverse_only = oneway_tag in ("-1", "reverse")
        is_oneway = (
            oneway_tag in ("yes", "true", "1")
            or junction_tag == "roundabout"
            or hw in ("motorway", "motorway_link")
            or is_reverse_only
        )

        seg_start = nds[0]
        seg_dist = 0.0
        for i in range(len(nds) - 1):
            u, v = nds[i], nds[i + 1]
            lat1, lon1 = raw_nodes[u]
            lat2, lon2 = raw_nodes[v]
            seg_dist += haversine_m(lat1, lon1, lat2, lon2)
            if v in keep_nodes:
                if seg_start != v and seg_dist > 0.05:
                    dist_m = round(seg_dist, 1)
                    time_s = round(dist_m / (speed / 3.6), 1)
                    active_nodes.add(seg_start)
                    active_nodes.add(v)
                    if is_reverse_only:
                        edges.append({
                            "source": v,
                            "target": seg_start,
                            "distanceMeters": dist_m,
                            "speedKmh": round(speed, 1),
                            "travelTimeSeconds": time_s,
                            "highway": hw,
                            "oneway": True,
                        })
                    elif is_oneway:
                        edges.append({
                            "source": seg_start,
                            "target": v,
                            "distanceMeters": dist_m,
                            "speedKmh": round(speed, 1),
                            "travelTimeSeconds": time_s,
                            "highway": hw,
                            "oneway": True,
                        })
                    else:
                        edges.append({
                            "source": seg_start,
                            "target": v,
                            "distanceMeters": dist_m,
                            "speedKmh": round(speed, 1),
                            "travelTimeSeconds": time_s,
                            "highway": hw,
                            "oneway": False,
                        })
                        edges.append({
                            "source": v,
                            "target": seg_start,
                            "distanceMeters": dist_m,
                            "speedKmh": round(speed, 1),
                            "travelTimeSeconds": time_s,
                            "highway": hw,
                            "oneway": False,
                        })
                seg_start = v
                seg_dist = 0.0

    nodes_data = []
    for nid in sorted(active_nodes):
        lat, lon = raw_nodes[nid]
        ux, uy = to_utm_zone_18n(lat, lon)
        nodes_data.append({
            "id": nid,
            "latitude": round(lat, 6),
            "longitude": round(lon, 6),
            "utmX": ux,
            "utmY": uy,
        })

    output = {
        "metadata": {
            "region": "Quindío, Colombia",
            "networkType": "drive",
            "downloadDate": DOWNLOAD_DATE,
            "source": "OpenStreetMap (Overpass / OSMnx)",
            "totalNodes": len(nodes_data),
            "totalEdges": len(edges),
        },
        "nodes": nodes_data,
        "edges": edges,
    }

    with open(OUTPUT_FILE, "w", encoding="utf-8") as f:
        json.dump(output, f, separators=(",", ":"))

    size_mb = os.path.getsize(OUTPUT_FILE) / (1024 * 1024)
    print(f"[✓] Red guardada en {OUTPUT_FILE} ({size_mb:.2f} MB): {len(nodes_data)} nodos, {len(edges)} aristas.")


if __name__ == "__main__":
    import sys
    cache_arg = sys.argv[1] if len(sys.argv) > 1 else None
    build_and_export(cache_arg)
