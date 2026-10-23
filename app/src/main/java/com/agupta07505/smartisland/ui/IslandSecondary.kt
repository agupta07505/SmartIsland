/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.nextSameAppIndex
import com.agupta07505.smartisland.model.secondaryCardIndex
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

/**
 * Secondary / tertiary cutout state for [IslandOverlayView] (see [IslandLayout]).
 *
 * The animated values are held as State objects behind getters, like [IslandLayout]. Reading them in
 * rememberIslandSecondary, i.e. inside the root composable, would recompose the WHOLE overlay on every frame of an expand, a
 * collapse or a card arriving with two cards on the island (a live event plus a notification). So a read in a
 * graphicsLayer / layout / offset lambda only re-runs that lambda, and the `*Shown` flags (derived, they flip once per
 * animation) are what composition looks at.
 */
internal class IslandSecondary(
    val sameAppCompanion: Boolean,
    val secondaryNotification: IslandNotification?,
    val secondaryIndex: Int,
    val tertiaryNotification: IslandNotification?,
    val tertiaryIndex: Int,
    val cornerPillCenterRel: Dp,
    private val cornerPillAlphaState: State<Float>,
    private val secondaryAlphaState: State<Float>,
    private val secondaryScaleState: State<Float>,
    private val secondaryBubbleWidthState: State<Dp>,
    private val circleSize: Dp,
    private val miniPillWidth: Dp,
    private val secondaryIsPill: Boolean,
    private val secondaryBubbleCornerState: State<Dp>,
    private val tertiaryAlphaState: State<Float>,
    private val tertiaryScaleState: State<Float>,
    val expandedCompactX: Dp,
    private val secondaryOffsetState: State<Dp>,
    val secondaryMergeAnim: androidx.compose.animation.core.Animatable<Float, androidx.compose.animation.core.AnimationVector1D>,
    val idleDotShown: Boolean,
    val idleDotAlpha: Float,
    private val cornerPillShownState: State<Boolean>,
    private val secondaryShownState: State<Boolean>,
    private val tertiaryShownState: State<Boolean>,
    private val mergeBackdropShownState: State<Boolean>
) {
    val cornerPillAlpha: Float get() = cornerPillAlphaState.value
    val secondaryAlpha: Float get() = secondaryAlphaState.value
    val secondaryScale: Float get() = secondaryScaleState.value
    val secondaryBubbleWidth: Dp get() = secondaryBubbleWidthState.value
    val secondaryBubbleCorner: Dp get() = secondaryBubbleCornerState.value
    val tertiaryAlpha: Float get() = tertiaryAlphaState.value
    val tertiaryScale: Float get() = tertiaryScaleState.value
    val secondaryOffset: Dp get() = secondaryOffsetState.value

    val secondaryPillProgress: Float
        get() {
            val delta = (miniPillWidth - circleSize).value
            return if (delta > -0.0001f && delta < 0.0001f) {
                if (secondaryIsPill) 1f else 0f
            } else {
                ((secondaryBubbleWidthState.value - circleSize).value / delta).coerceIn(0f, 1f)
            }
        }

    val cornerPillShown: Boolean get() = cornerPillShownState.value
    val secondaryShown: Boolean get() = secondaryShownState.value
    val tertiaryShown: Boolean get() = tertiaryShownState.value
    val mergeBackdropShown: Boolean get() = mergeBackdropShownState.value
}

@Composable
internal fun rememberIslandSecondary(
    settings: SmartIslandSettings,
    notifications: List<IslandNotification>,
    safeIndex: Int,
    expanded: Boolean,
    layout: IslandLayout,
    activeNotification: IslandNotification?,
    isFullWidth: Boolean,
    isLandscape: Boolean,
    screenWidth: Dp,
    screenCenter: Dp,
    landscapeDot: com.agupta07505.smartisland.model.IdleDotGeometry?,
    /** Landscape: the widget pill is on the island with no card, so the idle dot (the toggle) is shown beside it. */
    widgetPill: Boolean = false,
    userInteractionTimestamp: Long,
    switchScaleAnim: androidx.compose.animation.core.Animatable<Float, androidx.compose.animation.core.AnimationVector1D>,
    forceMerge: Boolean = false,
    holdMerge: Boolean = false,
    /** Length (ms, user speed) of the merge a forced one plays: step 1 of the idle-circle close, so it ends with the expanded close. */
    forcedMergeMs: Int = 0
): IslandSecondary {
    val miniPillWidth = layout.miniPillWidth
    val circleSize = layout.circleSize
    val compactGap = layout.compactGap
    val compactShapes = layout.compactShapes
    val animSpeed = settings.animationSpeed(isLandscape)
    val hasCompanion = layout.hasCompanion
    val isCircleLeft = layout.isCircleLeft
    val collapsedMainLeft = layout.collapsedMainLeft
    val mainCenter = layout.mainCenter
    val circleCenter = layout.circleCenter
    val groupStart = layout.groupStart
    val groupEnd = layout.groupEnd
    val groupCenter = layout.groupCenter
    val isHiding = layout.isHiding
    val collapsedTopShift = layout.collapsedTopShift

    // The same-app rule applies to the expanded island: a sibling from the same app turns the secondary cutout into
    // the picker button; without one it shows the other card (pill / cutout) so neither vanishes while the island is
    // open.
    val sameAppRule = expanded
    val sameAppIndex = if (!settings.enableNotchMode && sameAppRule) {
        nextSameAppIndex(notifications, safeIndex)
    } else -1
    val sameAppCompanion = sameAppIndex >= 0
    val secondaryNotification = if (!settings.enableNotchMode) {
        notifications.getOrNull(secondaryCardIndex(notifications, safeIndex, sameAppRule))
    } else null
    val secondaryIndex = if (secondaryNotification != null) {
        notifications.indexOfFirst { it.key == secondaryNotification.key }
    } else -1
    // One app, one cutout: the tertiary card comes from an app that is neither the open / collapsed
    // card's nor the secondary cutout's.
    val tertiaryNotification = if (!settings.enableNotchMode && notifications.size >= 3) {
        notifications.getOrNull(
            com.agupta07505.smartisland.model.tertiaryCardIndex(notifications, safeIndex, secondaryIndex)
        )
    } else null
    val tertiaryIndex = if (tertiaryNotification != null) {
        notifications.indexOfFirst { it.key == tertiaryNotification.key }
    } else -1
    val isSplitMode = hasCompanion
    val secondaryIsPill = !sameAppCompanion &&
        compactShapes.singleOrNull() == CompactNotificationShape.MiniPill
    val showTertiaryPill = compactShapes.size == 2 && tertiaryNotification != null

    // The expanded window is always full width, so the pill sits at its collapsed spot measured from
    // the screen centre. The expanded card sits lower in landscape, so the pill never needs hiding.
    val cornerPillCenterRel = mainCenter - screenCenter
    // Like the portrait mini pill beside the open card: it shows the OTHER stored card and a tap swaps to it.
    val cornerPillVisible = isLandscape && !settings.enableNotchMode && expanded && !isHiding &&
        secondaryIsPill && secondaryNotification != null && secondaryIndex >= 0
    val cornerPillAlphaState = animateFloatAsState(
        targetValue = if (cornerPillVisible) 1f else 0f,
        animationSpec = tween(durationMillis = animSpeed.durationMs(300), easing = FastOutSlowInEasing),
        label = "landscapeCornerPillAlpha"
    )
    val secondaryAlphaState = animateFloatAsState(
        targetValue = if (isSplitMode && !isHiding && !cornerPillVisible) 1f else 0f,
        animationSpec = tween(durationMillis = animSpeed.durationMs(260), easing = FastOutSlowInEasing),
        label = "secondaryAlpha"
    )
    val secondaryScaleState = animateFloatAsState(
        targetValue = if (isSplitMode && !isHiding && !cornerPillVisible) 1f else 0.3f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = animSpeed.stiffness(320f)),
        label = "secondaryScale"
    )
    val secondaryBubbleWidthState = animateDpAsState(
        targetValue = if (secondaryIsPill) miniPillWidth else circleSize,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = animSpeed.stiffness(360f)),
        label = "secondaryBubbleWidth"
    )
    val secondaryBubbleCornerState = animateDpAsState(
        targetValue = if (secondaryIsPill) settings.cornerRadius.dp else circleSize / 2f,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = animSpeed.stiffness(360f)),
        label = "secondaryBubbleCorner"
    )
    val tertiaryAlphaState = animateFloatAsState(
        targetValue = if (showTertiaryPill && !isHiding) 1f else 0f,
        animationSpec = tween(durationMillis = animSpeed.durationMs(260), easing = FastOutSlowInEasing),
        label = "tertiaryAlpha"
    )
    val tertiaryScaleState = animateFloatAsState(
        targetValue = if (showTertiaryPill && !isHiding) 1f else 0.3f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = animSpeed.stiffness(320f)),
        label = "tertiaryScale"
    )

    val expandedCompactX = collapsedMainLeft
    val collapsedSecondaryOffset = if (isFullWidth) {
        circleCenter - screenCenter
    } else {
        circleCenter - groupCenter
    }
    val secondaryExpandedOffset = calculateSecondaryExpandedOffset(
        secondaryIsPill = secondaryIsPill,
        isCircleLeft = isCircleLeft,
        isFullWidth = isFullWidth,
        expandedCompactX = expandedCompactX.value,
        screenCenter = screenCenter.value,
        miniPillWidth = miniPillWidth.value,
        circleSize = circleSize.value,
        compactGap = compactGap.value
    ).dp
    // Same two speeds as the main island: a spring for expand / collapse, the slow glide for the
    // collapsed group re-arranging itself (so the circle keeps pace with the pill).
    var prevExpandedForOffset by remember { mutableStateOf(expanded) }
    val offsetFollowsExpand = expanded != prevExpandedForOffset
    SideEffect { prevExpandedForOffset = expanded }
    // The secondary pill changing sides because a system chip came or went jumps, it does not slide.
    var prevCircleLeftForOffset by remember { mutableStateOf(isCircleLeft) }
    val sideJumped = isCircleLeft != prevCircleLeftForOffset
    SideEffect { prevCircleLeftForOffset = isCircleLeft }
    val secondaryOffsetState = animateDpAsState(
        targetValue = if (!expanded) collapsedSecondaryOffset else secondaryExpandedOffset,
        animationSpec = if (sideJumped) {
            androidx.compose.animation.core.snap()
        } else if (offsetFollowsExpand) {
            spring<androidx.compose.ui.unit.Dp>(dampingRatio = islandSpringDamping(isLandscape), stiffness = animSpeed.stiffness(islandSpringStiffness(isLandscape)))
        } else {
            spring<androidx.compose.ui.unit.Dp>(dampingRatio = 1f, stiffness = animSpeed.stiffness(if (isLandscape) 150f else 220f))
        },
        label = "secondaryOffset"
    )

    // Idle merge (collapsed island only): when the secondary round cutout has not been interacted with for
    // SECONDARY_IDLE_MS, it merges into the primary cutout.
    val secondaryCanIdleMerge = !expanded && secondaryNotification != null && !isHiding
    var secondaryIdleMerged by remember { mutableStateOf(false) }
    // Idle circle pressed: the secondary cutout merges into the collapsed pill now, together with the expanded island's close
    // (step 1 of the idle-circle close), and only then the pill leaves (step 2).
    val idleCloseMerge = forceMerge && secondaryCanIdleMerge
    LaunchedEffect(secondaryCanIdleMerge, userInteractionTimestamp, settings.secondaryMergeMs, idleCloseMerge) {
        secondaryIdleMerged = false
        if (idleCloseMerge) {
            secondaryIdleMerged = true
            return@LaunchedEffect
        }
        if (!secondaryCanIdleMerge) return@LaunchedEffect
        kotlinx.coroutines.delay(settings.secondaryMergeMs.toLong())
        secondaryIdleMerged = true
    }
    val secondaryMergeAnim = remember { androidx.compose.animation.core.Animatable(0f) }
    val secondaryBubblePresent = secondaryNotification != null
    LaunchedEffect(secondaryIdleMerged, expanded, settings.mergeStyle, secondaryBubblePresent, holdMerge, idleCloseMerge) {
        if (holdMerge && !secondaryBubblePresent && !expanded) {
            // The pill is leaving and the last card went: finish the merge it was in (no snap back to apart), while
            // the cutout fades with its normal animation.
            val remainingMs = (settings.mergeStyleKind().baseDurationMs * (1f - secondaryMergeAnim.value)).toInt().coerceAtLeast(60)
            val pillSpringMs = com.agupta07505.smartisland.model.pillSpringSettleMs(
                animSpeed.stiffness(com.agupta07505.smartisland.model.PILL_SPRING_STIFFNESS)
            ).toInt().coerceAtLeast(60)
            secondaryMergeAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(minOf(animSpeed.durationMs(remainingMs), pillSpringMs), easing = MERGE_IN_EASING)
            )
        } else if (secondaryIdleMerged) {
            // The small bubble slides sideways to the big one, pulled in as it nears (the bubble slides to the pill's
            // side and fuses into it, see mergeTravelDp / mergeSwell).
            val mergeMs = if (idleCloseMerge && forcedMergeMs > 0) {
                animSpeed.systemDurationMs(forcedMergeMs)
            } else {
                animSpeed.durationMs(settings.mergeStyleKind().baseDurationMs)
            }
            secondaryMergeAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(mergeMs, easing = MERGE_IN_EASING)
            )
        } else if (expanded || !secondaryBubblePresent) {
            // No cutout left (expanded, or none to show): reset without animating,
            // so the next cutout starts apart instead of mid-return.
            secondaryMergeAnim.snapTo(0f)
        } else {
            secondaryMergeAnim.animateTo(0f, tween(animSpeed.durationMs(SECONDARY_RETURN_MS), easing = MERGE_OUT_EASING))
        }
    }

    // Idle dot: a permanent control (tap and long press only) at the idle spot while a card is on the island
    // (landscape only; portrait uses the pill's centre).
    val idleDotShown = !settings.enableNotchMode && com.agupta07505.smartisland.model.shouldShowIdleDot(
        dot = landscapeDot,
        hasCards = com.agupta07505.smartisland.model.pillOnIsland(notifications.isNotEmpty(), widgetPill),
        isLandscape = isLandscape
    )
    val idleDotAlpha = if (idleDotShown) 1f else 0f
    val cornerPillShownState = remember(cornerPillAlphaState) { derivedStateOf { cornerPillAlphaState.value > 0.01f } }
    val secondaryShownState = remember(secondaryAlphaState, secondaryMergeAnim) {
        derivedStateOf { secondaryAlphaState.value > 0f && secondaryMergeAnim.value < 0.999f }
    }
    val tertiaryShownState = remember(tertiaryAlphaState) { derivedStateOf { tertiaryAlphaState.value > 0f } }
    val mergeBackdropShownState = remember(secondaryAlphaState, secondaryMergeAnim) {
        derivedStateOf {
            secondaryAlphaState.value > 0f && secondaryMergeAnim.value > 0f && secondaryMergeAnim.value < 0.999f
        }
    }
    return IslandSecondary(
        sameAppCompanion = sameAppCompanion,
        secondaryNotification = secondaryNotification,
        secondaryIndex = secondaryIndex,
        tertiaryNotification = tertiaryNotification,
        tertiaryIndex = tertiaryIndex,
        cornerPillCenterRel = cornerPillCenterRel,
        cornerPillAlphaState = cornerPillAlphaState,
        secondaryAlphaState = secondaryAlphaState,
        secondaryScaleState = secondaryScaleState,
        secondaryBubbleWidthState = secondaryBubbleWidthState,
        circleSize = circleSize,
        miniPillWidth = miniPillWidth,
        secondaryIsPill = secondaryIsPill,
        secondaryBubbleCornerState = secondaryBubbleCornerState,
        tertiaryAlphaState = tertiaryAlphaState,
        tertiaryScaleState = tertiaryScaleState,
        expandedCompactX = expandedCompactX,
        secondaryOffsetState = secondaryOffsetState,
        secondaryMergeAnim = secondaryMergeAnim,
        idleDotShown = idleDotShown,
        idleDotAlpha = idleDotAlpha,
        cornerPillShownState = cornerPillShownState,
        secondaryShownState = secondaryShownState,
        tertiaryShownState = tertiaryShownState,
        mergeBackdropShownState = mergeBackdropShownState
    )
}

@Composable
internal fun SecondaryBubbleContent(
    notification: IslandNotification,
    settings: SmartIslandSettings
) {
    when (notification.mode) {
        IslandMode.Bluetooth -> {
            BluetoothCollapsedRight(
                notification = notification,
                settings = settings
            )
        }
        IslandMode.Flashlight -> {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF59E0B).copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.FlashlightOn,
                    contentDescription = "Flashlight",
                    tint = Color(0xFFFACC15),
                    modifier = Modifier.size(12.dp)
                )
            }
        }
        IslandMode.Hotspot -> {
            HotspotCollapsedGlyph(notification = notification, settings = settings)
        }
        IslandMode.Battery -> {
            BatteryCollapsedGlyph(notification = notification, settings = settings)
        }
        IslandMode.LiveActivity -> {
            LiveActivityCollapsedGlyph(notification = notification, settings = settings)
        }
        IslandMode.Navigation -> {
            NavigationCollapsedGlyph(notification = notification, settings = settings)
        }
        IslandMode.IncomingCall -> {
            val icon = notification.largeIcon ?: notification.icon
            if (icon != null) {
                Image(
                    bitmap = icon.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                )
            } else {
                Icon(
                    Icons.Rounded.Call,
                    contentDescription = null,
                    tint = Color(settings.callColor),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        IslandMode.Music -> {
            val artwork = com.agupta07505.smartisland.util.MediaArt.glyphBitmap(notification)
            if (artwork != null) {
                Image(
                    bitmap = artwork.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color(settings.musicVisualizerColor)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
        IslandMode.ScreenRecording -> {
            ScreenRecordingCollapsedGlyph(settings = settings, paused = notification?.recordingPaused == true)
        }
        IslandMode.AudioRecording -> {
            AudioRecordingCollapsedGlyph(settings = settings, paused = notification?.recordingPaused == true)
        }
        IslandMode.Timer -> {
            TimerCollapsedGlyph(notification = notification, settings = settings)
        }
        IslandMode.Stopwatch -> {
            StopwatchCollapsedGlyph(notification = notification, settings = settings)
        }
        IslandMode.Notification, IslandMode.DownloadUpload, IslandMode.Empty -> {
            NotificationGlyph(notification = notification, settings = settings)
        }
    }
}
