package dev.alaev.rezkatv.rezka

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.Test

class NavRailTest {
    @Test
    fun menuRoutesBestThroughCategoryGrid() {
        val actual = Section.TOP.category
        val expected = Category.BEST
        assertThat("Best menu uses live category grid", actual, `is`(expected))
    }
}
