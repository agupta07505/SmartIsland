/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 *
 * Ported from Vorssaint (c) 2026 Vorssaint, GPL-3.0-or-later, https://github.com/vorssaint/vorssaint-utils
 */

package com.agupta07505.smartisland.buddy.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import com.agupta07505.smartisland.buddy.model.MascotTrack
import com.agupta07505.smartisland.buddy.model.MascotVisit
import com.agupta07505.smartisland.buddy.motion.MascotMotion
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Plays [visit] along the path for [track]. With [reduceMotion] it does not walk: it says its greeting
 * where it stands. A visit that ends out of sight (the idle tuck, a farewell) keeps its last frame, so the
 * buddy is not seen jumping back to its place; whoever ends the visit hides it.
 */
suspend fun MascotPose.playVisit(visit: MascotVisit, track: MascotTrack, reduceMotion: Boolean) {
    val path = MascotMotion.path(visit.kind, track)
    if (reduceMotion || path.size == 0) {
        if (path.greeting.endInclusive > path.greeting.start) {
            flashFace(visit.greeting, ((path.greeting.endInclusive - path.greeting.start) * 1000f).toInt(), true)
        }
        return
    }
    this.path = path
    visitTime.snapTo(0f)
    try {
        coroutineScope {
            if (path.greeting.endInclusive > path.greeting.start) {
                launch {
                    delay((path.greeting.start * 1000f).toLong())
                    flashFace(visit.greeting, ((path.greeting.endInclusive - path.greeting.start) * 1000f).toInt(), false)
                }
            }
            visitTime.animateTo(path.durationSec, tween((path.durationSec * 1000f).toInt(), easing = LinearEasing))
        }
    } finally {
        withContext(NonCancellable) {
            if (!visit.kind.endsOutOfSight) {
                this@playVisit.path = null
                visitTime.snapTo(0f)
            }
            settle()
        }
    }
}
