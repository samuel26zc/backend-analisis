# Especificación SDD — Requerimiento 4: Ruta Óptima de Recorrido (ATSP) e Itinerario de 11 Horas

## 1. Descripción y Comportamiento Esperado
Resolver el Problema del Viajante Asimétrico (ATSP) sobre la matriz asimétrica de tiempos del Requerimiento 2 partiendo de un lugar origen (por defecto Armenia, `ARM_CAB`), visitando $k$ lugares adicionales y regresando al origen:
1. **Fuerza Bruta (`BruteForceTsp`)**: Evaluación exhaustiva de las $k!$ permutaciones con corte de tiempo a los 5 minutos. Complejidad: $\Theta(k!)$ en tiempo, $O(k)$ en memoria.
2. **Programación Dinámica de Bellman-Held-Karp (`HeldKarpTsp`)**:
   - Subestructura óptima: Si el camino óptimo desde $0$ que visita $S$ y termina en $j$ tiene a $i$ como penúltimo nodo, entonces el subcamino desde $0$ que visita $S \setminus \{j\}$ y termina en $i$ es óptimo.
   - Recurrencia: $C(S, j) = \min_{i \in S \setminus \{j\}} \{ C(S \setminus \{j\}, i) + d(i, j) \}$ con caso base $C(\{j\}, j) = d(0, j)$.
   - Complejidad: $\Theta(2^k k^2)$ en tiempo y $\Theta(2^k k)$ en memoria ($12 \cdot k \cdot 2^k$ bytes en tabla DP + padre).
3. **Heurística Voraz (`GreedyTsp`)**: Vecino más cercano desde el nodo actual en $\Theta(k^2)$ tiempo y $O(k)$ memoria.
4. **Planificador de Jornada de 11 Horas (`DayItineraryPlanner`)**: Ordena entre 6 y 8 atractivos con Held-Karp, suma tiempos de viaje y tiempos de visita, y si supera 660 min (11 h) poda iterativamente el atractivo con **mayor duración estimada de visita (`visitDurationMinutes`)** que causa el exceso sobre las 11 h, recalculando el circuito óptimo con Held-Karp hasta que la jornada total sea $\le 660\text{ min}$.

## 2. Protocolo Experimental y Casos de Prueba
- **Soluciones exactas (`results/r4_exact_times.csv`)**: $k = 5 \dots 12$ desde `ARM_CAB` con semilla fija `42`, verificando coincidencia exacta de costo óptimo entre Fuerza Bruta y Held-Karp para todo $k$ completado por ambos.
- **Calidad de la heurística (`results/r4_heuristic_quality.csv`)**: $k \in \{5, 8, 10, 12\}$ con 5 instancias aleatorias por cada $k$ (semilla fija `42`), reportando brecha porcentual promedio y máxima respecto a Held-Karp.
- **Itinerario diario (`results/r4_day_itinerary.csv`)**: Tabla detallada parada por parada con tiempos de desplazamiento, visita y acumulado en horas.
