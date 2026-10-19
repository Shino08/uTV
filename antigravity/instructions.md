# Project Instructions - Serverless IPTV Streaming App (Mobile & Smart TV)

## 1. Objetivo del Proyecto
Desarrollar una aplicación nativa en **Kotlin**, 100% *serverless*, para reproducir canales de televisión abierta y streaming gratuito (FAST) en español y de Latinoamérica consumiendo directamente la lista pública `https://iptv-org.github.io/iptv/languages/spa.m3u`.

La aplicación debe funcionar en un único proyecto adaptable a dos factores de forma:
- **Android Mobile:** Teléfonos y Tablets (Interfaz táctil + Picture-in-Picture).
- **Android TV / Google TV:** Smart TVs y TV Boxes (Navegación 100% con control remoto D-Pad).

---

## 2. Stack Tecnológico Configurado
- **Lenguaje:** Kotlin 2.0+ con Coroutines y `StateFlow`.
- **Arquitectura:** Clean Architecture + MVVM (Model-View-ViewModel).
- **UI Mobile:** Jetpack Compose (`androidx.compose.material3`).
- **UI Smart TV:** Compose for TV (`androidx.tv.foundation` y `androidx.tv.material3`).
- **Reproductor de Video:** AndroidX Media3 (`ExoPlayer` + `media3-datasource-okhttp` + `media3-ui`).
- **Persistencia Local:** Room Database (SQLite) + Jetpack DataStore (Preferences).
- **Red e Imágenes:** OkHttp (descarga de `.m3u`) + Coil (`AsyncImage` con caché en disco).

---

## 3. Reglas de Negocio y Lógica Core

### 3.1. Ingesta y Parseo del Archivo M3U (`M3UParser`)
- Fuente de datos única: `https://iptv-org.github.io/iptv/languages/spa.m3u`.
- Ejecutar descarga y parseo siempre en `Dispatchers.IO`.
- Extraer de cada `#EXTINF`:
  - `tvg-id` (deducir `countryCode` del sufijo después del punto, ej. `EnerGeek.cl` -> `CL`).
  - `tvg-logo` (URL del logo).
  - `group-title` (mapear al español: *Animation* -> `Anime y Animación`, *Movies* -> `Cine y Series`, *Kids* -> `Infantil`, *Sports* -> `Deportes`, *News* -> `Noticias`, *Music* -> `Música`, *General* -> `Variedades`).
- Capturar directivas `#EXTVLCOPT:http-user-agent=` y `#EXTVLCOPT:http-referrer=` asociadas al stream.

### 3.2. Numeración Automática de Diales (`DialNumberAssigner`)
Asignar un número de canal estilo decodificador satelital (SimpleTV / DirecTV) según su categoría:
- **100 - 199:** Nacionales, Variedades y Entretenimiento.
- **200 - 299:** Música y Conciertos.
- **300 - 399:** Anime, Cultura Geek e Infantil.
- **500 - 599:** Cine y Series.
- **600 - 699:** Deportes.
- **700 - 799:** Noticias, Cultura y Documentales.

### 3.3. Persistencia Desacoplada en Room (TTL 24 Horas)
- No descargar el archivo `.m3u` en cada arranque si han pasado menos de 24 horas desde la última sincronización exitosa.
- Mantener **dos tablas separadas** en Room:
  1. `channels`: Se actualiza al sincronizar el `.m3u`.
  2. `user_channel_prefs`: Guarda `tvgId`, `isFavorite`, `lastWatchedTimestamp` y `watchCount`. **Nunca debe borrarse al refrescar la lista de canales.**

### 3.4. Reproductor Media3 ExoPlayer (`PlayerManager`)
- Inyectar dinámicamente las cabeceras `User-Agent` y `Referer` en `DefaultHttpDataSource.Factory` cuando el canal las requiera.
- Detectar resolución en tiempo real mediante `Player.Listener.onVideoSizeChanged` para etiquetar el stream como `FHD (1080p)`, `HD (720p)` o `SD`.
- Capturar errores HTTP `403` (mostrar estado de Geo-bloqueo) y `404` / Timeout de 8 segundos (mostrar estado de Señal fuera del aire) sin cerrar el reproductor, permitiendo seguir haciendo zapping.

---

## 4. Estructura de Paquetes Obligatoria
```text
com.iptv.app/
├── core/
│   ├── database/       # Room (ChannelDatabase, ChannelDao, Entities)
│   ├── datastore/      # AppPreferencesDataStore (TTL, lastWatchedChannel)
│   ├── network/        # M3UDownloader, M3UParser, DialNumberAssigner
│   ├── player/         # ExoPlayerManager, StreamQualityDetector
│   └── model/          # Domain models (Channel, CategoryHub, StreamErrorState)
├── ui/
│   ├── theme/          # Color.kt, Type.kt, Theme.kt
│   ├── shared/         # HomeViewModel.kt, PlayerViewModel.kt
│   ├── components/
│   │   ├── osd/        # SimpleTvInfoBanner, MiniGuideOverlay, SignalErrorOverlay
│   │   └── cards/      # ChannelCard, CategoryHubPill, HeroFeaturedBanner
│   ├── mobile/         # MobileHomeScreen, MobilePlayerScreen
│   └── tv/             # TvHomeScreen, TvPlayerScreen
└── MainActivity.kt     # Detección de FEATURE_LEANBACK (TV vs Mobile)