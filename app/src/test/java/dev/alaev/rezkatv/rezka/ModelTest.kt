package dev.alaev.rezkatv.rezka

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.Test

class ModelTest {
    @Test
    fun usesYearlessBestFilmsUrl() {
        val actual = Category.BEST.path(Sort.POPULAR)
        val expected = "/films/best/"
        assertThat("Best films route must not depend on calendar year", actual, `is`(expected))
    }
}
