package dev.alaev.rezkatv.rezka

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.Test

class ModeSelectionTest {
    @Test
    fun resolvesTvSystemModeUsesTvUi() {
        val actual = ModeSelection.resolve(null, television = true, leanback = false)
        val expected = ModeSelection.TV
        assertThat("mode selection case 1", actual, `is`(expected))
    }

    @Test
    fun resolvesLeanbackUsesTvUi() {
        val actual = ModeSelection.resolve(null, television = false, leanback = true)
        val expected = ModeSelection.TV
        assertThat("mode selection case 2", actual, `is`(expected))
    }

    @Test
    fun resolvesOrdinaryPhoneUsesTouchUi() {
        val actual = ModeSelection.resolve(null, television = false, leanback = false)
        val expected = ModeSelection.TOUCH
        assertThat("mode selection case 3", actual, `is`(expected))
    }

    @Test
    fun resolvesTouchOverrideWinsOnTv() {
        val actual = ModeSelection.resolve(ModeSelection.TOUCH, television = true, leanback = true)
        val expected = ModeSelection.TOUCH
        assertThat("mode selection case 4", actual, `is`(expected))
    }

    @Test
    fun resolvesTvOverrideWinsOnPhone() {
        val actual = ModeSelection.resolve(ModeSelection.TV, television = false, leanback = false)
        val expected = ModeSelection.TV
        assertThat("mode selection case 5", actual, `is`(expected))
    }

    @Test
    fun resolvesAutoUsesSystemCapabilities() {
        val actual = ModeSelection.resolve(ModeSelection.AUTO, television = false, leanback = true)
        val expected = ModeSelection.TV
        assertThat("mode selection case 6", actual, `is`(expected))
    }

    @Test
    fun resolvesUnknownSavedValueFallsBackDeterministically() {
        val actual = ModeSelection.resolve("invalid", television = false, leanback = false)
        val expected = ModeSelection.TOUCH
        assertThat("mode selection case 7", actual, `is`(expected))
    }
    @Test
    fun enablesCardFocusForTvMode() {
        val actual = ModeSelection.cardFocusable(ModeSelection.TV)
        val expected = true
        assertThat("TV cards accept D-pad focus", actual, `is`(expected))
    }

    @Test
    fun disablesCardFocusForTouchMode() {
        val actual = ModeSelection.cardFocusable(ModeSelection.TOUCH)
        val expected = false
        assertThat("touch cards do not capture D-pad focus", actual, `is`(expected))
    }
}
