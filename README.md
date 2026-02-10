# Snake Race — ARSW Lab #2 (Java 21, Virtual Threads)

**Escuela Colombiana de Ingeniería – Arquitecturas de Software**  
Laboratorio de programación concurrente: condiciones de carrera, sincronización y colecciones seguras.

---

## Requisitos

- **JDK 21** (Temurin recomendado)
- **Maven 3.9+**
- SO: Windows, macOS o Linux

---

## Cómo ejecutar

```bash
mvn clean verify
mvn -q -DskipTests exec:java -Dsnakes=4
```

- `-Dsnakes=N` → inicia el juego con **N** serpientes (por defecto 2).
- **Controles**:
  - **Flechas**: serpiente **0** (Jugador 1).
  - **WASD**: serpiente **1** (si existe).
  - **Espacio** o botón **Action**: Pausar / Reanudar.

---

## Reglas del juego (resumen)

- **N serpientes** corren de forma autónoma (cada una en su propio hilo).
- **Ratones**: al comer uno, la serpiente **crece** y aparece un **nuevo obstáculo**.
- **Obstáculos**: si la cabeza entra en un obstáculo hay **rebote**.
- **Teletransportadores** (flechas rojas): entrar por uno te **saca por su par**.
- **Rayos (Turbo)**: al pisarlos, la serpiente obtiene **velocidad aumentada** temporal.
- Movimiento con **wrap-around** (el tablero “se repite” en los bordes).

---

## Arquitectura (carpetas)

```
co.eci.snake
├─ app/                 # Bootstrap de la aplicación (Main)
├─ core/                # Dominio: Board, Snake, Direction, Position
├─ core/engine/         # GameClock (ticks, Pausa/Reanudar)
├─ concurrency/         # SnakeRunner (lógica por serpiente con virtual threads)
└─ ui/legacy/           # UI estilo legado (Swing) con grilla y botón Action
```

---

# Actividades del laboratorio

## Parte I — (Calentamiento) `wait/notify` en un programa multi-hilo

1. Toma el programa [**PrimeFinder**](https://github.com/ARSW-ECI/wait-notify-excercise).
2. Modifícalo para que **cada _t_ milisegundos**:
   - Se **pausen** todos los hilos trabajadores.
   - Se **muestre** cuántos números primos se han encontrado.
   - El programa **espere ENTER** para **reanudar**.
3. La sincronización debe usar **`synchronized`**, **`wait()`**, **`notify()` / `notifyAll()`** sobre el **mismo monitor** (sin _busy-waiting_).
4. Entrega en el reporte de laboratorio **las observaciones y/o comentarios** explicando tu diseño de sincronización (qué lock, qué condición, cómo evitas _lost wakeups_).

> Objetivo didáctico: practicar suspensión/continuación **sin** espera activa y consolidar el modelo de monitores en Java.

---

## Parte II — SnakeRace concurrente (núcleo del laboratorio)

### 1) Análisis de concurrencia

- Explica **cómo** el código usa hilos para dar autonomía a cada serpiente.
- **Identifica** y documenta en **`el reporte de laboratorio`**:
  - Posibles **condiciones de carrera**.
  - **Colecciones** o estructuras **no seguras** en contexto concurrente.
  - Ocurrencias de **espera activa** (busy-wait) o de sincronización innecesaria.

### 2) Correcciones mínimas y regiones críticas

- **Elimina** esperas activas reemplazándolas por **señales** / **estados** o mecanismos de la librería de concurrencia.
- Protege **solo** las **regiones críticas estrictamente necesarias** (evita bloqueos amplios).
- Justifica en **`el reporte de laboratorio`** cada cambio: cuál era el riesgo y cómo lo resuelves.

### 3) Control de ejecución seguro (UI)

- Implementa la **UI** con **Iniciar / Pausar / Reanudar** (ya existe el botón _Action_ y el reloj `GameClock`).
- Al **Pausar**, muestra de forma **consistente** (sin _tearing_):
  - La **serpiente viva más larga**.
  - La **peor serpiente** (la que **primero murió**).
- Considera que la suspensión **no es instantánea**; coordina para que el estado mostrado no quede “a medias”.

### 4) Robustez bajo carga

- Ejecuta con **N alto** (`-Dsnakes=20` o más) y/o aumenta la velocidad.
- El juego **no debe romperse**: sin `ConcurrentModificationException`, sin lecturas inconsistentes, sin _deadlocks_.
- Si habilitas **teleports** y **turbo**, verifica que las reglas no introduzcan carreras.

> Entregables detallados más abajo.

---

## Entregables

1. **Código fuente** funcionando en **Java 21**.
2. Todo de manera clara en **`**el reporte de laboratorio**`** con:
   - Data races encontradas y su solución.
   - Colecciones mal usadas y cómo se protegieron (o sustituyeron).
   - Esperas activas eliminadas y mecanismo utilizado.
   - Regiones críticas definidas y justificación de su **alcance mínimo**.
3. UI con **Iniciar / Pausar / Reanudar** y estadísticas solicitadas al pausar.

---

## Criterios de evaluación (10)

- (3) **Concurrencia correcta**: sin data races; sincronización bien localizada.
- (2) **Pausa/Reanudar**: consistencia visual y de estado.
- (2) **Robustez**: corre **con N alto** y sin excepciones de concurrencia.
- (1.5) **Calidad**: estructura clara, nombres, comentarios; sin _code smells_ obvios.
- (1.5) **Documentación**: **`reporte de laboratorio`** claro, reproducible;

---

## Tips y configuración útil

- **Número de serpientes**: `-Dsnakes=N` al ejecutar.
- **Tamaño del tablero**: cambiar el constructor `new Board(width, height)`.
- **Teleports / Turbo**: editar `Board.java` (métodos de inicialización y reglas en `step(...)`).
- **Velocidad**: ajustar `GameClock` (tick) o el `sleep` del `SnakeRunner` (incluye modo turbo).

---

## Cómo correr pruebas

```bash
mvn clean verify
```

Incluye compilación y ejecución de pruebas JUnit. Si tienes análisis estático, ejecútalo en `verify` o `site` según tu `pom.xml`.

---

---

## **Reporte de Laboratorio — Solución**
### Autor: Diego Fernando Chavarro Castillo

Este reporte documenta las soluciones aplicadas para garantizar la concurrencia correcta, la robustez y la consistencia del sistema SnakeRace bajo carga.

---

### 1. Análisis de Concurrencia y Data Races
Durante el análisis inicial, se identificaron condiciones de carrera críticas donde los hilos de las serpientes actualizaban su posición mientras el hilo de la UI intentaba renderizar el tablero.
- **Problema**: Acceso concurrente a la estructura del cuerpo de la serpiente. Un snapshot para la UI podía ocurrir mientras la cabeza se añadía pero la cola no se había removido, causando inconsistencias visuales y de estado.
- **Solución**: Se implementó sincronización intrínseca (`synchronized`) en la clase `Snake` para los métodos de modificación (`advance`) y consulta (`snapshot`, `head`, `getLength`). Esto garantiza que cualquier lectura de estado vea un "paso" completo y atómico del movimiento de la serpiente.

### 2. Colecciones y Estructuras Seguras
El uso de colecciones no atómicas (`HashSet`, `HashMap` y `ArrayDeque`) era la fuente principal de excepciones `ConcurrentModificationException`.
- **Cambios realizados**:
  - **Board.java**: Se sustituyeron `HashSet` y `HashMap` por `ConcurrentHashMap.newKeySet()` y `ConcurrentHashMap`. Esto permite que múltiples serpientes comprueben colisiones y consuman ítems (comida/turbo) simultáneamente sin bloqueos globales.
  - **Snake.java**: Se reemplazó `ArrayDeque` por `ConcurrentLinkedDeque` para el cuerpo. Aunque el acceso está sincronizado, el uso de una colección concurrente añade una capa extra de seguridad para iteraciones.
- **Justificación**: Estas colecciones de la librería `java.util.concurrent` ofrecen un rendimiento superior en escenarios multi-hilo comparado con sincronizar manualmente cada acceso a una colección estándar.

### 3. Eliminación de Esperas Activas (Busy-Wait)
Se identificó que los hilos `SnakeRunner` realizaban un sondeo constante (polling) del estado del reloj, desperdiciando ciclos de CPU.
- **Mecanismo de Sincronización**: Se utilizó el modelo de monitores de Java (`wait/notify`).
  - **GameClock**: Actúa como el monitor. El método `resume()` utiliza `notifyAll()` para despertar a los hilos suspendidos.
  - **SnakeRunner**: En el método `checkPause()`, los hilos entran en `clock.wait()` si el juego está pausado.
- **Resultado**: La eficiencia del sistema mejoró drásticamente; cuando el juego está pausado, los hilos de las serpientes no consumen CPU hasta que se reanuda la ejecución.

### 4. Regiones Críticas y Alcance Mínimo
Para maximizar el paralelismo, se evitó el uso de bloqueos extensos, protegiendo solo las regiones críticas estrictamente necesarias.
- **Justificación**: 
  - En `Snake.java`, la sincronización se limita a la manipulación del `Deque` de posiciones.
  - En `Board.java`, el método `step` está sincronizado para asegurar que solo una serpiente a la vez pueda interactuar con un mismo ítem (como una única pieza de comida en una posición específica), evitando que dos serpientes "coman" el mismo recurso simultáneamente.

### 5. Pausa, Consistencia y Robustez
- **Pausa y Estadísticas**: Al presionar pausa, el sistema garantiza la consistencia visual. Las estadísticas de "Longest Snake" y "Worst Snake" (capturada mediante un sistema de notificaciones `die()` -> `notifyDeath()`) se calculan sobre un estado estático y seguro.
- **Robustez (N alto)**: Gracias al uso de **Virtual Threads** (Java 21) y la sincronización localizada, el juego soporta 20+ serpientes sin degradación de rendimiento.
- **Verificación**: Se incluye la clase `verify.ConcurrencyTest` que somete al sistema a 100,000 operaciones concurrentes para validar la ausencia de `data races`.

---

## Créditos
Desarrollado como parte del laboratorio 2 de Arquitecturas de Software (ARSW).
| Componente | Estado |
| :--- | :--- |
| Concurrencia | Correcta (sin data races) |
| Pausa/Reanudar | Consistente (wait/notify) |
| Robustez | Verificada (N=20, Test de estrés) |
| Documentación | Completa |
