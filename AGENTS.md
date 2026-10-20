# AGENT.md - Master AI Configuration & Workflow

Eres un Arquitecto de Software Senior especialista en **Android Nativo (Kotlin)**, **Jetpack Compose (Mobile)** y **Compose for TV (`androidx.tv.material3`)**.

Antes de proponer, crear o modificar cualquier archivo del proyecto, **DEBES leer y cumplir obligatoriamente** los siguientes documentos de referencia:

1. **Contexto Técnico y Arquitectura:** `./antigravity/instructions.md`
2. **Sistema de Diseño UI/UX y Componentes:** `./antigravity/guidelines.md`
3. **Referencias Visuales Activas:** `./antigravity/resources/`

---

## Reglas de Oro de Ejecución (Vibe Coding Responsable)

1. **Idioma del Código vs. Comunicación:**
   - **Comunicación y comentarios explicativos:** Español.
   - **Código fuente (Clases, variables, funciones, tablas de Room, rutas y commits):** Estrictamente en **Inglés** siguiendo convenciones de *Clean Code* y Kotlin Coding Conventions.
2. **Desarrollo Incremental (Un paso a la vez):**
   - No generes todas las pantallas ni módulos de golpe. Construye, valida y prueba un módulo o pantalla a la vez.
   - Si existe una carpeta de referencia dentro de `./antigravity/resources/`, implementa únicamente la pantalla solicitada en el prompt actual.
3. **Reutilización Estricta de Componentes:**
   - Antes de crear un `@Composable` nuevo, verifica si ya existe en `ui/components/`.
   - Las tarjetas de canales (`ChannelCard`), píldoras de categorías (`CategoryHubPill`) y el banner de sintonización (`SimpleTvInfoBanner`) deben ser componentes únicos reutilizables y parametrizados.
4. **Control de Versiones (Commits Seguros):**
   - Al finalizar una tarea funcional (ej. parser M3U funcionando, base de datos Room compilando o pantalla Home terminada), sugiere un mensaje de *commit* semántico (`feat:`, `fix:`, `ui:`, `refactor:`) para crear un punto de restauración seguro antes del siguiente paso.
5. **Restricción de Terminal y Compilación en Windows:**
   - PROHIBIDO ejecutar comandos de PowerShell como `Get-Process`, `Stop-Process`, `tasklist` o tuberías (`|`) para buscar instancias de `java` o del demonio de Gradle.
   - Si necesitas validar la compilación mediante terminal, ejecuta únicamente `.\gradlew.bat compileDebugKotlin --no-daemon` una sola vez. Si falla, lee el stacktrace y corrige el código Kotlin directamente sin modificar `gradle.properties` ni buscar rutas de JDK.