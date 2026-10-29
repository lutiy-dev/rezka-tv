package dev.alaev.rezkatv.rezka.player

import dev.alaev.rezkatv.rezka.Quality
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.Test

class StreamRecoveryTest {
    @Test
    fun triesNextMirrorBeforeChangingQuality() {
        val low = Quality("480p", listOf("low-a"))
        val high = Quality("1080p", listOf("high-a", "high-b"))
        val actual = StreamRecovery.next(listOf(low, high), high, setOf("1080p" to 0))
        val expected = StreamRecovery.Choice(high, 1)
        assertThat("mirror fallback", actual, `is`(expected))
    }

    @Test
    fun choosesLowerQualityAfterExhaustingMirrors() {
        val low = Quality("480p", listOf("low-a"))
        val high = Quality("1080p", listOf("high-a", "high-b"))
        val tried = setOf("1080p" to 0, "1080p" to 1)
        val actual = StreamRecovery.next(listOf(low, high), high, tried)
        val expected = StreamRecovery.Choice(low, 0)
        assertThat("quality fallback", actual, `is`(expected))
    }

    @Test
    fun stopsWhenEverySourceWasAttempted() {
        val low = Quality("480p", listOf("low-a"))
        val high = Quality("1080p", listOf("high-a", "high-b"))
        val tried = setOf("1080p" to 0, "1080p" to 1, "480p" to 0)
        val actual = StreamRecovery.next(listOf(low, high), low, tried)
        val expected: StreamRecovery.Choice? = null
        assertThat("no infinite retry", actual, `is`(expected))
    }
}
