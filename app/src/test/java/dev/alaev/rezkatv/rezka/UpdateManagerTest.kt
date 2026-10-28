package dev.alaev.rezkatv.rezka

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UpdateManagerTest {

    @Test
    fun detectsNewerSemanticVersion() {
        assertTrue(UpdateManager.isNewer("1.1.0", "1.0"))
    }

    @Test
    fun detectsNewerMajorVersion() {
        assertTrue(UpdateManager.isNewer("2.0", "1.99.99"))
    }

    @Test
    fun ignoresSameVersion() {
        assertFalse(UpdateManager.isNewer("1.1.0", "1.1.0"))
    }

    @Test
    fun ignoresOlderVersion() {
        assertFalse(UpdateManager.isNewer("1.0.9", "1.1.0"))
    }

    @Test
    fun rejectsInvalidVersion() {
        assertFalse(UpdateManager.isNewer("not-a-version", "1.1.0"))
    }
}
