# Backend Análisis de Algoritmos — Quindío 2026-2 (Spring Boot 3.4 & Java 21 LTS)

Sistema de backend para modelado territorial sobre la red vial real de **OpenStreetMap**, algoritmos de grafos, geometría computacional y optimización combinatoria en el Departamento del Quindío, Colombia. Desarrollado como proyecto final de la asignatura **Análisis y Complejidad de Algoritmos (2026-2)**.

---

## 🏛️ Arquitectura del Sistema y Estructura del Repositorio

El backend sigue una arquitectura multicapa en **Java 21 LTS** con **Spring Boot 3.4**, acompañada de los scripts de descarga y verificación en Python, las especificaciones SDD en `specs/` y los archivos de resultados experimentales en `results/`:

```text
backend-analisis/
├── specs/                        # Especificaciones SDD (Spec-Driven Development) por requerimiento
│   ├── R1_modelo_red_vial.md
│   ├── R2_caminos_minimos.md
│   ├── R3_envolvente_convexa.md
│   ├── R4_ruta_optima_atsp.md
│   └── R5_aplicacion_y_protocolo.md
├── scripts/                      # Scripts reproducibles de descarga OSM y benchmarks de librería
│   ├── download_network.py       # Descarga y exportación de la red vial de OpenStreetMap (quindio_network.json)
│   ├── measure_r1_scc_osmnx.py   # R1.3: Medición de ox.truncate.largest_component (radios 2, 5, 10, 20 km y depto)
│   └── scipy_convexhull_benchmark.py # R3: Medición y verificación exacta con scipy.spatial.ConvexHull (Qhull C)
├── results/                      # Archivos CSV y JSON generados por el protocolo experimental (Sección 6 y Anexo)
│   ├── r1_network_sizes.csv
│   ├── r1_scc_times.csv
│   ├── r1_places_mapping.csv
│   ├── r2_dijkstra_measurements.csv
│   ├── r2_ratio_summary.csv
│   ├── r2_time_matrix.csv
│   ├── r3_convex_hull.csv
│   ├── r4_exact_times.csv
│   ├── r4_heuristic_quality.csv
│   ├── r4_day_itinerary.csv
│   └── experiments_results.json
└── src/main/java/com/analisis/backend/
    ├── algorithm/                # Implementaciones algorítmicas propias desde cero
    │   ├── convexhull/
    │   │   ├── QuickHull2D.java
    │   │   └── scipy/SciPyBenchmarkRunner.java
    │   ├── scc/
    │   │   └── TarjanScc.java
    │   ├── shortestpath/
    │   │   ├── DijkstraLinear.java
    │   │   └── DijkstraPriorityQueue.java
    │   └── tsp/
    │       ├── BruteForceTsp.java
    │       ├── HeldKarpTsp.java
    │       ├── GreedyTsp.java
    │       └── DayItineraryPlanner.java
    ├── config/                   # Configuración global de CORS
    ├── controller/               # Controladores REST (/api/v1/network, /places, /routes, /convex-hull, /tsp, /experiments)
    ├── service/                  # Contratos de servicio e implementaciones (service/impl/)
    ├── repository/               # Persistencia del catálogo (PlaceRepository)
    ├── model/                    # Modelos en memoria (RoadNetwork, RoadNode, RoadEdge, NetworkDataHolder) y entidades JPA
    ├── dto/                      # DTOs de petición y respuesta
    ├── util/                     # DataLoader y utilidades geodésicas (GeoUtils: WGS84 a UTM Zona 18N)
    └── exception/                # Manejador global de excepciones
```

---

## 🧠 Algoritmos y Modelo de Datos

1. **Requerimiento 1 — Modelo de la Red Vial de OpenStreetMap & Componente Fuertemente Conexa**:
   - **Red vial descargada (`quindio_network.json`)**: $V = 37\,117$ nodos y $E = 73\,615$ aristas dirigidas de OpenStreetMap (`network_type="drive"`, fecha de descarga `2026-10-17T10:00:00-05:00`).
   - **Estimación de memoria (R1.1)**: Bajo la fórmula del modelo ($24V + 8V^2$ bytes vs. $24V + 48E$ bytes en JVM 64 bits), la **Matriz de Adyacencia** requeriría **10,511.65 MB**, mientras que las **Listas de Adyacencia** ocupan únicamente **4.22 MB** ($\approx 2\,491.3\times$ menos memoria).
   - **Velocidades y Pesos (R1.2)**: Se utiliza la etiqueta `maxspeed` de OpenStreetMap cuando está registrada ($20 \dots 100$ km/h); en tramos sin `maxspeed`, se imputa por etiqueta `highway` (`motorway: 80`, `trunk: 70`, `primary: 60`, `secondary: 50`, `tertiary: 40`, `unclassified: 35`, `residential: 30`, `living_street: 20` km/h; tramos con superficie destapada `surface=unpaved/dirt/gravel/compacted` se limitan a `20 km/h`). El tiempo de viaje en segundos es $t = \text{length} / (\text{speed} / 3.6)$.
   - **Componente Fuertemente Conexa (R1.3)**: Se mide `ox.truncate.largest_component(G, strongly=True)` en Python con OSMnx sobre los radios de 2, 5, 10 y 20 km alrededor de la Plaza de Bolívar de Armenia y el departamento completo (`scripts/measure_r1_scc_osmnx.py`), y se extrae en Java mediante implementación propia del **Algoritmo de Tarjan** (`TarjanScc.java`, $\Theta(V + E)$), conservando $35\,442$ nodos y $70\,837$ aristas en la mayor componente fuertemente conexa.
   - **Asociación de los 42 lugares (R1.4)**: Cada lugar se asocia al nodo de la SCC más cercano en línea recta en coordenadas métricas UTM Zona 18N (`EPSG:32618`).

2. **Requerimiento 2 — Caminos Mínimos & Matriz de Tiempos Asimétrica**:
   - **Dijkstra Lineal (`DijkstraLinear.java`)**: Búsqueda lineal del mínimo en $\Theta(V^2)$.
   - **Dijkstra con Cola de Prioridad (`DijkstraPriorityQueue.java`)**: Extracción con Min-Heap en $O((V + E)\log V)$.
   - **Matriz Asimétrica $42 \times 42$**: Precalculada ejecutando `computeOneToAll` desde cada lugar sobre la SCC de OpenStreetMap y exportada a `results/r2_time_matrix.csv`.

3. **Requerimiento 3 — Envolvente Convexa (`QuickHull2D.java`)**:
   - Enfoque Divide y Vencerás sobre coordenadas enteras de 64 bits en UTM Zona 18N, descartando duplicados y puntos colineales sobre los lados del polígono.
   - Verificado al 100 % en cantidad y coordenadas de vértices contra `scipy.spatial.ConvexHull` (`scripts/scipy_convexhull_benchmark.py` y `SciPyBenchmarkRunner.java`) sobre los $35\,442$ nodos de la SCC ($17$ vértices) y cuadrados aleatorios de $1\,000$, $10\,000$ y $50\,000$ puntos con semilla fija `42`.

4. **Requerimiento 4 — Ruta Óptima ATSP e Itinerario de 11 Horas**:
   - **Fuerza Bruta (`BruteForceTsp.java`, $\Theta(k!)$)** y **Bellman-Held-Karp (`HeldKarpTsp.java`, $\Theta(2^k k^2)$ en tiempo y $\Theta(2^k k)$ en memoria)** evaluados para $k = 5 \dots 12$ desde Armenia (`ARM_CAB`) con semilla fija `42`.
   - **Heurística Voraz (`GreedyTsp.java`, $\Theta(k^2)$)** evaluada en $k \in \{5, 8, 10, 12\}$ con 5 instancias por cada $k$.
   - **Planificador de Jornada de 11 Horas (`DayItineraryPlanner.java`)** con poda justificada y recálculo óptimo con Held-Karp.

---

## 🔁 Reproducción de la Descarga de Datos y los Experimentos

### 1. Reproducir descarga y procesamiento de la red vial de OpenStreetMap
```bash
pip install osmnx networkx scipy numpy
python scripts/download_network.py
```
Genera `src/main/resources/data/quindio_network.json`.

### 2. Reproducir mediciones R1.3 con OSMnx (`ox.truncate.largest_component`)
```bash
python scripts/measure_r1_scc_osmnx.py
```
Genera `results/r1_network_sizes.csv` y `results/r1_scc_times.csv`.

### 3. Reproducir benchmark R3 con `scipy.spatial.ConvexHull`
```bash
python scripts/scipy_convexhull_benchmark.py
```
Genera `src/main/resources/data/experiments/scipy_benchmark_seed42.json`.

### 4. Reproducir el protocolo experimental completo R1–R4 en Java 21 y exportar CSV/JSON
```bash
.\mvnw.cmd test "-Dtest=QuickHull2DTest,TarjanSccTest,DijkstraTest,GeoUtilsTest,ExperimentSuiteGeneratorTest"
```
Verifica todos los algoritmos y actualiza todos los archivos en `results/` y `src/main/resources/data/experiments/experiments_results.json`.

### 5. Iniciar el Servidor Backend
```bash
.\mvnw.cmd spring-boot:run
```
El servidor quedará disponible en `http://localhost:8080`.

---

## 👥 Equipo de Trabajo

- **Samuel Ortiz** (`Samuxll0`) — *Arquitectura Spring Boot, catálogo y Requerimiento 1 (Red vial OSM y componente conexa).*
- **Samuel Zuluaga Castaño** (`samuel26zc`) — *Requerimiento 2 (Dijkstra lineal y cola de prioridad, Matriz Asimétrica) y Requerimiento 3 (QuickHull 2D vs SciPy).*
- **Juan Pablo Mejia Parada** (`jpelefantino`) — *Requerimiento 4 (ATSP Fuerza Bruta, Held-Karp, Heurística Voraz e Itinerario de 11h) y Requerimiento 5 (Aplicación y Laboratorio Experimental).*
