/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import com.agupta07505.smartisland.ui.pickers.PickerKinds
import com.agupta07505.smartisland.util.PickerRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PickerKindsTest {
    @Test
    fun knownKindsResolveToThemselves() {
        assertEquals(PickerKinds.APPS, PickerKinds.resolve("apps"))
        assertEquals(PickerKinds.UTILITY, PickerKinds.resolve("utility"))
    }

    @Test
    fun missingOrUnknownKindsDoNotResolve() {
        assertNull(PickerKinds.resolve(null))
        assertNull(PickerKinds.resolve(""))
        assertNull(PickerKinds.resolve("calculator"))
        assertNull(PickerKinds.resolve("Apps"))
    }

    @Test
    fun theIslandsRequestsMapToTheirWindowKinds() {
        assertEquals(PickerKinds.APPS, PickerKinds.forRequest(PickerRequest.APPS))
        assertEquals(PickerKinds.UTILITY, PickerKinds.forRequest(PickerRequest.UTILITY))
    }

    @Test
    fun anUnknownRequestOpensNothing() {
        assertNull(PickerKinds.forRequest(null))
        assertNull(PickerKinds.forRequest("settings"))
    }

    @Test
    fun everyRequestMapsToAKindTheWindowSupports() {
        listOf(PickerRequest.APPS, PickerRequest.UTILITY).forEach {
            assertEquals(PickerKinds.forRequest(it), PickerKinds.resolve(PickerKinds.forRequest(it)))
        }
    }
}
