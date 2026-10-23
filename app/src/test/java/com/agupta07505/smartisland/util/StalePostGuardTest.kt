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

class StalePostGuardTest {

    @Test
    fun olderRingingSnapshotIsStaleOnceTheOngoingOneWasPosted() {
        val guard = StalePostGuard()
        guard.notePosted("call", 100L) // ringing
        guard.notePosted("call", 200L) // ongoing
        assertTrue(guard.isStale("call", 100L))
        assertFalse(guard.isStale("call", 200L))
    }

    @Test
    fun snapshotOfARemovedNotificationIsStale() {
        val guard = StalePostGuard()
        guard.notePosted("call", 200L)
        guard.noteRemoved("call", 200L)
        assertTrue(guard.isStale("call", 200L))
    }

    @Test
    fun aNewPostUnderTheSameKeyAfterRemovalIsFine() {
        val guard = StalePostGuard()
        guard.notePosted("call", 200L)
        guard.noteRemoved("call", 200L)
        guard.notePosted("call", 300L)
        assertFalse(guard.isStale("call", 300L))
    }

    @Test
    fun unknownKeysAndOtherKeysAreNeverStale() {
        val guard = StalePostGuard()
        guard.notePosted("a", 500L)
        assertFalse(guard.isStale("b", 1L))
    }

    @Test
    fun clearForgetsEverything() {
        val guard = StalePostGuard()
        guard.notePosted("call", 200L)
        guard.noteRemoved("call", 200L)
        guard.clear()
        assertFalse(guard.isStale("call", 100L))
    }

    @Test
    fun aDismissalStopsASlowHandlerFromBringingTheCardBack() {
        // The user dismissed the card (noted at "now" = 1000) while an older post (950) was still being read.
        val guard = StalePostGuard()
        guard.notePosted("msg", 950L)
        guard.noteRemoved("msg", 1_000L)
        assertTrue(guard.isStale("msg", 950L))
        // The app posting the same key again after the dismissal is a new notification and must show.
        guard.notePosted("msg", 1_200L)
        assertFalse(guard.isStale("msg", 1_200L))
    }
}
