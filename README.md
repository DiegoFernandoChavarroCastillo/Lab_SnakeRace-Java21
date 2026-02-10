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

## **Solución**
### Autor: Diego Fernando Chavarro Castillo

En esta sección estan documentados los procesos de las soluciones propuestas (4 problemas) y el progreso de las soluciones aplicadas a los problemas identificados en el laboratorio.

### PROBLEMA 1: Colecciones NO Thread-Safe (Resuelto)
Identifique que uso de colecciones estándar (`HashSet`, `HashMap` y `ArrayDeque`) causaba excepciones `ConcurrentModificationException` bajo carga moderada/alta.

**Acciones tomadas:**
- **Board.java**: Reemplace `HashSet` y `HashMap` por versiones thread-safe:
  - `mice`, `obstacles` y `turbo` ahora utilizan `ConcurrentHashMap.newKeySet()`.
  - `teleports` utiliza `ConcurrentHashMap`.
- **Snake.java**: Reemplace `ArrayDeque` por `ConcurrentLinkedDeque` para el cuerpo de la serpiente, permitiendo iteraciones (snapshots) seguras mientras la serpiente avanza.

**Verificación:**
- Cree un test de estrés ([ConcurrencyTest.java](file:///c:/Users/chava/OneDrive/Escritorio/Tareas/ARWS/Lab02/Lab_SnakeRace-Java21/src/main/java/co/eci/snake/verify/ConcurrencyTest.java)) que realiza 100,000 operaciones simultáneas de lectura y escritura sin fallos.

---

### PROBLEMA 2: Falta de Sincronización en Snake (Resuelto)
Identifique que, aunque las colecciones eran seguras (Problema 1), las operaciones de la serpiente (avanzar, snapshot, obtener cabeza) no eran atómicas entre sí.

**Acciones tomadas:**
- **Snake.java**: Se sincronizaron los métodos `advance`, `snapshot` y `head`.
  - Esto nos garantiza que un `snapshot` (leido por la UI) vea un estado consistente de la serpiente (el cuerpo y la longitud) sin estados intermedios corruptos mientras la serpiente avanza.
  - Se asegura la visibilidad y atomicidad de la variable `maxLength` dentro del bloque sincronizado de `advance`.

**Verificación:**
- El test de estres ([ConcurrencyTest.java](file:///c:/Users/chava/OneDrive/Escritorio/Tareas/ARWS/Lab02/Lab_SnakeRace-Java21/src/main/java/co/eci/snake/verify/ConcurrencyTest.java)) fue actualizado para verificar la integridad de los snapshots bajo carga, confirmando que no hay excepciones ni inconsistencias.

### PROBLEMA 3: Sistema de Pausa Roto (Resuelto)
Observe que los hilos `SnakeRunner` seguían ejecutándose en segundo plano aunque el reloj estuviera pausado, lo que causaba que las serpientes se movieran sin que el usuario viera la actualización hasta reanudar.

**Acciones tomadas:**
- **GameClock.java**: Transforme el GameClock en un monitor de sincronización. Sincronice los métodos `pause()` y `resume()`, y este último ahora llama a `notifyAll()`.
- **SnakeRunner.java**: Ahora recibe la instancia de `GameClock`. En su ciclo principal, verifica si el juego está pausado y, de ser así, entra en estado `wait()` sobre el objeto del reloj.
- **SnakeApp.java**: Actualice para pasar la instancia del reloj a los hilos de las serpientes al inicio.

**Verificación:**
- Al presionar **Action** o **Espacio**, todas las serpientes se detienen inmediatamente. Al reanudar, continúan su curso sin saltos de posición.

### PROBLEMA 4: Lectura Inconsistente de Estado (Resuelto)
Se resolvió la necesidad de leer el estado de todas las serpientes de forma atómica al pausar para mostrar estadísticas precisas.

**Acciones tomadas:**
- **Snake.java**: Se añadió un estado `alive` y un método `getLength()`.
- **SnakeRunner.java**: Ahora detecta choques con obstáculos, cambia el estado de la serpiente a muerta y notifica a la aplicación.
- **SnakeApp.java**: Implementa `notifyDeath` para capturar cuál serpiente murió primero. Al pausar, calcula la serpiente viva más larga y muestra un cuadro de diálogo con las estadísticas (`Longest Snake` y `Worst Snake`).
  - La sincronización del Reloj (Problema 3) garantiza que este cálculo se haga sobre un estado "congelado", sin interferencia de los hilos de las serpientes.

**Verificación:**
- Al pausar el juego, aparece un mensaje informando cuál es la serpiente más larga en ese momento y cuál fue la primera en morir, coincidiendo siempre con el estado visual del tablero.

---

## Créditos
