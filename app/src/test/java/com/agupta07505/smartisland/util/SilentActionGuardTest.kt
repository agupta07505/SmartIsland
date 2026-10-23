/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SilentActionGuardTest {
    @Test fun consumesOnceForArmedPackage() {
        SilentActionGuard.arm("rec", now = 1000)
        assertTrue(SilentActionGuard.consume("rec", now = 1500))
        assertFalse(SilentActionGuard.consume("rec", now = 1600))
    }

    @Test fun ignoresOtherPackages() {
        SilentActionGuard.arm("rec", now = 1000)
        assertFalse(SilentActionGuard.consume("other", now = 1100))
        assertTrue(SilentActionGuard.consume("rec", now = 1200))
    }

    @Test fun expires() {
        SilentActionGuard.arm("rec", now = 1000)
        assertFalse(SilentActionGuard.consume("rec", now = 1000 + SilentActionGuard.WINDOW_MS + 1))
    }

    @Test fun disarmClears() {
        SilentActionGuard.arm("rec", now = 1000)
        SilentActionGuard.disarm()
        assertFalse(SilentActionGuard.consume("rec", now = 1100))
    }
}
