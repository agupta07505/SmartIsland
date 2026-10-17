/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 *
 * Ported from Vorssaint (c) 2026 Vorssaint, GPL-3.0-or-later, https://github.com/vorssaint/vorssaint-utils
 */

package com.agupta07505.smartisland.buddy.model

/** A short reaction to something the island saw happen. [lengthSec] is about how long it plays. */
enum class MascotReaction(val lengthSec: Float) {
    /** Something finished well. */
    Celebrate(1.0f),
    /** Plugged in to charge, or petted. */
    Love(1.4f),
    /** A timer ran out. */
    Surprised(0.9f),
    /** A hard blink, as a camera's flash. */
    Flash(0.5f),
    /** Something failed. */
    Confused(1.2f),
    /** The phone was unlocked: it wakes up glad to see you. */
    WakeUp(1.6f),
    /** The torch let go: a yawn. */
    Yawn(1.2f),
    /** The torch came on: wide eyes and a hop. */
    Perk(1.0f),
    /** Wide eyes and two quick hops. */
    Bounce(1.0f),
    /** Eyes shut a moment as it ducks. */
    Hush(1.0f),
    /** A timer started: a determined look and a little nod. */
    Ready(1.0f),
    /** Music started: smiling eyes and a bob to the beat. */
    Groove(1.6f)
}
