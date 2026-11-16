# Especificación SDD — Requerimiento 5: Aplicación Demostrativa y Protocolo Experimental

## 1. Descripción y Comportamiento Esperado
Aplicación web demostrativa (React + TypeScript + Leaflet + Spring Boot 3.4 / Java 21) que consume en vivo los algoritmos propios y los archivos experimentales generados sobre la red vial de OpenStreetMap del Quindío:
1. **R1 (Territorio y Red Vial)**: Visualización de los 42 lugares sobre el mapa de OpenStreetMap, estadísticas de la SCC y estimación de memoria Matriz vs. Listas de Adyacencia.
2. **R2 (Caminos Mínimos)**: Selección de origen y destino, cálculo en tiempo real con `DijkstraPriorityQueue` y `DijkstraLinear`, trazado directo de la secuencia de nodos de OpenStreetMap en el mapa y exploración de la matriz asimétrica $42 \times 42$.
3. **R3 (Envolvente Convexa)**: Visualización del polígono convexo de la red vial en el mapa y tabla comparativa contra `scipy.spatial.ConvexHull`.
4. **R4 (ATSP e Itinerario de 11 Horas)**: Selección de origen y atractivos, comparación en vivo entre Held-Karp y la heurística voraz, y trazado del circuito en el mapa uniendo las rutas de Dijkstra entre paradas consecutivas.
5. **R5 (Laboratorio Experimental)**: Tablas y gráficas generadas directamente a partir de los resultados experimentales (`results/*.csv` y `experiments_results.json`), sin valores escritos a mano en el frontend.
