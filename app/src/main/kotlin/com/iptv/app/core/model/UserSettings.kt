package com.iptv.app.core.model

/**
 * Enums and domain representation of all user-configurable options in uTV.
 */

enum class StreamQualityPreference(val label: String, val maxHeight: Int) {
    AUTO("Automática", Int.MAX_VALUE),
    FHD_1080P("1080p (Full HD)", 1080),
    HD_720P("720p (HD)", 720),
    SD_480P("480p (SD)", 480),
}

enum class BufferDurationMode(
    val label: String,
    val minBufferMs: Int,
    val maxBufferMs: Int,
    val bufferForPlaybackMs: Int,
    val rebufferMs: Int,
) {
    SHORT("Corto (Red rápida)", 5_000, 15_000, 1_000, 1_500),
    MEDIUM("Medio (Estándar)", 10_000, 25_000, 1_500, 2_500),
    LONG("Largo (Red inestable)", 20_000, 50_000, 3_000, 5_000),
}

enum class DecoderMode(val label: String) {
    HARDWARE("Por Hardware (Recomendado)"),
    SOFTWARE("Por Software (Compatibilidad)"),
}

enum class AspectRatioPreference(val label: String) {
    FIT("Ajustar (16:9)"),
    FILL("Estirar (Pantalla completa)"),
    ZOOM("Llenar (Recortar bordes)"),
}

enum class ThemePreference(val label: String) {
    DARK("Oscuro (Recomendado)"),
    LIGHT("Claro"),
    SYSTEM("Seguir sistema"),
}

enum class FontSizePreference(val label: String, val scale: Float) {
    SMALL("Pequeño", 0.85f),
    NORMAL("Normal", 1.0f),
    LARGE("Grande", 1.25f),
}

enum class ZappingSpeedPreference(val label: String, val debounceMs: Long) {
    FAST("Rápido (Directo)", 0L),
    NORMAL("Normal (400ms)", 400L),
    CONFIRM("Con Confirmación (Presionar OK)", -1L),
}

/**
 * Immutable data holder for all active application preferences.
 */
data class UserSettings(
    // ── Playback & Video ─────────────────────────────────────────────────────
    val streamQuality: StreamQualityPreference = StreamQualityPreference.AUTO,
    val bufferMode: BufferDurationMode = BufferDurationMode.MEDIUM,
    val decoderMode: DecoderMode = DecoderMode.HARDWARE,
    val aspectRatio: AspectRatioPreference = AspectRatioPreference.FIT,
    val autoPlayNext: Boolean = false,
    val startMuted: Boolean = false,
    val defaultVolume: Int = 100,

    // ── Network & Data ───────────────────────────────────────────────────────
    val onlyWifi: Boolean = false,
    val syncOnlyWifi: Boolean = false,
    val customUserAgent: String = "",
    val connectionTimeoutSeconds: Int = 8,
    val lowDataMode: Boolean = false,

    // ── Privacy ──────────────────────────────────────────────────────────────
    val privateMode: Boolean = false,

    // ── Appearance & Controls ────────────────────────────────────────────────
    val themePreference: ThemePreference = ThemePreference.DARK,
    val fontSizePreference: FontSizePreference = FontSizePreference.NORMAL,
    val reduceAnimations: Boolean = false,
    val zappingSpeed: ZappingSpeedPreference = ZappingSpeedPreference.NORMAL,
)
