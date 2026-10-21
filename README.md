# uTV

[![Release](https://img.shields.io/github/v/release/Shino08/uTV?label=versi%C3%B3n)](https://github.com/Shino08/uTV/releases)
[![Platform](https://img.shields.io/badge/plataforma-Android%205.0%2B-green)]()
[![TV](https://img.shields.io/badge/Android%20TV-compatible-blue)]()

**uTV** es un reproductor de TV en vivo gratuito para Android y Android TV. Sincroniza listas M3U públicas de [iptv-org](https://github.com/iptv-org/iptv) y las organiza en una guía de canales con categorías, favoritos, historial y control por D-Pad.

> uTV no aloja, hospeda ni retransmite contenido. Los canales provienen de listas públicas de terceros y cada stream pertenece a su respectivo propietario.

## Descargar

Ve a la sección de [Releases](https://github.com/Shino08/uTV/releases) y descarga el APK de la última versión.

- Requiere Android 5.0 (API 21) o superior.
- Compatible con teléfonos, tablets y Android TV / Google TV.
- No requiere cuenta ni registro.

## Características

- Guía de canales con dial numérico por categoría.
- Filtros por categoría: Anime, Cine y Series, Deportes, Noticias, Música y más.
- Filtro por país.
- Favoritos y historial de "Recientemente vistos".
- Reproducción con Media3 / ExoPlayer (HLS y DASH).
- Mini-guía superpuesta sin interrumpir la reproducción.
- Picture-in-Picture en móvil.
- Control completo por D-Pad en Android TV.
- Cache local de la lista de canales (24 h) para uso offline parcial.
- Detección automática de TV vs móvil.

## Cómo compilar

Requisitos: Android Studio con JDK 17.

```bash
git clone https://github.com/Shino08/uTV.git
cd uTV
./gradlew assembleDebug
```

El APK de debug queda en `app/build/outputs/apk/debug/`.

Para compilar la versión de release:

```bash
./gradlew assembleRelease
```

## Estructura del proyecto

- `app/src/main/kotlin/com/iptv/app/core` — modelos, red, base de datos y preferencias.
- `app/src/main/kotlin/com/iptv/app/ui` — pantallas y componentes Compose (móvil y TV).
- `app/src/main/kotlin/com/iptv/app/ui/mobile` — pantallas específicas de móvil.
- `app/src/main/kotlin/com/iptv/app/ui/tv` — pantallas específicas de TV.

## Créditos

- Listas de canales: [iptv-org/iptv](https://github.com/iptv-org/iptv)
- Reproductor: [Media3 / ExoPlayer](https://github.com/androidx/media)
- HTTP: [OkHttp](https://github.com/square/okhttp)
- Imágenes: [Coil](https://github.com/coil-kt/coil)
- Base de datos: [Room](https://developer.android.com/training/data-storage/room)

## Reportar problemas

Si un canal no funciona, abre un issue con la plantilla "Canal caído". Si encuentras un bug de la app, usa "Reporte de bug". También puedes revisar el código y proponer mejoras con un Pull Request.

## Nota legal

Este proyecto es un reproductor. No proporciona, aloja ni controla ningún stream. El contenido reproducido depende de listas de terceros y puede dejar de estar disponible en cualquier momento.
