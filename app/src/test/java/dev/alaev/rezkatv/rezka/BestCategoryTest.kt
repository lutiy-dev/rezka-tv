package dev.alaev.rezkatv.rezka

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.Test

class BestCategoryTest {
    @Test
    fun usesYearlessBestFilmsUrl() {
        val actual = Category.BEST.path(Sort.POPULAR)
        val expected = "/films/best/"
        assertThat("Best films route must not depend on calendar year", actual, `is`(expected))
    }

    @Test
    fun menuRoutesBestThroughCategoryGrid() {
        val actual = Section.TOP.category
        val expected = Category.BEST
        assertThat("Best menu uses live category grid", actual, `is`(expected))
    }
}
