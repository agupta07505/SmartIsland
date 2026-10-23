/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import com.agupta07505.smartisland.data.LandscapeCamera
import com.agupta07505.smartisland.data.OrientationState
import com.agupta07505.smartisland.data.SmartIslandSettings

/** Landscape Lab: the matrix of landscape situations and the script played in each. */
enum class LabRotation(val label: String, val reversed: Boolean) {
    NORMAL("Normal landscape (camera left)", false),
    REVERSE("Reverse landscape (camera right)", true)
}

enum class LabIdle(val label: String, val position: String, val cameraReported: Boolean) {
    AUTO_CAMERA("Idle: camera cutout", SmartIslandSettings.LANDSCAPE_POSITION_AUTO, true),
    AUTO_NO_CAMERA("Idle: no cutout reported", SmartIslandSettings.LANDSCAPE_POSITION_AUTO, false),
    LEFT("Idle: left corner", SmartIslandSettings.LANDSCAPE_POSITION_LEFT, false),
    RIGHT("Idle: right corner", SmartIslandSettings.LANDSCAPE_POSITION_RIGHT, false),
    MIDDLE("Idle: top middle", SmartIslandSettings.LANDSCAPE_POSITION_MIDDLE, false)
}

enum class LabPill(val label: String, val position: String) {
    LEFT("Pill: left", SmartIslandSettings.LANDSCAPE_POSITION_LEFT),
    MIDDLE("Pill: middle", SmartIslandSettings.LANDSCAPE_POSITION_MIDDLE),
    RIGHT("Pill: right", SmartIslandSettings.LANDSCAPE_POSITION_RIGHT)
}

data class LabStage(val rotation: LabRotation, val idle: LabIdle, val pill: LabPill) {
    val title: String get() = "${rotation.name.lowercase()} / ${idle.name.lowercase()} / ${pill.name.lowercase()}"
}

/** What the lab forces while it plays: the stage's layout choices plus the merge style of the moment. */
data class LabOverride(
    val stage: LabStage,
    val mergeStyle: String = com.agupta07505.smartisland.util.MergeStyle.ID_SLIDE
) {
    /** Stored settings with the stage's layout applied and everything that could hide or end a demo card switched off. */
    fun applyTo(raw: SmartIslandSettings): SmartIslandSettings = raw.copy(
        showInLandscape = true,
        disabledEvents = emptySet(),
        disabledEventsLandscape = emptySet(),
        noExpandPackages = emptySet(),
        landscapeCirclePosition = stage.idle.position,
        landscapeCircleXAdjust = 0f,
        landscapeCircleYAdjust = 0f,
        landscapePillPosition = stage.pill.position,
        landscapeXOffset = 0f,
        mergeStyle = mergeStyle,
        mergeStyleLandscape = mergeStyle,
        // The lab puts cards away itself; the idle timer must not end a step early.
        idleTimeoutMs = LAB_IDLE_TIMEOUT_MS
    )

    /** Display state with the stage's rotation and camera applied; portrait is left alone. */
    fun applyTo(state: OrientationState): OrientationState {
        if (!state.isLandscape) return state
        val reversed = stage.rotation.reversed
        val camera = if (stage.idle.cameraReported) {
            LandscapeCamera(
                xOffsetDp = (if (reversed) 1f else -1f) * (state.screenWidthDp / 2f - LAB_CAMERA_INSET_DP).coerceAtLeast(0f),
                centerYDp = LAB_CAMERA_CENTER_Y_DP
            )
        } else {
            null
        }
        return state.copy(
            camera = camera,
            rotationSide = if (reversed) SmartIslandSettings.LANDSCAPE_POSITION_RIGHT
            else SmartIslandSettings.LANDSCAPE_POSITION_LEFT,
            reversed = reversed
        )
    }

    companion object {
        const val LAB_IDLE_TIMEOUT_MS = 10 * 60 * 1000
        const val LAB_CAMERA_INSET_DP = 28f
        const val LAB_CAMERA_CENTER_Y_DP = 20f
    }
}

fun allLabStages(): List<LabStage> =
    LabRotation.entries.flatMap { r -> LabIdle.entries.flatMap { i -> LabPill.entries.map { p -> LabStage(r, i, p) } } }

fun labStages(rotation: LabRotation?, idle: LabIdle?, pill: LabPill?): List<LabStage> =
    allLabStages().filter {
        (rotation == null || it.rotation == rotation) &&
            (idle == null || it.idle == idle) &&
            (pill == null || it.pill == pill)
    }

/** Everything a step can ask for; the runner turns these into cards, ViewModel commands and settings changes. */
sealed interface LabAction {
    data class Post(val card: LabCard, val autoExpand: Boolean = false) : LabAction
    data class Remove(val card: LabCard) : LabAction
    data object RemoveAll : LabAction
    data object Expand : LabAction
    data object Collapse : LabAction
    data object OpenPicker : LabAction
    data object ClosePicker : LabAction
    data object TapIdleDot : LabAction
    data class SetMerge(val style: String) : LabAction
    data class Wait(val ms: Long) : LabAction
}

/** Sample cards for the script; apps differ on purpose (one app, one cutout). */
enum class LabCard {
    MUSIC, MESSAGE, DOWNLOAD,
    SIBLING_A, SIBLING_B, SIBLING_C,
    CALL, PLUG
}

data class LabStep(val id: String, val title: String, val actions: List<LabAction>, val holdMs: Long, val quick: Boolean)

private const val BUBBLE = com.agupta07505.smartisland.util.MergeStyle.ID_BUBBLE
private const val SLIDE = com.agupta07505.smartisland.util.MergeStyle.ID_SLIDE

/** The pill exit plays 360 ms (x animation speed); the interrupted exit brings the card back in the middle of it. */
const val LAB_INTERRUPT_AFTER_MS = 170L

/**
 * The script. Each step starts from the state the previous one left; the list begins and ends with nothing on the
 * island. [quick] steps are the short run: entry, expand, exit.
 */
fun labSteps(): List<LabStep> = listOf(
    LabStep("idle", "Idle circle", listOf(LabAction.RemoveAll), 1400, quick = true),
    LabStep("enter", "Card arrives: circle gives way, pill slides in", listOf(LabAction.Post(LabCard.MUSIC)), 2000, quick = true),
    LabStep("expand", "Expand", listOf(LabAction.Expand), 2400, quick = true),
    LabStep("collapse", "Collapse", listOf(LabAction.Collapse), 1400, quick = false),
    LabStep(
        "second_slide", "Second app: secondary cutout, slide merge",
        listOf(LabAction.SetMerge(SLIDE), LabAction.Post(LabCard.MESSAGE)), 2400, quick = false
    ),
    LabStep(
        "second_bubble", "Second app again: bubble merge",
        listOf(
            LabAction.Remove(LabCard.MESSAGE), LabAction.Wait(900), LabAction.SetMerge(BUBBLE),
            LabAction.Post(LabCard.MESSAGE)
        ), 2800, quick = false
    ),
    LabStep("third", "Third app: tertiary cutout", listOf(LabAction.Post(LabCard.DOWNLOAD)), 2400, quick = false),
    LabStep(
        "expand_many", "Expand with three apps: corner pill shows the other card",
        listOf(LabAction.Expand), 2600, quick = false
    ),
    LabStep(
        "siblings", "Same-app siblings: picker opens away from the camera",
        listOf(
            LabAction.Collapse, LabAction.Wait(500), LabAction.RemoveAll, LabAction.Wait(900),
            LabAction.Post(LabCard.SIBLING_A), LabAction.Post(LabCard.SIBLING_B), LabAction.Post(LabCard.SIBLING_C),
            LabAction.Wait(900), LabAction.Expand, LabAction.Wait(700), LabAction.OpenPicker
        ), 2800, quick = false
    ),
    LabStep(
        "picker_close", "Picker closes, island collapses",
        listOf(LabAction.ClosePicker, LabAction.Wait(500), LabAction.Collapse), 1400, quick = false
    ),
    LabStep(
        "call", "Incoming call opens expanded",
        listOf(LabAction.RemoveAll, LabAction.Wait(900), LabAction.Post(LabCard.CALL, autoExpand = true)), 2800, quick = false
    ),
    LabStep(
        "popup", "Short event waits behind a live card (no popup)",
        listOf(
            LabAction.Collapse, LabAction.Wait(500), LabAction.RemoveAll, LabAction.Wait(900),
            LabAction.Post(LabCard.MUSIC), LabAction.Wait(1400), LabAction.Post(LabCard.PLUG)
        ), 3200, quick = false
    ),
    LabStep("exit", "Last card goes: pill slides out, circle opens", listOf(LabAction.RemoveAll), 2200, quick = true),
    LabStep(
        "interrupt", "Card returns in the middle of the exit (reverses in place)",
        listOf(
            LabAction.Post(LabCard.MUSIC), LabAction.Wait(1600), LabAction.RemoveAll,
            LabAction.Wait(LAB_INTERRUPT_AFTER_MS), LabAction.Post(LabCard.MUSIC)
        ), 2400, quick = false
    ),
    LabStep(
        "dot_tap", "Expanded, then tap the idle circle: island and secondary merge first, then the pill leaves (as long as the opening)",
        listOf(LabAction.Expand, LabAction.Wait(1500), LabAction.TapIdleDot), 3000, quick = false
    ),
    LabStep("end", "Clear", listOf(LabAction.RemoveAll), 600, quick = true)
)

fun labStepsFor(quick: Boolean): List<LabStep> = labSteps().filter { !quick || it.quick }

fun labStageDurationMs(quick: Boolean): Long = labStepsFor(quick).sumOf { step ->
    step.holdMs + step.actions.filterIsInstance<LabAction.Wait>().sumOf { it.ms }
}

/** Exit direction the pill is expected to take in [stage] on a screen [screenWidthDp] wide (what to watch for). */
fun expectedExitDirection(stage: LabStage, screenWidthDp: Float, pillWidthDp: Float, edgeInsetDp: Float): PillExitDirection {
    val cornerX = (screenWidthDp / 2f - edgeInsetDp - pillWidthDp / 2f).coerceAtLeast(0f)
    val reversed = stage.rotation.reversed
    val side = when (stage.pill.position) {
        SmartIslandSettings.LANDSCAPE_POSITION_LEFT -> if (reversed) 1f else -1f
        SmartIslandSettings.LANDSCAPE_POSITION_RIGHT -> if (reversed) -1f else 1f
        else -> 0f
    }
    return pillExitDirection(side * cornerX, screenWidthDp)
}

/** Side the sibling picker opens on: away from the camera / the idle circle's edge. True = left. */
fun expectedPickerOnLeft(stage: LabStage): Boolean {
    val cameraOnRight = when (stage.idle.position) {
        SmartIslandSettings.LANDSCAPE_POSITION_LEFT -> stage.rotation.reversed
        SmartIslandSettings.LANDSCAPE_POSITION_RIGHT -> !stage.rotation.reversed
        else -> stage.rotation.reversed
    }
    return cameraOnRight
}
