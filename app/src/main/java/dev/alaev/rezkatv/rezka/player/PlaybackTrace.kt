package dev.alaev.rezkatv.rezka.player

class PlaybackTrace(private val capacity: Int = 60) {
    private val events = ArrayDeque<String>()

    fun add(elapsedMs: Long, event: String) {
        if (events.size == capacity) events.removeFirst()
        events.addLast("${elapsedMs.coerceAtLeast(0)}ms | $event")
    }

    fun snapshot(): String = events.joinToString("\n")
}
