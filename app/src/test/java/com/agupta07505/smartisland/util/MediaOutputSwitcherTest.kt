/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaOutputSwitcherTest {
    @Test
    fun commandTargetsSystemUiWithThePackage() {
        assertEquals(
            "am broadcast -a com.android.systemui.action.LAUNCH_MEDIA_OUTPUT_DIALOG -p com.android.systemui --es package_name com.spotify.music 2>&1",
            MediaOutputSwitcher.broadcastCommand("com.spotify.music")
        )
    }

    @Test
    fun anythingThatIsNotAPlainPackageNameIsRefused() {
        assertNull(MediaOutputSwitcher.broadcastCommand("com.a; rm -rf /"))
        assertNull(MediaOutputSwitcher.broadcastCommand("com.a b"))
        assertNull(MediaOutputSwitcher.broadcastCommand("nodots"))
        assertNull(MediaOutputSwitcher.broadcastCommand(""))
        assertNull(MediaOutputSwitcher.broadcastCommand("com.a$(id)"))
    }

    @Test
    fun successNeedsACompletedBroadcastAndNoException() {
        assertTrue(MediaOutputSwitcher.broadcastSucceeded("Broadcasting: Intent { ... }\nBroadcast completed: result=0"))
        assertFalse(MediaOutputSwitcher.broadcastSucceeded("java.lang.SecurityException: Permission Denial"))
        assertFalse(MediaOutputSwitcher.broadcastSucceeded(""))
    }

    @Test
    fun systemVariantHasNoPackage() {
        assertEquals(
            "am broadcast -a com.android.systemui.action.LAUNCH_SYSTEM_MEDIA_OUTPUT_DIALOG -p com.android.systemui 2>&1",
            MediaOutputSwitcher.systemBroadcastCommand()
        )
    }
}
