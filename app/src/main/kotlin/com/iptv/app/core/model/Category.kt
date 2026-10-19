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
         * Maps the raw [groupTitle] from #EXTINF into a [Category].
         * Normalises to lowercase and trims before comparison.
         */
        fun fromGroupTitle(groupTitle: String): Category {
            return when (groupTitle.trim().lowercase()) {
                "animation", "anime"                         -> ANIMATION
                "kids", "children", "infantil"               -> KIDS
                "movies", "series", "films", "movie"         -> MOVIES
                "sports", "sport"                            -> SPORTS
                "news", "noticias"                           -> NEWS
                "music", "música", "musica", "concerts"      -> MUSIC
                "entertainment", "general", "variedades",
                "lifestyle", "culture"                       -> GENERAL
                else                                         -> UNDEFINED
            }
        }
    }
}
