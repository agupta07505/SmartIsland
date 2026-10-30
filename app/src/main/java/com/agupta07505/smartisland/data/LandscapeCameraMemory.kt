/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.data

/**
 * Android can still report the old cutout just after a turn.
 * Before the settled read, a missing cutout falls back to the last one for the same screen setup,
 * i.e. the same rotation and size.
 */
class LandscapeCameraMemory {
    private data class Key(val reversed: Boolean, val widthDp: Int, val heightDp: Int)

    private val seen = HashMap<Key, LandscapeCamera>()

    fun resolve(read: OrientationState, settled: Boolean): OrientationState {
        if (!read.isLandscape) return read
        val key = Key(read.reversed, read.screenWidthDp.toInt(), read.screenHeightDp.toInt())
        val camera = read.camera
        if (settled || camera != null) {
            if (camera != null) seen[key] = camera else seen.remove(key)
            return read
        }
        return seen[key]?.let { read.copy(camera = it) } ?: read
    }
}
