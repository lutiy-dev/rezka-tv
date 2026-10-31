package dev.alaev.rezkatv.rezka

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.Test

class MobileNavigationTest {
    @Test
    fun preservesOriginalMoreMenuSectionOrder() {
        val actual = mobileMoreSections()
        val expected = listOf(
            Section.CARTOONS,
            Section.ANIME,
            Section.FAVORITES,
            Section.TOP,
            Section.OSCAR,
            Section.SETTINGS,
        )
        assertThat("More menu order and navigation targets", actual, `is`(expected))
    }

    @Test
    fun excludesPrimaryNavigationFromMoreMenu() {
        val actual = mobileMoreSections().any {
            it in listOf(Section.HOME, Section.FILMS, Section.SERIES, Section.SEARCH)
        }
        val expected = false
        assertThat("Primary tabs are not duplicated", actual, `is`(expected))
    }
}
