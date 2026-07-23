/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertNotNull
import org.junit.Test

class OemAutostartUtilTest {

    @Test
    fun testOpenAutostartSettingsFallback() {
        val context = mockk<Context>()
        every { context.packageName } returns "com.agupta07505.smartisland"
        every { context.startActivity(any()) } answers { }

        val result = OemAutostartUtil.openAutostartSettings(context)
        assertNotNull(result)
    }
}
