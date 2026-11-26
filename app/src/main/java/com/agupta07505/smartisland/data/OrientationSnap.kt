/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.data

/** Keeps island size changes immediate briefly after rotation, while window and configuration updates settle. */
class OrientationSnap(private val windowMs: Long = RotationMorph.SETTLE_WINDOW_MS) {
    private var landscape: Boolean? = null
    private var edge: Boolean? = null
    private var turnedAtMs: Long = Long.MIN_VALUE / 2

    /**
     * Call on every composition. Returns whether size changes snap now
     * @param isLandscape the window's configuration orientation
     * @param edgeCameraResolved the orientation read produced an edge camera (it exists only in landscape)
     */
    fun observe(isLandscape: Boolean, edgeCameraResolved: Boolean, nowMs: Long): Boolean {
        val seenLandscape = landscape
        val seenEdge = edge
        if (seenLandscape != null && seenEdge != null && (seenLandscape != isLandscape
            || seenEdge != edgeCameraResolved)) {
            turnedAtMs = nowMs
        }
        landscape = isLandscape
        edge = edgeCameraResolved
        return nowMs - turnedAtMs < windowMs
    }
}
