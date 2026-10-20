package com.iptv.app.core.network

import com.iptv.app.core.model.Category
import com.iptv.app.core.model.Channel

/**
 * Assigns dial numbers to parsed channels following the SimpleTV / DirecTV
 * numbering convention defined in the project spec.
 *
 * Ranges:
 *  100–199  Variedades / Nacionales (GENERAL + UNDEFINED)
 *  200–299  Música (MUSIC)
 *  300–399  Anime, Geek e Infantil (ANIMATION + KIDS)
 *  500–599  Cine y Series (MOVIES)
 *  600–699  Deportes (SPORTS)
 *  700–799  Noticias y Documentales (NEWS)
 *  800–899  Otros (UNDEFINED overflow)
 *
 * Each category gets its own sequential counter starting at the range base.
 * Channels that exceed the range cap continue into the next hundred block.
 */
object DialNumberAssigner {

    fun assign(channels: List<Channel>): List<Channel> {
        // Per-dial-range sequential counter (key by dialRangeStart so shared ranges like ANIMATION & KIDS don't duplicate dials)
        val counters = mutableMapOf<Int, Int>()

        return channels.map { channel ->
            val rangeStart = channel.category.dialRangeStart
            val current = counters.getOrDefault(rangeStart, rangeStart)
            counters[rangeStart] = current + 1
            channel.copy(dialNumber = current)
        }
    }
}
