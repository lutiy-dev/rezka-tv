package dev.alaev.rezkatv.rezka

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UpdateManagerTest {

    @Test
    fun newerSemanticVersionIsDetected() {
        assertTrue(UpdateManager.isNewer("1.1.0", "1.0"))
        assertTrue(UpdateManager.isNewer("v1.2.3", "1.2.2"))
        assertTrue(UpdateManager.isNewer("2.0", "1.99.99"))
    }

    @Test
    fun sameOrOlderVersionIsIgnored() {
        assertFalse(UpdateManager.isNewer("1.1.0", "1.1.0"))
        assertFalse(UpdateManager.isNewer("1.0.9", "1.1.0"))
        assertFalse(UpdateManager.isNewer("not-a-version", "1.1.0"))
    }
}
