package dev.alaev.rezkatv.rezka

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.Test

class ResponsiveLayoutTest {
    @Test
    fun detectsCompactPhoneWidth() {
        val actual = ResponsiveLayout.isCompact(360)
        val expected = true
        assertThat("phone width is compact", actual, `is`(expected))
    }

    @Test
    fun detectsPhoneInLandscapeBySmallestWidth() {
        val actual = ResponsiveLayout.isPhone(360)
        val expected = true
        assertThat("phone stays phone in landscape", actual, `is`(expected))
    }

    @Test
    fun keepsTvOutOfPhoneMode() {
        val actual = ResponsiveLayout.isPhone(720)
        val expected = false
        assertThat("TV-sized device is not phone mode", actual, `is`(expected))
    }

    @Test
    fun usesTwoGridColumnsOnPhone() {
        val actual = ResponsiveLayout.gridColumns(360)
        val expected = 2
        assertThat("phone category grid uses two columns", actual, `is`(expected))
    }

    @Test
    fun keepsSixGridColumnsOnTv() {
        val actual = ResponsiveLayout.gridColumns(1280)
        val expected = 6
        assertThat("TV category grid keeps six columns", actual, `is`(expected))
    }
}
