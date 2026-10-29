package dev.alaev.rezkatv.rezka.player

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.Test

class PlaybackTraceTest {
    @Test
    fun retainsOnlyMostRecentEntries() {
        val trace = PlaybackTrace(2)
        trace.add(1, "first")
        trace.add(2, "second")
        trace.add(3, "third")
        val actual = trace.snapshot()
        val expected = "2ms | second\n3ms | third"
        assertThat("oldest event removed", actual, `is`(expected))
    }

    @Test
    fun storesRelativeTimesWithoutWallClockDates() {
        val trace = PlaybackTrace()
        trace.add(-2, "request")
        val actual = trace.snapshot()
        val expected = "0ms | request"
        assertThat("negative relative time is clamped", actual, `is`(expected))
    }
}
