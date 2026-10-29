package dev.alaev.rezkatv.rezka.player

import dev.alaev.rezkatv.rezka.Quality

data class StreamChoice(val quality: Quality, val mirrorIndex: Int)

object StreamRecovery {
    fun next(
        qualities: List<Quality>,
        current: Quality?,
        attempted: Set<Pair<String, Int>>,
    ): StreamChoice? {
        val height = current?.height ?: Int.MAX_VALUE
        val lower = qualities.filter { it != current && it.height <= height }.sortedByDescending { it.height }
        val higher = qualities.filter { it != current && it.height > height }.sortedBy { it.height }
        return (listOfNotNull(current) + lower + higher).firstNotNullOfOrNull { quality ->
            quality.urls.indices.firstOrNull { quality.label to it !in attempted }?.let { StreamChoice(quality, it) }
        }
    }
}
