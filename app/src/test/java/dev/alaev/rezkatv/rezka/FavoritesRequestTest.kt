package dev.alaev.rezkatv.rezka

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.Test

class FavoritesRequestTest {
    @Test
    fun buildsAddPostRequestWithSelectedCollection() {
        val expected = listOf("post_id" to "123", "cat_id" to "7", "action" to "add_post")
        val actual = FavoritesRequest.addPost("123", "7")
        assertThat("request includes exact post and selected collection", actual, `is`(expected))
    }

    @Test
    fun rejectsNonNumericCollectionIds() {
        val expected = true
        val actual = runCatching { FavoritesRequest.addPost("123", "../danger") }.isFailure
        assertThat("collection id validation rejects paths", actual, `is`(expected))
    }

    @Test
    fun usesYearlessBestFilmsRoute() {
        val actual = Category.BEST.path(Sort.POPULAR)
        val expected = "/films/best/"
        assertThat("Best films route is yearless", actual, `is`(expected))
    }

    @Test
    fun routesBestMenuThroughCategoryGrid() {
        val actual = Section.TOP.category
        val expected = Category.BEST
        assertThat("Best section opens category grid", actual, `is`(expected))
    }
}
