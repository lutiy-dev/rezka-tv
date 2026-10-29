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
    fun usesEdgeAwareRowsOnPhone() {
        val actual = ResponsiveLayout.useEdgeAwareRows(360)
        val expected = true
        assertThat("phone rows stay inside screen edges", actual, `is`(expected))
    }

    @Test
    fun keepsTvOutOfPhoneMode() {
        val actual = ResponsiveLayout.isPhone(720)
        val expected = false
        assertThat("TV-sized device is not phone mode", actual, `is`(expected))
    }

    @Test
    fun usesTouchCatalogOnCompactScreens() {
        val actual = ResponsiveLayout.useMobileCatalog(360)
        val expected = true
        assertThat("compact screen uses touch grid", actual, `is`(expected))
    }

    @Test
    fun preservesTvCatalogOnWideScreens() {
        val actual = ResponsiveLayout.useMobileCatalog(1280)
        val expected = false
        assertThat("TV screen keeps Leanback grid", actual, `is`(expected))
    }

    @Test
    fun keepsPhoneRowsInsideTouchMode() {
        val actual = ResponsiveLayout.useMobileCatalog(400)
        val expected = true
        assertThat("compact screen has touch shelves", actual, `is`(expected))
    }

    @Test
    fun keepsTvRowsOnLeanback() {
        val actual = ResponsiveLayout.useMobileCatalog(800)
        val expected = false
        assertThat("wide screen keeps D-pad shelves", actual, `is`(expected))
    }

    @Test
    fun usesTwoSearchColumnsOnPortraitPhone() {
        val actual = ResponsiveLayout.searchColumns(392)
        val expected = 2
        assertThat("portrait search grid", actual, `is`(expected))
    }

    @Test
    fun usesFourSearchColumnsOnLandscapePhone() {
        val actual = ResponsiveLayout.searchColumns(800)
        val expected = 4
        assertThat("landscape search grid", actual, `is`(expected))
    }

    @Test
    fun usesThreeCatalogColumnsOnNormalPhone() {
        val actual = ResponsiveLayout.mobileCatalogColumns(392)
        val expected = 3
        assertThat("normal width uses three posters", actual, `is`(expected))
    }

    @Test
    fun usesTwoCatalogColumnsOnNarrowPhone() {
        val actual = ResponsiveLayout.mobileCatalogColumns(320)
        val expected = 2
        assertThat("narrow phone leaves readable posters", actual, `is`(expected))
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
