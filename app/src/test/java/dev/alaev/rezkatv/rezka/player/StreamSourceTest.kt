package dev.alaev.rezkatv.rezka.player

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.Test

class StreamSourceTest {
    @Test
    fun recognizesHlsWithQueryParameters() {
        assertThat("HLS query is ignored", StreamSource.isHls("https://cdn.test/stream.m3u8?token=abc"), `is`(true))
    }

    @Test
    fun recognizesHlsWithFragmentAndUppercaseExtension() {
        assertThat("HLS fragment is ignored", StreamSource.isHls("https://cdn.test/stream.M3U8#start"), `is`(true))
    }

    @Test
    fun leavesMp4AsNonHls() {
        assertThat("MP4 is not HLS", StreamSource.isHls("https://cdn.test/stream.mp4?token=abc"), `is`(false))
    }
}
