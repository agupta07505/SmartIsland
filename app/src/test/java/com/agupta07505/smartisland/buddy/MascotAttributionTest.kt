/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.buddy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MascotAttributionTest {
    @Test
    fun noticeNamesAuthorLicenceAndSource() {
        assertTrue(MascotAttribution.NOTICE.startsWith("Ported from Vorssaint (c) 2026 Vorssaint"))
        assertTrue(MascotAttribution.NOTICE.contains("GPL-3.0-or-later"))
        assertTrue(MascotAttribution.NOTICE.endsWith(MascotAttribution.SOURCE_URL))
    }

    @Test
    fun sourceUrlIsTheUpstreamRepository() {
        assertEquals("https://github.com/vorssaint/vorssaint-utils", MascotAttribution.SOURCE_URL)
    }
}
