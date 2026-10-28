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
}
