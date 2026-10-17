/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 *
 * Ported from Vorssaint (c) 2026 Vorssaint, GPL-3.0-or-later, https://github.com/vorssaint/vorssaint-utils
 */

package com.agupta07505.smartisland.buddy.runtime

import com.agupta07505.smartisland.buddy.model.MascotReaction

/**
 * Things the island saw happen that the buddy can react to (and the Settings preview can act out).
 * A moment with no [reaction] is not a reaction; Phase 3 adds a visit.
 */
enum class MascotMoment(val reaction: MascotReaction?) {
    Charging(MascotReaction.Love),
    BatteryLow(MascotReaction.Yawn),
    TimerStarted(MascotReaction.Ready),
    TimeIsUp(MascotReaction.Surprised),
    DownloadFinished(MascotReaction.Celebrate),
    DownloadFailed(MascotReaction.Confused),
    Music(MascotReaction.Groove),
    TorchOn(MascotReaction.Perk),
    TorchOff(MascotReaction.Yawn),
    ScreenRecording(MascotReaction.Ready),
    Unlock(MascotReaction.WakeUp),
    /** The last seconds of a timer: the buddy comes out to watch it run out. Not a reaction. */
    CountdownStart(null),
    /** The timer it watched was paused or cancelled: back behind the camera. Not a reaction. */
    CountdownCancel(null)
}
