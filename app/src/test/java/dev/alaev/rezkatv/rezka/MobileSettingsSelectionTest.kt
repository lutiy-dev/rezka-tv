package dev.alaev.rezkatv.rezka

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.Test

class MobileSettingsSelectionTest {
    @Test
    fun returnsSelectedQuality() {
        val actual = MobileSettingsSelection.index(arrayOf("auto", "720p", "1080p"), "720p")
        val expected = 1
        assertThat("stored quality points at the matching entry", actual, `is`(expected))
    }

    @Test
    fun fallsBackToFirstOptionForUnknownValue() {
        val actual = MobileSettingsSelection.index(arrayOf("auto", "720p"), "legacy")
        val expected = 0
        assertThat("unknown stored value uses the first option", actual, `is`(expected))
    }

    @Test
    fun handlesMissingValue() {
        val actual = MobileSettingsSelection.index(arrayOf("auto", "720p"), null)
        val expected = 0
        assertThat("missing value uses the first option", actual, `is`(expected))
    }
}
