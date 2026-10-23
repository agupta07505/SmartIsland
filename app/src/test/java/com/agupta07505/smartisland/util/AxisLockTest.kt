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

class AxisLockTest {
    @Test
    fun staysOpenInsideTheSlop() {
        val lock = AxisLock(slopPx = 20f)
        assertFalse(lock.update(10f, 12f))
        assertNull(lock.axis)
        assertEquals(5f, lock.filterX(5f), 0f)
        assertEquals(5f, lock.filterY(5f), 0f)
    }

    @Test
    fun verticalDragIgnoresSidewaysMovement() {
        val lock = AxisLock(slopPx = 20f)
        assertTrue(lock.update(6f, 25f))
        assertEquals(AxisLock.Axis.Vertical, lock.axis)
        assertEquals(0f, lock.filterX(30f), 0f)
        assertEquals(7f, lock.filterY(7f), 0f)
    }

    @Test
    fun horizontalDragIgnoresVerticalMovement() {
        val lock = AxisLock(slopPx = 20f)
        assertTrue(lock.update(-30f, 8f))
        assertEquals(AxisLock.Axis.Horizontal, lock.axis)
        assertEquals(0f, lock.filterY(-40f), 0f)
        assertEquals(-7f, lock.filterX(-7f), 0f)
    }

    @Test
    fun theLockNeverFlipsWithinOneTouch() {
        val lock = AxisLock(slopPx = 20f)
        lock.update(0f, 30f)
        assertFalse(lock.update(200f, 30f))
        assertEquals(AxisLock.Axis.Vertical, lock.axis)
    }
}
