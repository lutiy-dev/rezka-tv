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
        val versions = "1.1.4" to "1.1.3"
        val actual = UpdateManager.isNewer(versions.first, versions.second)
        val expected = true
        assertThat("next patch update is detected", actual, `is`(expected))
    }

    @Test
    fun ignoresSameVersion() {
        val versions = "1.1.4" to "1.1.4"
        val actual = UpdateManager.isNewer(versions.first, versions.second)
        val expected = false
        assertThat("same version is not newer", actual, `is`(expected))
    }

    @Test
    fun acceptsNewMajorVersion() {
        val versions = "2.0.0" to "1.9.9"
        val actual = UpdateManager.isNewer(versions.first, versions.second)
        val expected = true
        assertThat("major version update is newer", actual, `is`(expected))
    }

    @Test
    fun usesManifestWhenApiUnavailable() {
        val first = UpdateManager.Release("1.1.4", "manifest")
        val actual = UpdateManager.newest(first, null)?.version
        val expected = first.version
        assertThat("manifest remains available", actual, `is`(expected))
    }

    @Test
    fun selectsNewestReleaseSource() {
        val first = UpdateManager.Release("1.1.3", "manifest")
        val second = UpdateManager.Release("1.1.4", "api")
        val actual = UpdateManager.newest(first, second)?.version
        val expected = second.version
        assertThat("freshest update source wins", actual, `is`(expected))
    }

    @Test
    fun displaysHttpFailureStatus() {
        val actual = UpdateManager.failureReason(IllegalStateException("HTTP 404"))
        val expected = "HTTP 404"
        assertThat("HTTP errors remain identifiable", actual, `is`(expected))
    }

    @Test
    fun displaysNetworkFailureTypeWithoutExceptionDetails() {
        val actual = UpdateManager.failureReason(java.net.UnknownHostException("example.internal"))
        val expected = "UnknownHostException"
        assertThat("network failures do not reveal host-specific details", actual, `is`(expected))
    }

    @Test
    fun distinguishesEmptyReleaseFromNetworkException() {
        val actual = UpdateManager.failureReason(null)
        val expected = "No release data"
        assertThat("empty manifest is distinguishable", actual, `is`(expected))
    }
}
