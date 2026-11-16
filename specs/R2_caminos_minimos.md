# Especificación SDD — Requerimiento 2: Caminos Mínimos y Matriz de Tiempos

## 1. Descripción y Comportamiento Esperado
Implementar desde cero el algoritmo de Dijkstra en dos versiones sobre la mayor componente fuertemente conexa (SCC) de la red vial de OpenStreetMap del Quindío usando el tiempo de viaje como peso:
1. **Versión Lineal (`DijkstraLinear`)**: Búsqueda del nodo no visitado con menor distancia tentativa mediante escaneo lineal en arreglo en $O(V^2)$.
2. **Versión con Cola de Prioridad (`DijkstraPriorityQueue`)**: Extracción del mínimo mediante Min-Heap binario en $O((V + E) \log V)$.

Ambas versiones se detienen al extraer el nodo destino, cuentan cada nodo como explorado una única vez (al salir de la frontera) y reconstruyen la secuencia de nodos de la ruta a partir del mapa de predecesores.

## 2. Invariante y Condición de Correctitud
- **Invariante**: Al inicio de cada iteración, para todo nodo $u \in S$ (conjunto de nodos ya extraídos/explorados), $d[u] = \delta(s, u)$ es el tiempo mínimo definitivo desde el origen $s$ hasta $u$.
- **Condición sobre los pesos**: $\forall (u, v) \in E, \, w(u, v) \ge 0$ (no negatividad de los tiempos de viaje). Permite detenerse al extraer el destino $t$ porque cualquier camino alternativo desde $s$ hasta $t$ debe cruzar la frontera $V \setminus S$ por algún nodo $x$ con $d[x] \ge d[t]$, y al sumar pesos no negativos nunca puede mejorar $d[t]$.

## 3. Protocolo Experimental y Casos de Prueba
- **Mediciones (`results/r2_dijkstra_measurements.csv`)**: 20 pares origen-destino del catálogo medidos con `cola_prioridad` (5 repeticiones, mediana en ms) y 10 de esos pares (lugares cercanos) medidos también con `lineal`.
- **Cociente (`results/r2_ratio_summary.csv`)**: Mediana, mínimo y máximo de $T_{\text{lineal}} / T_{\text{cola\_prioridad}}$ en los 10 pares medidos con ambas versiones.
- **Matriz Asimétrica (`results/r2_time_matrix.csv`)**: Matriz $42 \times 42$ calculada ejecutando `DijkstraPriorityQueue.computeOneToAll` una vez desde cada lugar sin detenerse en un destino.
- **Verificación (`DijkstraTest`)**: Comprueba que ambas versiones entreguen exactamente el mismo tiempo de viaje ($\Delta < 10^{-3}$ s) en todos los pares evaluados.
