package dev.alaev.rezkatv.rezka

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.Test

class ModeSelectionTest {
    @Test
    fun tvSystemModeUsesTvUi() {
        val actual = ModeSelection.resolve(null, television = true, leanback = false)
        val expected = ModeSelection.TV
        assertThat(actual, `is`(expected))
    }

    @Test
    fun leanbackUsesTvUi() {
        val actual = ModeSelection.resolve(null, television = false, leanback = true)
        val expected = ModeSelection.TV
        assertThat(actual, `is`(expected))
    }

    @Test
    fun ordinaryPhoneUsesTouchUi() {
        val actual = ModeSelection.resolve(null, television = false, leanback = false)
        val expected = ModeSelection.TOUCH
        assertThat(actual, `is`(expected))
    }

    @Test
    fun touchOverrideWinsOnTv() {
        val actual = ModeSelection.resolve(ModeSelection.TOUCH, television = true, leanback = true)
        val expected = ModeSelection.TOUCH
        assertThat(actual, `is`(expected))
    }

    @Test
    fun tvOverrideWinsOnPhone() {
        val actual = ModeSelection.resolve(ModeSelection.TV, television = false, leanback = false)
        val expected = ModeSelection.TV
        assertThat(actual, `is`(expected))
    }

    @Test
    fun autoUsesSystemCapabilities() {
        val actual = ModeSelection.resolve(ModeSelection.AUTO, television = false, leanback = true)
        val expected = ModeSelection.TV
        assertThat(actual, `is`(expected))
    }

    @Test
    fun unknownSavedValueFallsBackDeterministically() {
        val actual = ModeSelection.resolve("invalid", television = false, leanback = false)
        val expected = ModeSelection.TOUCH
        assertThat(actual, `is`(expected))
    }
}
