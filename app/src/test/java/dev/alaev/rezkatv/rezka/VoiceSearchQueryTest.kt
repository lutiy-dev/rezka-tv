package dev.alaev.rezkatv.rezka

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class VoiceSearchQueryTest {
    @Test
    fun returnsFirstRecognizedPhrase() {
        val expected = "архитектура"
        val actual = VoiceSearchQuery.first(arrayListOf(expected, "другое"))
        assertThat("voice search uses the first result", actual, equalTo(expected))
    }

    @Test
    fun rejectsEmptyRecognition() {
        val expected: String? = null
        val actual = VoiceSearchQuery.first(arrayListOf("   "))
        assertThat("blank speech does not trigger search", actual, equalTo(expected))
    }

    @Test
    fun handlesMissingSpeechResults() {
        val expected: String? = null
        val actual = VoiceSearchQuery.first(null)
        assertThat("missing speech result is ignored", actual, equalTo(expected))
    }
}
