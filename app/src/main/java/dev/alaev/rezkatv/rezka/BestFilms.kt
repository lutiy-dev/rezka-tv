package dev.alaev.rezkatv.rezka

import java.util.Calendar

internal object BestFilms {
    data class Selection(val year: Int, val page: Page)

    fun currentYear(): Int = Calendar.getInstance().get(Calendar.YEAR)

    fun load(year: Int, fetch: (String) -> Page): Selection {
        val current = fetch(path(year))
        if (current.cards.isNotEmpty()) return Selection(year, current)
        val previous = fetch(path(year - 1))
        return if (previous.cards.isNotEmpty()) Selection(year - 1, previous) else Selection(year, current)
    }

    private fun path(year: Int) = "/films/best/$year/"
}
