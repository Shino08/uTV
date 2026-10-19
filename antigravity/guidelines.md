# UI/UX & Component Guidelines

## 1. Filosofía Visual
La interfaz combina dos mundos visuales complementarios:
1. **Exploración de Catálogo (Home):** Inspirada en **Max (HBO)**, **Disney+** y **Crunchyroll**. Fondos oscuros profundos, *Brand Hubs* interactivos y un *Hero Banner* cinemático.
2. **Experiencia de Televisión en Vivo (OSD & Guía):** Inspirada en **SimpleTV / DirecTV**. Numeración de diales clara, barra inferior informativa traslúcida (*Info Bar*) y mini-guía superpuesta que nunca interrumpe la reproducción del canal actual.

---

## 2. Paleta de Colores (Dark Mode Exclusivo)
No utilizar negro puro (`#000000`) en contenedores ni fondos de menú para evitar fatiga visual.

- `BackgroundPrimary`: `#0B0C10` (Deep Space Dark - Fondo base estilo Max).
- `BackgroundSecondary`: `#121521` (Rich Navy Dark - Fondo de secciones y barras laterales).
- `SurfaceCard`: `#1C2030` (Superficie de tarjetas en reposo).
- `SurfaceGlassOSD`: `#D90F121C` (`85%` de opacidad para el Banner OSD y Mini-Guía sobre el video).
- `AccentAnimeOrange`: `#F47521` (Naranja Crunchyroll - Foco principal en Hub de Anime y bordes activos).
- `AccentElectricBlue`: `#0063E5` (Azul Disney+/DirecTV - Indicadores del OSD, barra de progreso y sintonización).
- `LiveBadgeRed`: `#E50914` (Indicador de transmisión "EN VIVO").
- `TextPrimary`: `#F9F9F9` (Títulos, nombres de canales y números de dial).
- `TextSecondary`: `#A0A6B8` (Metadatos, país, categoría y resolución).
- `BorderSubtle`: `#26FFFFFF` (Borde de `1dp` blanco al `15%` de opacidad para efecto *Glassmorphism*).

---

## 3. Tipografía y Jerarquía
Usar `Inter` o `Roboto` (Sans-Serif limpia de alta legibilidad a 3 metros de distancia en TV):
- **Dial Number (OSD / Guía):** `28sp`, `FontWeight.ExtraBold`, `Monospace` o números tabulares (ej. `301`).
- **Hero Title:** `24sp` (Mobile) / `32sp` (TV), `FontWeight.Bold`.
- **Section Header (Carruseles):** `18sp` (Mobile) / `22sp` (TV), `FontWeight.SemiBold`.
- **Card Title:** `14sp` (Mobile) / `16sp` (TV), `FontWeight.Medium`, máximo 1 línea (`TextOverflow.Ellipsis`).
- **Badges (HD / FHD / País):** `11sp`, `FontWeight.Bold`, letras mayúsculas.

---

## 4. Reglas de Componentes Reutilizables

### 4.1. `ChannelCard` (Proporción 16:9)
- **Estructura:** Contenedor horizontal 16:9 con fondo `SurfaceCard` y esquinas redondeadas (`12.dp`).
- **Contenido:**
  - Centro: Logo del canal (`AsyncImage` con `ContentScale.Fit` y `padding(16.dp)`). Si no tiene logo, mostrar iniciales estilizadas.
  - Esquina superior izquierda: Badge con el número de dial (ej. `301`).
  - Esquina superior derecha: Código/Bandera del país (ej. `CL`, `VE`, `MX`).
  - Parte inferior: Nombre del canal y categoría.
- **Comportamiento en Smart TV (D-Pad Focus):**
  - Escala animada a `1.08f` cuando `isFocused == true`.
  - Borde iluminado de `2.dp` con color `AccentAnimeOrange` o `AccentElectricBlue`.

### 4.2. `CategoryHubPill` (Estilo Disney+ Hubs)
- Botones horizontales anchos ubicados debajo del *Hero Banner* para filtrar rápido:
  - **Anime & Geek** (Gradiente sutil naranja `#F47521`).
  - **Cine & Series** (Gradiente púrpura/azul estilo Max).
  - **Deportes** (Gradiente verde/azul).
  - **TV por País** (Selector rápido de Latinoamérica / España).
  - **Favoritos** (Icono de estrella dorada).

### 4.3. `SimpleTvInfoBanner` (Banner Inferior OSD del Reproductor)
- **Visibilidad:** Se muestra durante **4 segundos** al cambiar de canal o al presionar `OK / Centro`, con animación `slideInVertically` + `fadeIn`.
- **Distribución en 3 bloques (Estilo SimpleTV / DirecTV):**
  1. **Bloque Izquierdo:** Número de dial gigante (`301`) + Logo del canal en contenedor cuadrado redondeado.
  2. **Bloque Central:** Nombre del canal en grande, píldora de categoría (`Anime y Animación`), país y etiqueta roja `● EN VIVO`.
  3. **Bloque Derecho:** Hora actual (`HH:mm`), badge de calidad detectada por ExoPlayer (`1080p FHD` / `720p HD`) e icono de Favorito (`★`).

### 4.4. `MiniGuideOverlay` (Guía en Vivo sin cortar el video)
- Panel lateral izquierdo traslúcido (`SurfaceGlassOSD`) que ocupa el `35%` del ancho de la pantalla en TV (o *BottomSheet* en móvil) mientras el canal actual sigue reproduciéndose de fondo.
- Lista vertical rápida ordenada por número de dial (`100` al `799`) con pestañas rápidas por categoría.

## 5. Reglas de Navegación (TV vs. Mobile)
- **En Smart TV:**
  - Prohibido depender de gestos táctiles. Todo componente interactivo debe ser alcanzable con `D-Pad Up`, `Down`, `Left`, `Right` y `Center/Enter`.
  - Dentro del reproductor a pantalla completa: `D-Pad Up` sube un canal (`dial + 1`), `D-Pad Down` baja un canal (`dial - 1`), `D-Pad Left` abre `MiniGuideOverlay`, y `D-Pad Center` muestra `SimpleTvInfoBanner`.
- **En Mobile:**
  - Barra de navegación inferior (`NavigationBar`) con: *Inicio*, *Guía TV (Diales)*, *Anime Hub* y *Favoritos*.
  - En el reproductor: *Swipe vertical* cambia de canal y botón dedicado para entrar en modo *Picture-in-Picture (PiP)*.
