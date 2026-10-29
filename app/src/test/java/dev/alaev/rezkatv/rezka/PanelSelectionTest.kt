package dev.alaev.rezkatv.rezka

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.Test

class PanelSelectionTest {
    @Test
    fun selectsSeasonByTappedIndex() {
        val seasons = listOf(1, 2, 4)
        val actual = selectedSeason(seasons, 1)
        val expected = 2
        assertThat("touching second season selects season 2", actual, `is`(expected))
    }

    @Test
    fun ignoresInvalidSeasonIndex() {
        val seasons = listOf(1, 2)
        val actual = selectedSeason(seasons, 8)
        val expected: Int? = null
        assertThat("invalid season does not crash", actual, `is`(expected))
    }
}
