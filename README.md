# Backend Análisis de Algoritmos - Quindío 2026-2 (Spring Boot 3.4.3 & Java 21)

Sistema de backend de alto rendimiento para modelado territorial, grafos viales y optimización combinatoria en el Departamento del Quindío, Colombia. Desarrollado como proyecto final de la asignatura **Análisis y Complejidad de Algoritmos (2026-2)**.

---

## 🏛️ Arquitectura del Sistema

El backend sigue una arquitectura limpia multicapa (Layered Architecture) implementada en **Java 21 LTS** con **Spring Boot 3.4.3**, garantizando un desacoplamiento estricto entre presentación, lógica de negocio y estructuras algorítmicas de bajo nivel:

```text
src/main/java/com/analisis/backend/
├── algorithm/               # Implementaciones algorítmicas nativas desde cero (sin librerías externas de grafos)
│   ├── convexhull/          # R3: QuickHull 2D con proyección UTM 18N y producto cruz long
│   │   ├── QuickHull2D.java
│   │   └── scipy/           # SciPyBenchmarkRunner (comparación con Qhull C)
│   ├── scc/                 # R1: Algoritmo de Tarjan para Componentes Fuertemente Conexas O(V+E)
│   │   └── TarjanScc.java
│   ├── shortestpath/        # R2: Dijkstra Lineal O(V^2) y Dijkstra con Min-Heap O((V+E)log V)
│   │   ├── DijkstraLinear.java
│   │   ├── DijkstraMinHeap.java
│   │   └── TimeMatrixCalculator.java
│   └── tsp/                 # R4: Viajante Asimétrico (ATSP) y Planificador de Itinerarios
│       ├── BruteForceTsp.java
│       ├── HeldKarpTsp.java
│       ├── GreedyTsp.java
│       └── DayItineraryPlanner.java
├── config/                  # Configuración global de CORS, beans y carga de grafo
├── controller/              # Controladores REST (/api/v1/network, /places, /routes, /convexhull, /tsp, /experiments)
├── service/                 # Servicios de orquestación y contratos de negocio
│   ├── impl/                # Implementaciones de servicios
├── repository/              # Acceso a entidades de catálogo territorial (Spring Data JPA)
├── model/                   # Modelos de dominio y entidades JPA
│   ├── domain/              # Modelos en memoria: RoadNetwork, GraphNode, GraphEdge, NetworkDataHolder
│   └── entity/              # Entidades relacionales: Place
├── dto/                     # DTOs de petición y respuesta tipados
├── util/                    # Utilidades de proyección geográfica (GeoUtils: WGS84 a UTM Zona 18N)
└── exception/               # Manejador global de excepciones (@RestControllerAdvice)
```

---

## 🧠 Algoritmos Implementados desde Cero

1. **Requerimiento 1 — Modelo de la Red Vial & Componente Conexa**:
   - Descarga e indexación de la red vial de OpenStreetMap (`osmnx` filtro `drive`).
   - Grafo dirigido $G = (V, E)$ representado en **Listas de Adyacencia** (1.97 MB frente a 1,675 MB de la matriz de adyacencia).
   - Detección de la Componente Fuertemente Conexa (SCC) principal mediante el **Algoritmo de Tarjan** $O(V + E)$ con numeración DFS y pila de ancestros.
   - Proyección de 42 atractivos y cabeceras hacia el nodo vial más cercano en UTM 18N.
2. **Requerimiento 2 — Caminos Mínimos & Matriz de Tiempos Asimétrica**:
   - **Dijkstra Lineal** $O(V^2)$ con escaneo de distancias sobre arreglo.
   - **Dijkstra con Min-Heap Binario** $O((V + E)\log V)$ con parada temprana al extraer el destino.
   - Verificación del invariante formal $d[v] \le d[u] + w(u, v)$ y monotonicidad de la cola de prioridad.
   - Cálculo de la matriz asimétrica de tiempos $42 \times 42$ (1,764 trayectos) vía One-to-All.
3. **Requerimiento 3 — Envolvente Convexa (QuickHull 2D)**:
   - Enfoque divide y vencerás sobre coordenadas UTM 18N redondeadas a enteros con producto cruz entero de 64 bits (`long`).
   - Eliminación estricta de puntos colineales intermedios y ordenamiento antihorario (CCW).
   - Recurrencia $T(n) = 2T(n/2) + O(n) \implies O(n \log n)$ en caso promedio, contrastada contra `scipy.spatial.ConvexHull` (Qhull en C).
4. **Requerimiento 4 — Viajante Asimétrico (ATSP) & Planificador de 11 Horas**:
   - Solución exacta mediante **Bellman-Held-Karp** con programación dinámica y máscaras de bits $O(2^k k^2)$.
   - Búsqueda exhaustiva por **Fuerza Bruta** $O(k!)$ con control de timeout a 5 minutos.
   - **Heurística Voraz** del vecino más cercano $O(k^2)$ y análisis de brecha de calidad.
   - Planificador de jornada turística con **poda sistemática** de atractivos para no exceder 11 horas (660 minutos), garantizando la optimalidad del subcircuito restante.

---

## 📡 Endpoints de la API REST (`/api/v1`)

| Módulo | Método | Endpoint | Descripción |
|---|---|---|---|
| **R1 Red** | `GET` | `/api/v1/network/stats` | Estadísticas del grafo, análisis de memoria y tiempos de Tarjan |
| **R1 Red** | `GET` | `/api/v1/network/scc` | Componente fuertemente conexa calculada con Tarjan |
| **R1 Lugares** | `GET` | `/api/v1/places` | Catálogo de los 42 lugares (cabeceras, atractivos, fincas, acopios) |
| **R2 Rutas** | `POST` | `/api/v1/routes/shortest-path` | Camino mínimo entre dos lugares (Linear vs MinHeap) |
| **R2 Rutas** | `POST` | `/api/v1/routes/compare-versions` | Comparativa directa de complejidad e invariante |
| **R2 Rutas** | `GET` | `/api/v1/routes/time-matrix` | Matriz asimétrica de tiempos $42 \times 42$ |
| **R2 Rutas** | `GET` | `/api/v1/routes/time-matrix/csv` | Descarga de la matriz en formato CSV |
| **R3 QuickHull** | `GET` | `/api/v1/convexhull` | Envolvente convexa de la red y lugares en UTM 18N |
| **R3 QuickHull** | `GET` | `/api/v1/convexhull/benchmarks` | Benchmarks experimentales vs SciPy Qhull |
| **R4 ATSP** | `POST` | `/api/v1/tsp/solve` | Solución del ATSP (Held-Karp, Fuerza Bruta o Voraz) |
| **R4 ATSP** | `POST` | `/api/v1/tsp/compare` | Comparación exacta de las tres metodologías |
| **R4 Itinerario**| `POST` | `/api/v1/tsp/itinerary` | Itinerario de un día (11h) con algoritmo de poda |
| **R5 Experimentos**| `GET`| `/api/v1/experiments/all` | Ejecución canónica del protocolo experimental del anexo |

---

## 🚀 Requisitos y Puesta en Marcha

### Requisitos del Sistema
- **Java Development Kit (JDK)**: 21 LTS (Oracle OpenJDK, Eclipse Temurin o similar)
- **Maven**: 3.9+ (incluido wrapper `mvnw` y `mvnw.cmd`)
- Memoria RAM recomendada: 4 GB o superior

### 1. Compilación y Pruebas Unitarias
```bash
# En Windows:
.\mvnw.cmd clean test-compile
.\mvnw.cmd test -Dtest="QuickHull2DTest,TarjanSccTest,DijkstraTest,GeoUtilsTest"

# En Linux/macOS:
./mvnw clean test-compile
./mvnw test -Dtest="QuickHull2DTest,TarjanSccTest,DijkstraTest,GeoUtilsTest"
```

### 2. Iniciar el Servidor Backend
```bash
# En Windows:
.\mvnw.cmd spring-boot:run

# En Linux/macOS:
./mvnw spring-boot:run
```
El servidor quedará disponible en `http://localhost:8080`.

---

## 👥 Equipo de Trabajo

- **Samuel Ortiz** (`Samuxll0`) — *Arquitectura Spring Boot, entidades, catálogo y Requerimiento 1 (Red y Tarjan SCC).*
- **Samuel Zuluaga Castaño** (`samuel26zc`) — *Requerimiento 2 (Dijkstra lineal y heap, Matriz Asimétrica) y Requerimiento 3 (QuickHull 2D UTM 18N).*
- **Juan Pablo Mejia Parada** (`jpelefantino`) — *Requerimiento 4 (ATSP exacto Held-Karp, Fuerza Bruta, Heurística Voraz y Poda de 11h) y Requerimiento 5 (Laboratorio Experimental).*
