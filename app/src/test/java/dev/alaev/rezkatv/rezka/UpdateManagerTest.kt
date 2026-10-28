package dev.alaev.rezkatv.rezka

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.Test

class UpdateManagerTest {

    @Test
    fun detectsNewerSemanticVersion() {
        assertThat("newer semantic version is detected", UpdateManager.isNewer("1.1.0", "1.0"), `is`(true))
    }

    @Test
    fun detectsNewerMajorVersion() {
        assertThat("newer major version is detected", UpdateManager.isNewer("2.0", "1.99.99"), `is`(true))
    }

    @Test
    fun ignoresSameVersion() {
        assertThat("same version is ignored", UpdateManager.isNewer("1.1.0", "1.1.0"), `is`(false))
    }

    @Test
    fun ignoresOlderVersion() {
        assertThat("older version is ignored", UpdateManager.isNewer("1.0.9", "1.1.0"), `is`(false))
    }

    @Test
    fun rejectsInvalidVersion() {
        assertThat("invalid version is rejected", UpdateManager.isNewer("not-a-version", "1.1.0"), `is`(false))
    }
}
