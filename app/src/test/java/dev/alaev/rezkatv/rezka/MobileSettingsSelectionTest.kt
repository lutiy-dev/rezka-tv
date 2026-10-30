package dev.alaev.rezkatv.rezka

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.Test

class MobileSettingsSelectionTest {
    @Test
    fun returnsSelectedQuality() {
        val actual = MobileSettingsSelection.index(arrayOf("auto", "720p", "1080p"), "720p")
        assertThat("stored quality points at the matching entry", actual, `is`(1))
    }

    @Test
    fun fallsBackToFirstOptionForUnknownValue() {
        val actual = MobileSettingsSelection.index(arrayOf("auto", "720p"), "legacy")
        assertThat("unknown stored value uses the first option", actual, `is`(0))
    }

    @Test
    fun handlesMissingValue() {
        val actual = MobileSettingsSelection.index(arrayOf("auto", "720p"), null)
        assertThat("missing value uses the first option", actual, `is`(0))
    }
}
