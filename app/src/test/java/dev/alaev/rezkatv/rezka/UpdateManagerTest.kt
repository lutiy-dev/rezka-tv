package dev.alaev.rezkatv.rezka
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.Test
class UpdateManagerTest {
    @Test
    fun detectsNewerSemanticVersion() {
        val versions = "1.1.0" to "1.0"
        val actual = UpdateManager.isNewer(versions.first, versions.second)
        val expected = true
        assertThat("newer semantic version is detected", actual, `is`(expected))
    }

    @Test
    fun ignoresOlderVersion() {
        val versions = "1.0.9" to "1.1.0"
        val actual = UpdateManager.isNewer(versions.first, versions.second)
        val expected = false
        assertThat("older version is ignored", actual, `is`(expected))
    }

    @Test
    fun detectsPatchUpdate() {
        val versions = "1.1.2" to "1.1.0"
        val actual = UpdateManager.isNewer(versions.first, versions.second)
        val expected = true
        assertThat("patch update is detected", actual, `is`(expected))
    }
}
