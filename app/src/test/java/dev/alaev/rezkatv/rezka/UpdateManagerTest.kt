package dev.alaev.rezkatv.rezka

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.Test

class UpdateManagerTest {

    @Test
    fun detectsNewerSemanticVersion() {
        val candidate = "1.1.0"
        val current = "1.0"
        val actual = UpdateManager.isNewer(candidate, current)
        val expected = true
        assertThat("newer semantic version is detected", actual, `is`(expected))
    }

    @Test
    fun ignoresOlderVersion() {
        val candidate = "1.0.9"
        val current = "1.1.0"
        val actual = UpdateManager.isNewer(candidate, current)
        val expected = false
        assertThat("older version is ignored", actual, `is`(expected))
    }
}
