package dev.alaev.rezkatv.rezka.player

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.Test

class StreamSourceTest {
    @Test
    fun recognizesHlsWithQueryParameters() {
        val url = "https://cdn.test/stream.m3u8?token=abc"
        val actual = StreamSource.isHls(url)
        val expected = true
        assertThat("HLS query is ignored", actual, `is`(expected))
    }

    @Test
    fun recognizesHlsWithFragmentAndUppercaseExtension() {
        val url = "https://cdn.test/stream.M3U8#start"
        val actual = StreamSource.isHls(url)
        val expected = true
        assertThat("HLS fragment is ignored", actual, `is`(expected))
    }

    @Test
    fun recognizesSignedHlsUrlWithQueryAndFragment() {
        val url = "https://cdn.test/stream.m3u8?token=abc123&expires=999#playback"
        val actual = StreamSource.isHls(url)
        val expected = true
        assertThat("signed HLS URLs remain HLS in baseline playback", actual, `is`(expected))
    }

    @Test
    fun leavesMp4AsNonHls() {
        val url = "https://cdn.test/stream.mp4?token=abc"
        val actual = StreamSource.isHls(url)
        val expected = false
        assertThat("MP4 is not HLS", actual, `is`(expected))
    }
}
