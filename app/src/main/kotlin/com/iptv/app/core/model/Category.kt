package com.iptv.app.core.model

/**
 * Domain category keys — language-agnostic internal identifiers.
 * Display strings live in [Category.displayName].
 */
enum class Category(
    val displayName: String,
    val dialRangeStart: Int,
    val dialRangeEnd: Int,
) {
    GENERAL("Variedades", 100, 199),
    MUSIC("Música", 200, 299),
    ANIMATION("Anime y Animación", 300, 399),
    KIDS("Infantil", 300, 399),
    MOVIES("Cine y Series", 500, 599),
    SPORTS("Deportes", 600, 699),
    NEWS("Noticias", 700, 799),
    UNDEFINED("Otros", 800, 899);

    companion object {
        /**
         * Maps the raw [groupTitle] (which may contain semicolon-separated tags like "Animation;Kids")
         * and [channelName] from #EXTINF into a [Category].
         */
        fun fromGroupTitle(groupTitle: String, channelName: String = ""): Category {
            val tokens = groupTitle.lowercase()
                .split(";", ",", "/")
                .map { it.trim() }

            val nameLower = channelName.lowercase()

            return when {
                tokens.any { it in listOf("animation", "anime") } ||
                    nameLower.contains("anime") ||
                    nameLower.contains("energeek") ||
                    nameLower.contains("otaku") ||
                    nameLower.contains("locomotion") ||
                    nameLower.contains("senpai") ||
                    nameLower.contains("animax") -> ANIMATION

                tokens.any { it in listOf("kids", "children", "infantil", "family") } ||
                    nameLower.contains("kids") ||
                    nameLower.contains("infantil") ||
                    nameLower.contains("cartoon") ||
                    nameLower.contains("nick") ||
                    nameLower.contains("junior") -> KIDS

                tokens.any { it in listOf("movies", "series", "films", "movie", "cinema", "cine") } ||
                    nameLower.contains("hbo") ||
                    nameLower.contains("cine") ||
                    nameLower.contains("pelicula") ||
                    nameLower.contains("películas") ||
                    nameLower.contains("film") ||
                    nameLower.contains("movie") ||
                    nameLower.contains("golden") ||
                    nameLower.contains("space") ||
                    nameLower.contains("tnt") ||
                    nameLower.contains("star ") ||
                    nameLower.contains("warner") ||
                    nameLower.contains("universal") ||
                    nameLower.contains("axn") ||
                    nameLower.contains("sony") ||
                    nameLower.contains("paramount") -> MOVIES

                tokens.any { it in listOf("sports", "sport", "deportes") } ||
                    nameLower.contains("sport") ||
                    nameLower.contains("deporte") ||
                    nameLower.contains("espn") ||
                    nameLower.contains("fox sports") ||
                    nameLower.contains("tudn") ||
                    nameLower.contains("tyc") ||
                    nameLower.contains("win ") ||
                    nameLower.contains("gol") -> SPORTS

                tokens.any { it in listOf("news", "noticias", "documentary", "legislative") } ||
                    nameLower.contains("noticias") ||
                    nameLower.contains("news") ||
                    nameLower.contains("24 horas") ||
                    nameLower.contains("cnn") ||
                    nameLower.contains("milenio") ||
                    nameLower.contains("tn ") ||
                    nameLower.contains("c5n") ||
                    nameLower.contains("ntn24") -> NEWS

                tokens.any { it in listOf("music", "música", "musica", "concerts") } ||
                    nameLower.contains("music") ||
                    nameLower.contains("música") ||
                    nameLower.contains("musica") ||
                    nameLower.contains("htv") ||
                    nameLower.contains("telehit") ||
                    nameLower.contains("mtv") -> MUSIC

                tokens.any {
                    it in listOf(
                        "entertainment", "general", "variedades",
                        "lifestyle", "culture", "comedy", "classic", "education"
                    )
                } -> GENERAL

                else -> GENERAL
            }
        }
    }
}
