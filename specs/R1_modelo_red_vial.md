# Especificación SDD — Requerimiento 1: Modelo de la Red Vial del Quindío

## 1. Descripción y Comportamiento Esperado
Modelar la red vial vehicular (`network_type="drive"`) del Departamento del Quindío descargada desde OpenStreetMap como un grafo dirigido ponderado $G = (V, E)$, extraer su mayor componente fuertemente conexa (SCC) y proyectar el catálogo de 42 lugares turísticos/productivos sobre los nodos de dicha componente en coordenadas métricas UTM Zona 18N.

## 2. Reglas de Negocio y Fórmulas
- **Representación (R1.1)**:
  - Matriz de adyacencia: $M_{\text{matriz}} = 24V + 8V^2$ bytes.
  - Listas de adyacencia: $M_{\text{listas}} = 24V + 48E$ bytes.
  - Se selecciona **Listas de Adyacencia** por tratarse de un grafo vial disperso ($E \approx 2V$).
- **Pesos y Velocidades (R1.2)**:
  - Distancia $d(u, v)$ en metros obtenida de la longitud geodésica de la vía en OpenStreetMap.
  - Velocidad $v(u, v)$ en km/h obtenida de la etiqueta `maxspeed` de OSM ($20 \le v \le 100$ km/h) o imputada según el tipo de vía `highway` (`motorway: 80`, `trunk: 70`, `primary: 60`, `secondary: 50`, `tertiary: 40`, `unclassified: 35`, `residential: 30`, `living_street: 20`; tramos destapados limitados a `20 km/h`).
  - Tiempo de viaje en segundos: $t(u, v) = \frac{d(u, v)}{v(u, v) / 3.6}$.
- **Componente Fuertemente Conexa (R1.3)**:
  - Medición en Python con `ox.truncate.largest_component(G, strongly=True)` sobre subredes radiales de 2, 5, 10 y 20 km alrededor de la Plaza de Bolívar de Armenia y el departamento completo (5 repeticiones, mediana en ms).
  - Verificación en Java con implementación propia del **Algoritmo de Tarjan** ($\Theta(V + E)$ en tiempo, $O(V)$ en memoria).
- **Asociación de Lugares (R1.4)**:
  - Cada uno de los 42 lugares se asocia al nodo de la SCC con menor distancia euclidiana en coordenadas UTM Zona 18N ($\text{EPSG:32618}$). Dos lugares cercanos pueden compartir el mismo nodo vial si es el más próximo para ambos. Se reportan los lugares a más de 500 m.

## 3. Casos de Prueba Verificables
1. `TarjanSccTest`: Verifica la identificación exacta de la mayor componente fuertemente conexa descartando nodos sumidero unidireccionales.
2. `GeoUtilsTest`: Verifica la proyección WGS84 $\to$ UTM Zona 18N y el cálculo de distancia euclidiana.
3. `ExperimentSuiteGeneratorTest`: Verifica la carga de los 37,117 nodos y 73,615 aristas de `quindio_network.json` y la exportación de `results/r1_network_sizes.csv`, `results/r1_scc_times.csv` y `results/r1_places_mapping.csv`.
