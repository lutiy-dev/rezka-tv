package dev.alaev.rezkatv.rezka

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.Test
import java.io.IOException

class BestFilmsTest {
    @Test
    fun keepsCurrentYearAndPaginationWithoutFetchingPreviousYear() {
        val paths = mutableListOf<String>()
        val page = populated("/films/best/2026/page/2/")
        val actual = BestFilms.load(2026) { path ->
            paths.add(path)
            page
        }
        val expected = BestFilms.Selection(2026, page) to listOf("/films/best/2026/")
        assertThat("current year preserves page and request count", actual to paths, `is`(expected))
    }

    @Test
    fun fallsBackExactlyOneYearWhenCurrentYearIsEmpty() {
        val paths = mutableListOf<String>()
        val previous = populated("/films/best/2026/page/2/")
        val actual = BestFilms.load(2027) { path ->
            paths.add(path)
            if (path == "/films/best/2027/") Page(emptyList(), null) else previous
        }
        val expected = BestFilms.Selection(2026, previous) to listOf("/films/best/2027/", "/films/best/2026/")
        assertThat("new year falls back with actual year and pagination", actual to paths, `is`(expected))
    }

    @Test
    fun stopsAfterTwoEmptyYearsAndKeepsCurrentYearLabel() {
        val paths = mutableListOf<String>()
        val empty = Page(emptyList(), null)
        val actual = BestFilms.load(2027) { path ->
            paths.add(path)
            empty
        }
        val expected = BestFilms.Selection(2027, empty) to listOf("/films/best/2027/", "/films/best/2026/")
        assertThat("empty lists do not trigger an unbounded search", actual to paths, `is`(expected))
    }

    @Test
    fun propagatesNetworkFailureWithoutTryingAnotherYear() {
        val paths = mutableListOf<String>()
        val error = IOException("offline")
        val actual = runCatching {
            BestFilms.load(2027) { path ->
                paths.add(path)
                throw error
            }
        }.exceptionOrNull()
        val expected = error to listOf("/films/best/2027/")
        assertThat("network error is not an empty list", actual to paths, `is`(expected))
    }

    @Test
    fun rechecksCurrentYearWhenJanuaryListBecomesAvailable() {
        val page = populated(null)
        val empty = Page(emptyList(), null)
        val january = BestFilms.load(2027) { if (it.contains("2027")) empty else page }
        val later = BestFilms.load(2027) { page }
        val expected = 2026 to 2027
        assertThat("previous year fallback is not sticky", january.year to later.year, `is`(expected))
    }

    @Test
    fun routesBestSectionToCatalogWithoutLocalImdbData() {
        val expected = Category.BEST
        assertThat("best section uses the catalog grid", Section.TOP.category, `is`(expected))
    }

    private fun populated(next: String?) =
        Page(listOf(Card("123", "/films/123-film.html", "Film", "", "", null)), next)
}
