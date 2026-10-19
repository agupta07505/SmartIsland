/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import com.agupta07505.smartisland.buddy.model.MascotSide
import androidx.compose.runtime.collectAsState
import com.agupta07505.smartisland.buddy.model.MascotVisitKind
import com.agupta07505.smartisland.buddy.runtime.MascotVisitScheduler
import com.agupta07505.smartisland.buddy.MascotRuntime
import com.agupta07505.smartisland.buddy.runtime.MascotEvents
import com.agupta07505.smartisland.buddy.model.MascotTrack
import com.agupta07505.smartisland.buddy.ui.MascotLayer
import com.agupta07505.smartisland.util.CameraCutoutDetector
import com.agupta07505.smartisland.util.AlertWidthBudget
import com.agupta07505.smartisland.util.CutoutInsets
import com.agupta07505.smartisland.data.IslandRestoreState
import com.agupta07505.smartisland.R
import androidx.compose.ui.res.stringResource
import com.agupta07505.smartisland.data.SmartIslandCommand
import com.agupta07505.smartisland.model.SwipeAction
import com.agupta07505.smartisland.ui.expanded.IslandExpandedContent
import com.agupta07505.smartisland.ui.expanded.trySendFirstAction
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.positionChange
import kotlin.math.abs
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.di.SmartIslandRepositories
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.data.LaunchableApp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
fun IslandOverlayView(
    settings: SmartIslandSettings,
    expanded: Boolean,
    notifications: List<IslandNotification>,
    selectedIndex: Int,
    launcherApps: List<LaunchableApp>?,
    onPageSelected: (Int) -> Unit,
    onOpenNotification: (IslandNotification) -> Unit,
    onLaunchApp: (String) -> Unit,
    onToggleExpanded: () -> Unit,
    onDismissNotification: () -> Unit,
    onOpenFloatingWindow: () -> Unit,
    onOpenNotificationShade: () -> Unit = {},
    statusBarHeight: Float,
    modifier: Modifier = Modifier,
    isInputActive: Boolean = false,
    onReplyStateChanged: (Boolean) -> Unit = {},
    onDismissAllNotifications: () -> Unit = {},
    isFullWidth: Boolean = true,
    ambientMode: Boolean = false,
    burnInShiftXDp: Float = 0f,
    burnInShiftYDp: Float = 0f,
    /** The user touched the island; pending messages get their full seven seconds again. */
    onUserInteraction: () -> Unit = {}
) {
    // Fix #1: rememberUpdatedState ensures the lambda is always fresh
    // even though pointerInput(Unit) never restarts its coroutine
    val currentOnToggle by rememberUpdatedState(onToggleExpanded)
    val currentOnDismiss by rememberUpdatedState(onDismissNotification)
    val currentOnDismissAll by rememberUpdatedState(onDismissAllNotifications)
    val currentOnOpenFloatingWindow by rememberUpdatedState(onOpenFloatingWindow)
    val currentOnOpenNotificationShade by rememberUpdatedState(onOpenNotificationShade)
    val currentOnOpenNotification by rememberUpdatedState(onOpenNotification)
    val currentOnPageSelected by rememberUpdatedState(onPageSelected)
    val currentExpanded by rememberUpdatedState(expanded)
    val currentSettings by rememberUpdatedState(settings)
    val safeIndex = selectedIndex.coerceIn(0, (notifications.size - 1).coerceAtLeast(0))
    val currentNotifications by rememberUpdatedState(notifications)
    val currentSelectedIndex by rememberUpdatedState(safeIndex)
    val haptic = LocalHapticFeedback.current

    val scope = rememberCoroutineScope()
    var dragOffset by remember { mutableStateOf(0f) }
    var pillDragOffsetX by remember { mutableStateOf(0f) }

    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val displayMetrics = context.resources.displayMetrics
    val density = LocalDensity.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenCenter = screenWidth / 2f
    val maxExpandedWidth = calculateExpandedWidth(
        isLandscape = isLandscape,
        screenWidthDp = configuration.screenWidthDp.toFloat(),
        screenHeightDp = configuration.screenHeightDp.toFloat()
    ).dp
    val transition = updateTransition(targetState = expanded, label = "islandTransition")

    // Motion follows Apple's spring conventions. Opening is a lightly damped spring (response ≈ 0.42s,
    // damping 0.86: a barely-there settle); closing is critically damped (response ≈ 0.36s, damping 1.0:
    // no bounce, so it never overshoots back into the content). Stiffness = (2π / response)².
    // With system animations off, geometry snaps and fades shorten to 120ms.
    val reduceMotion = rememberReduceMotion()
    // Ghost-animation prevention (Rule_Book §7.3): right after a (re)start the alerts the listener restored land
    // on their static bounds instead of springing open from nothing. See RestoreSnap for the window.
    val restoreSyncedAt by IslandRestoreState.syncedAtMs.collectAsState()
    val firstFrameAt = remember { System.currentTimeMillis() }
    var restoreSnap by remember { mutableStateOf(true) }
    LaunchedEffect(restoreSyncedAt) {
        val wait = RestoreSnap.snapUntil(firstFrameAt, restoreSyncedAt) - System.currentTimeMillis()
        if (wait > 0) {
            restoreSnap = true
            kotlinx.coroutines.delay(wait)
        }
        restoreSnap = false
    }
    val appleEase = androidx.compose.animation.core.CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)
    // A physical preset (Bouncy, Snappy, Critical, Loose) replaces both with one spring of its own
    // stiffness and damping, the same going out and coming back; Standard keeps the tuning above.
    val springPreset = SpringPreset.fromId(settings.springPreset)
    fun dpSpec(opening: Boolean): androidx.compose.animation.core.FiniteAnimationSpec<androidx.compose.ui.unit.Dp> =
        when {
            reduceMotion || restoreSnap -> androidx.compose.animation.core.snap()
            springPreset.isPhysical -> spring(dampingRatio = springPreset.dampingRatio, stiffness = springPreset.stiffness)
            opening -> spring(dampingRatio = 0.86f, stiffness = 224f)
            else -> spring(dampingRatio = 1f, stiffness = 305f)
        }
    fun floatSpec(opening: Boolean): androidx.compose.animation.core.FiniteAnimationSpec<Float> =
        when {
            reduceMotion || restoreSnap -> androidx.compose.animation.core.snap()
            springPreset.isPhysical -> spring(dampingRatio = springPreset.dampingRatio, stiffness = springPreset.stiffness)
            opening -> spring(dampingRatio = 0.86f, stiffness = 224f)
            else -> spring(dampingRatio = 1f, stiffness = 305f)
        }
    // Content cross-fade is staggered: opening content waits for the shell to start growing, and
    // closing content leaves first so the shell never shrinks around visible content.
    fun expandedAlphaSpec(opening: Boolean): androidx.compose.animation.core.FiniteAnimationSpec<Float> =
        when {
            reduceMotion -> tween(120)
            opening -> tween(durationMillis = 240, delayMillis = 70, easing = appleEase)
            else -> tween(durationMillis = 110, easing = androidx.compose.animation.core.FastOutLinearInEasing)
        }
    fun collapsedAlphaSpec(opening: Boolean): androidx.compose.animation.core.FiniteAnimationSpec<Float> =
        when {
            reduceMotion -> tween(120)
            opening -> tween(durationMillis = 90, easing = androidx.compose.animation.core.FastOutLinearInEasing)
            else -> tween(durationMillis = 180, delayMillis = 110, easing = appleEase)
        }

    val activeNotification = notifications.getOrNull(safeIndex)
    val activeMode = activeNotification?.mode ?: IslandMode.Empty

    val initialEstimatedHeight = remember(activeMode, notifications.isEmpty()) {
        if (notifications.isEmpty()) 290.dp else defaultEstimatedHeightForMode(activeMode)
    }
    var expandedHeight by remember { mutableStateOf(initialEstimatedHeight) }
    // Size classes: Medium (active task) is ~340dp wide, Large (idle shortcuts dashboard) ~350dp, both
    // capped to the screen. Medium heights stay content-measured; Large is ~300dp when it holds apps.
    val sizeClass = expandedSizeClass(idleDashboard = notifications.isEmpty())
    val expandedWidth = expandedWidthFor(sizeClass, maxExpandedWidth)
    val expandedRadius = expandedCornerRadiusFor(sizeClass)

    LaunchedEffect(activeMode, notifications.isEmpty()) {
        if (!expanded) {
            expandedHeight = if (notifications.isEmpty()) 290.dp else defaultEstimatedHeightForMode(activeMode)
        }
    }

    // One shared width: the base width, widened only as far as a resting buddy needs (see PillLayout).
    val appContext = androidx.compose.ui.platform.LocalContext.current
    // The overlay window's own cutout insets (fed by the service) refine the detection as soon as they arrive.
    val cutoutSnapshot by CutoutInsets.snapshot.collectAsState()
    val cutoutInfo = remember(configuration, cutoutSnapshot) { CameraCutoutDetector.detect(appContext) }
    val holeWidthDp = if (cutoutInfo.hasHardwareCutout) cutoutInfo.rawWidthDp else null
    val buddyTucked by MascotRuntime.controller.tucked.collectAsState()
    // Same budget the service sizes the window with (Rule_Book §10.2).
    val maxAlertWidthDp = remember(configuration, settings.xOffset) {
        AlertWidthBudget.maxAlertPillWidthDp(appContext, settings)
    }
    val pillWidthDp = PillLayout.effectiveWidthDp(
        settings, holeWidthDp, notifications.size, buddyTucked,
        contentWingDp = PillLayout.contentWingDp(activeNotification),
        maxAlertWidthDp = maxAlertWidthDp
    ).dp
    val buddyOnPill = PillLayout.buddyOnPill(settings, notifications.size)
    // A cameo or countdown watch brings the buddy out over a busy pill, in one wing (a countdown is
    // watched from the camera's left, over the timer's mark, whichever side it rests on).
    val buddyVisit by MascotRuntime.controller.visit.collectAsState()
    val buddyAside = settings.buddyEnabled && buddyVisit?.kind?.takesOnlyItsWing == true
    val buddyAsideSideLeft = when (buddyVisit?.kind) {
        is MascotVisitKind.Countdown, MascotVisitKind.Retreat -> true
        else -> settings.buddySideValue == MascotSide.Left
    }
    // The buddy hears about events even while the island is busy, so a reaction can wait for the rest.
    LaunchedEffect(settings.buddyEnabled, settings.buddyReacts) {
        MascotRuntime.controller.reacts = settings.buddyEnabled && settings.buddyReacts
        if (!settings.buddyEnabled) return@LaunchedEffect
        MascotEvents.moments.collect { MascotRuntime.controller.post(it) }
    }
    // Now and then it strolls through the pill, and a tucked buddy is called back out. Skipped under Battery
    // Saver, with Reduce Motion, and whenever the pill is busy (the controller only starts a visit at rest).
    val visitFrequency = settings.buddyVisitFrequency
    val visitsReduceMotion = rememberReduceMotion()
    val visitsContext = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(settings.buddyEnabled, visitFrequency, visitsReduceMotion) {
        if (!settings.buddyEnabled || visitsReduceMotion) return@LaunchedEffect
        val power = visitsContext.getSystemService(android.content.Context.POWER_SERVICE) as? android.os.PowerManager
        while (true) {
            val wait = MascotVisitScheduler.nextDelaySec(visitFrequency) ?: return@LaunchedEffect
            kotlinx.coroutines.delay((wait * 1000f).toLong())
            if (power?.isPowerSaveMode == true) continue
            val controller = MascotRuntime.controller
            if (controller.tucked.value) controller.untuck(MascotVisitScheduler.greeting())
            else controller.startVisit(MascotVisitKind.Lap, MascotVisitScheduler.greeting())
        }
    }
    val buddyTrack = remember(pillWidthDp, settings.height, settings.xOffset, settings.buddyEnabled, settings.buddySide, cutoutInfo) {
        MascotTrack.forPill(
            pillWidthDp = pillWidthDp.value,
            pillHeightDp = settings.height,
            sizeDp = PillLayout.buddySizeDp(settings.height),
            holeWidthDp = holeWidthDp,
            // Where the hole sits in the pill: the pill's middle, plus how far the hole is from where the pill is.
            holeCenterDp = holeWidthDp?.let { pillWidthDp.value / 2f + (cutoutInfo.rawCenterOffsetDp - settings.xOffset) },
            side = settings.buddySideValue
        )
    }
    // Read by the pill's one gesture handler, which outlives recompositions.
    val currentBuddyTrack by rememberUpdatedState(buddyTrack)
    val currentBuddyActive by rememberUpdatedState(buddyOnPill && !buddyTucked && !expanded)
    val compactGap = COMPACT_INDICATOR_GAP_DP.dp
    val miniPillWidth = pillWidthDp
    val circleSize = settings.height.dp
    val compactShapes = compactNotificationShapes(notifications.size, expanded)
    val hasCompanion = if (settings.enableNotchMode) false else notifications.size >= 2
    val isCircleLeft = settings.circlePosition == SmartIslandSettings.CIRCLE_POSITION_LEFT
    val collapsedGroupWidth = pillWidthDp + if (hasCompanion) compactGap + circleSize else 0.dp

    val desiredMainLeft = screenCenter + settings.xOffset.dp - pillWidthDp / 2f
    val (minMainLeft, maxMainLeft) = when {
        !hasCompanion -> compactGap to (screenWidth - compactGap - pillWidthDp).coerceAtLeast(compactGap)
        isCircleLeft -> (compactGap + circleSize + compactGap) to (screenWidth - compactGap - pillWidthDp).coerceAtLeast(compactGap + circleSize + compactGap)
        else -> compactGap to (screenWidth - compactGap - collapsedGroupWidth).coerceAtLeast(compactGap)
    }
    val collapsedMainLeft = desiredMainLeft.coerceIn(minMainLeft, maxMainLeft)
    val mainCenter = collapsedMainLeft + pillWidthDp / 2f
    val circleLeft = if (isCircleLeft) {
        collapsedMainLeft - compactGap - circleSize
    } else {
        collapsedMainLeft + pillWidthDp + compactGap
    }
    val circleCenter = circleLeft + circleSize / 2f
    val groupStart = if (isCircleLeft && hasCompanion) circleLeft else collapsedMainLeft
    val groupEnd = if (!isCircleLeft && hasCompanion) circleLeft + circleSize else collapsedMainLeft + pillWidthDp
    val groupCenter = (groupStart + groupEnd) / 2f

    val collapsedMainOffset = if (settings.enableNotchMode) {
        settings.xOffset.dp
    } else if (isFullWidth) {
        mainCenter - screenCenter
    } else {
        mainCenter - groupCenter
    }
    val expandedTopOffset = calculateExpandedTopOffset(
        enableNotchMode = settings.enableNotchMode,
        hasCompanion = hasCompanion,
        statusBarHeightDp = statusBarHeight,
        notchHeightDp = settings.height,
        circleSizeDp = settings.height,
        compactGapDp = COMPACT_INDICATOR_GAP_DP
    ).dp
    val isIdleHiding = PillLayout.pillIdleHiding(settings, notifications.size)

    var isAutoHidden by remember { mutableStateOf(false) }
    var userInteractionTimestamp by remember { mutableStateOf(System.currentTimeMillis()) }
    val currentOnInteraction by rememberUpdatedState(onUserInteraction)
    LaunchedEffect(userInteractionTimestamp) { currentOnInteraction() }

    // Reset auto-hide whenever active notifications change or selection changes
    LaunchedEffect(notifications.map { it.key }, selectedIndex) {
        isAutoHidden = false
        userInteractionTimestamp = System.currentTimeMillis()
    }

    // Auto-hide countdown timer when pill is collapsed and autoHidePill is enabled
    LaunchedEffect(expanded, settings.autoHidePill, settings.autoHideTimeoutSeconds, userInteractionTimestamp) {
        if (expanded || !settings.autoHidePill) {
            isAutoHidden = false
            return@LaunchedEffect
        }
        val timeoutMs = (settings.autoHideTimeoutSeconds.coerceAtLeast(1) * 1000L)
        kotlinx.coroutines.delay(timeoutMs)
        isAutoHidden = true
    }

    val isHiding = !ambientMode && (isIdleHiding || (settings.autoHidePill && isAutoHidden))
    val buddyCanCameo = settings.buddyEnabled && notifications.isNotEmpty() && !expanded && !isHiding
    LaunchedEffect(buddyCanCameo) { MascotRuntime.controller.setCanCameo(buddyCanCameo) }
    val pillBackgroundColor = Color(settings.pillColor)

    val width by transition.animateDp(transitionSpec = { dpSpec(targetState) }, label = "islandWidth") {
        if (it) expandedWidth else if (isHiding) 0.dp else pillWidthDp
    }
    val height by transition.animateDp(transitionSpec = { dpSpec(targetState) }, label = "islandHeight") {
        if (it) expandedHeight else if (isHiding) 0.dp else settings.height.dp
    }
    val yOffset by transition.animateDp(transitionSpec = { dpSpec(targetState) }, label = "islandYOffset") {
        if (it) expandedTopOffset else 0.dp
    }
    val radius by transition.animateDp(transitionSpec = { dpSpec(targetState) }, label = "islandRadius") {
        if (it) expandedRadius else if (isHiding) 0.dp else settings.cornerRadius.dp
    }
    val animatedXOffset by transition.animateDp(transitionSpec = { dpSpec(targetState) }, label = "islandXOffset") {
        if (it) 0.dp else collapsedMainOffset
    }

    val collapsedAlpha by transition.animateFloat(
        transitionSpec = { collapsedAlphaSpec(targetState) },
        label = "collapsedAlpha"
    ) {
        if (it || isHiding) 0f else 1f
    }

    val expandedAlpha by transition.animateFloat(
        transitionSpec = { expandedAlphaSpec(targetState) },
        label = "expandedAlpha"
    ) {
        if (it) 1f else 0f
    }

    val contentScale by transition.animateFloat(
        transitionSpec = { floatSpec(targetState) },
        label = "contentScale"
    ) {
        if (it) 1f else 0.92f
    }

    val contentSlideY by transition.animateDp(
        transitionSpec = { dpSpec(targetState) },
        label = "contentSlideY"
    ) {
        if (it) 0.dp else (-6).dp
    }

    val safeWidth = width.coerceAtLeast(0.dp)
    val safeHeight = height.coerceAtLeast(0.dp)
    val safeRadius = radius.coerceAtLeast(0.dp)

    // Kinetic glow: the island's speed is read off the animated size on Compose's frame clock (dp per second,
    // the larger of the width and height speeds) and the glow's blur follows KineticGlow.blurRadius(speed).
    // While the island is still this coroutine is suspended and costs nothing; it samples once per frame only
    // while the size is changing, and the glow is cleared once the size has held still for a few frames.
    var glowRadiusDp by remember { mutableStateOf(0f) }
    LaunchedEffect(settings.kineticGlow, reduceMotion, springPreset) {
        if (!settings.kineticGlow || reduceMotion) {
            glowRadiusDp = 0f
            return@LaunchedEffect
        }
        // Debug builds only: `adb shell settings put global debug_island_glow_dp 20` holds the glow at a fixed
        // radius so its rendering can be inspected without catching a 0.3s animation on camera.
        if (com.agupta07505.smartisland.BuildConfig.DEBUG) {
            val held = android.provider.Settings.Global.getString(context.contentResolver, "debug_island_glow_dp")?.toFloatOrNull()
            if (held != null) {
                glowRadiusDp = held
                return@LaunchedEffect
            }
        }
        // `width` and `height` are the live animated values; reading them here tracks every frame.
        var lastWidth = width.value
        var lastHeight = height.value
        while (true) {
            androidx.compose.runtime.snapshotFlow { width.value != lastWidth || height.value != lastHeight }
                .first { it }
            var lastNanos = androidx.compose.runtime.withFrameNanos { it }
            lastWidth = width.value
            lastHeight = height.value
            var stillFrames = 0
            var peakSpeed = 0f
            while (stillFrames < KINETIC_GLOW_SETTLE_FRAMES) {
                val nanos = androidx.compose.runtime.withFrameNanos { it }
                val w = width.value
                val h = height.value
                val dt = (nanos - lastNanos) / 1_000_000_000f
                if (dt > 0f) {
                    val speed = maxOf(kotlin.math.abs(w - lastWidth), kotlin.math.abs(h - lastHeight)) / dt
                    glowRadiusDp = KineticGlow.blurRadius(speed)
                    stillFrames = if (w == lastWidth && h == lastHeight) stillFrames + 1 else 0
                    if (com.agupta07505.smartisland.BuildConfig.DEBUG && speed > peakSpeed) {
                        peakSpeed = speed
                        android.util.Log.d("IslandMotion", "preset=${springPreset.id} peakSpeed=%.0f dp/s glow=%.1f dp".format(peakSpeed, glowRadiusDp))
                    }
                }
                lastNanos = nanos
                lastWidth = w
                lastHeight = h
            }
            glowRadiusDp = 0f
        }
    }

    // Tactile spring scale bounce animation only when user switches between active notifications
    val switchScaleAnim = remember { androidx.compose.animation.core.Animatable(1f) }
    var isInitialComposition by remember { mutableStateOf(true) }
    var lastSelectedIndex by remember { mutableStateOf(selectedIndex) }
    LaunchedEffect(selectedIndex) {
        if (isInitialComposition) {
            isInitialComposition = false
            lastSelectedIndex = selectedIndex
            return@LaunchedEffect
        }
        if (lastSelectedIndex != selectedIndex) {
            lastSelectedIndex = selectedIndex
            switchScaleAnim.animateTo(
                targetValue = 0.92f,
                animationSpec = tween(40, easing = FastOutSlowInEasing)
            )
            switchScaleAnim.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = 650f
                )
            )
        }
    }

    // Dual Pill (Multi-Tasking Split Island) Detection:
    // When 2 or more notifications exist (e.g. Music + Notification/Timer/Call), split into Main Pill + Secondary Bubble
    val secondaryNotification = if (!settings.enableNotchMode && notifications.size >= 2) {
        notifications.firstOrNull { it.key != activeNotification?.key }
    } else null
    val secondaryIndex = if (secondaryNotification != null) {
        notifications.indexOfFirst { it.key == secondaryNotification.key }
    } else -1
    val tertiaryNotification = if (!settings.enableNotchMode && notifications.size >= 3) {
        notifications.firstOrNull {
            it.key != activeNotification?.key && it.key != secondaryNotification?.key
        }
    } else null
    val tertiaryIndex = if (tertiaryNotification != null) {
        notifications.indexOfFirst { it.key == tertiaryNotification.key }
    } else -1
    val isSplitMode = if (settings.enableNotchMode) false else secondaryNotification != null
    val secondaryIsPill = compactShapes.singleOrNull() == CompactNotificationShape.MiniPill
    val showTertiaryPill = compactShapes.size == 2 && tertiaryNotification != null

    val secondaryAlpha by animateFloatAsState(
        targetValue = if (isSplitMode && !isHiding) 1f else 0f,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "secondaryAlpha"
    )
    val secondaryScale by animateFloatAsState(
        targetValue = if (isSplitMode && !isHiding) 1f else 0.3f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 480f),
        label = "secondaryScale"
    )
    val secondaryBubbleWidth by animateDpAsState(
        targetValue = if (secondaryIsPill) miniPillWidth else circleSize,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 520f),
        label = "secondaryBubbleWidth"
    )
    val secondaryPillProgress = (miniPillWidth - circleSize).value.let { widthDelta ->
        if (widthDelta == 0f) {
            if (secondaryIsPill) 1f else 0f
        } else {
            ((secondaryBubbleWidth - circleSize).value / widthDelta).coerceIn(0f, 1f)
        }
    }
    val secondaryBubbleCorner by animateDpAsState(
        targetValue = if (secondaryIsPill) settings.cornerRadius.dp else circleSize / 2f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 520f),
        label = "secondaryBubbleCorner"
    )
    val tertiaryAlpha by animateFloatAsState(
        targetValue = if (showTertiaryPill && !isHiding) 1f else 0f,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "tertiaryAlpha"
    )
    val tertiaryScale by animateFloatAsState(
        targetValue = if (showTertiaryPill && !isHiding) 1f else 0.3f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 480f),
        label = "tertiaryScale"
    )

    val expandedCompactX = collapsedMainLeft
    // Privacy Dot Edge Alignment (Rule_Book §4.15): pin dot to cutout right edge
    val isPrivacyIndicatorBubble = secondaryNotification?.mode == IslandMode.PrivacyIndicator
    val cutoutRightEdgeOffset = remember(context, isPrivacyIndicatorBubble) {
        if (isPrivacyIndicatorBubble) {
            val bounds = com.agupta07505.smartisland.util.DisplayBounds.bounds(context)
            val boxes = com.agupta07505.smartisland.util.CameraCutoutDetector.currentBoxes(context)
            com.agupta07505.smartisland.util.CameraCutoutDetector.cutoutRightEdgeOffsetDp(
                boxes, bounds.width(), context.resources.displayMetrics.density
            )
        } else null
    }

    val standardCollapsedSecondaryOffset = if (isFullWidth) {
        circleCenter - screenCenter
    } else {
        circleCenter - groupCenter
    }
    val collapsedSecondaryOffset = if (isPrivacyIndicatorBubble && cutoutRightEdgeOffset != null && isFullWidth) {
        (cutoutRightEdgeOffset + 6f).dp
    } else {
        standardCollapsedSecondaryOffset
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
    val secondaryOffset by animateDpAsState(
        targetValue = if (!expanded) collapsedSecondaryOffset else secondaryExpandedOffset,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 520f),
        label = "secondaryOffset"
    )

    // Outer Box: Fills the entire WindowManager window bounds (which are padded for easy touch)
    val outerModifier = if (currentExpanded) {
        modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures {
                    if (isInputActive) {
                        onReplyStateChanged(false)
                    }
                    currentOnToggle()
                }
            }
    } else {
        modifier.fillMaxSize()
    }

    Box(
        modifier = outerModifier,
        contentAlignment = Alignment.TopCenter
    ) {

        // Invisible touch target over the pill location when hiding, so tapping the area reveals the pill or opens shortcuts
        if (isHiding && !currentExpanded) {
            Box(
                modifier = Modifier
                    .width(pillWidthDp)
                    .height(settings.height.dp)
                    .graphicsLayer {
                        translationX = collapsedMainOffset.toPx()
                    }
                    .pointerInput(Unit) {
                        detectTapGestures {
                            if (settings.autoHidePill && isAutoHidden) {
                                // First tap on auto-hidden pill: awaken and reveal the pill
                                isAutoHidden = false
                                userInteractionTimestamp = System.currentTimeMillis()
                            } else if (settings.enableAppShortcuts || currentNotifications.isNotEmpty()) {
                                // Empty notifications idle hiding: expand favorite shortcuts if enabled
                                currentOnToggle()
                            }
                        }
                    }
            )
        }

        // The Large dashboard and message cards are pitch black so they merge with the camera cutout, whatever the pill colour.
        val shellColor by androidx.compose.animation.animateColorAsState(
            targetValue = if (ambientMode) Color.Black
            else if (expanded && (sizeClass == IslandSizeClass.Large || activeMode == IslandMode.Notification)) Color.Black
            else pillBackgroundColor.copy(alpha = settings.opacity),
            animationSpec = androidx.compose.animation.core.tween(180),
            label = "islandShellColor"
        )
        val mainShape = if (settings.enableNotchMode && !currentExpanded) {
            RoundedCornerShape(
                topStart = 0.dp,
                topEnd = 0.dp,
                bottomStart = safeRadius,
                bottomEnd = safeRadius
            )
        } else {
            RoundedCornerShape(safeRadius)
        }

        // Inner Box: The actual visible pill container, managing the black background shape and size animations
        Box(
            modifier = Modifier
                .width(safeWidth)
                .height(safeHeight)
                .graphicsLayer {
                    translationX = animatedXOffset.toPx() + (if (!currentExpanded) pillDragOffsetX else 0f) + (if (ambientMode) burnInShiftXDp.dp.toPx() else 0f)
                    translationY = yOffset.toPx() + dragOffset + (if (ambientMode) burnInShiftYDp.dp.toPx() else 0f)
                    scaleX = switchScaleAnim.value
                    scaleY = switchScaleAnim.value
                }
                .drawBehind {
                    if (glowRadiusDp > 0f) {
                        drawKineticGlow(glowRadiusDp.dp.toPx(), safeRadius.toPx())
                    }
                }
                .then(
                    if (settings.enableShadow && settings.shadowElevation > 0f && !isHiding) {
                        val activeMainShadow = if (currentExpanded) {
                            (settings.shadowElevation * 1.5f).dp
                        } else {
                            settings.shadowElevation.dp
                        }
                        Modifier.shadow(
                            elevation = activeMainShadow,
                            shape = mainShape,
                            clip = false,
                            ambientColor = Color.Black,
                            spotColor = Color.Black
                        )
                    } else Modifier
                )
                .clip(mainShape)
                .background(shellColor)
                .pointerInput(displayMetrics.density, isInputActive) {
                    if (isInputActive) return@pointerInput
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        userInteractionTimestamp = System.currentTimeMillis()
                        val pressTimeMs = System.currentTimeMillis()
                        val wasExpandedAtStart = currentExpanded
                        var isHoldRegistered = false
                        var dragAccumulatorY = 0f
                        var dragAccumulatorX = 0f
                        var isDragging = false
                        var pillGestureTriggered = false
                        // Set once a child (button, slider, pager) consumes this pointer: from then on
                        // the pill must not treat the gesture as its own tap, hold or swipe.
                        var childHandled = false
                        var slopAccumulatorX = 0f
                        var slopAccumulatorY = 0f

                        // The buddy looks at where a finger lands and, held on its head, gets a pat.
                        val touchXDp = down.position.x / displayMetrics.density
                        val touchYDp = down.position.y / displayMetrics.density
                        if (!wasExpandedAtStart && currentBuddyActive) {
                            MascotRuntime.controller.attend(touchXDp, touchYDp)
                        }

                        val holdJob = scope.launch {
                            kotlinx.coroutines.delay(HOLD_GESTURE_THRESHOLD_MS)
                            isHoldRegistered = true
                            triggerHapticVibration(context)
                            if (!wasExpandedAtStart && currentBuddyActive &&
                                currentBuddyTrack.touches(touchXDp, touchYDp, BUDDY_PAT_SLACK_DP)
                            ) {
                                MascotRuntime.controller.pat()
                            }
                        }

                        val pointerId = down.id

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == pointerId } ?: break

                            if (change.isConsumed) childHandled = true
                            if (childHandled) {
                                holdJob.cancel()
                                if (!change.pressed) break
                                continue
                            }

                            if (change.changedToUp()) {
                                change.consume()
                                holdJob.cancel()
                                val totalElapsedMs = System.currentTimeMillis() - pressTimeMs
                                val currentNotification = currentNotifications.getOrNull(currentSelectedIndex)

                                if (wasExpandedAtStart) {
                                    val swipeUpThreshold = -SWIPE_THRESHOLD_DP * displayMetrics.density
                                    val swipeDownThreshold = SWIPE_THRESHOLD_DP * displayMetrics.density
                                    if (isDragging && currentSettings.enableSwipeActions && dragOffset < swipeUpThreshold) {
                                        val isHold = isHoldRegistered || totalElapsedMs >= HOLD_GESTURE_THRESHOLD_MS
                                        val actionStr = if (isHold) currentSettings.swipeHoldUpAction else currentSettings.swipeUpAction
                                        val action = SwipeAction.fromId(actionStr, if (isHold) SwipeAction.DismissAll else SwipeAction.DismissCurrent)
                                        executeSwipeAction(
                                            action = action,
                                            currentNotification = currentNotification,
                                            context = context,
                                            onDismiss = currentOnDismiss,
                                            onDismissAll = currentOnDismissAll,
                                            onToggle = currentOnToggle,
                                            onOpenNotification = currentOnOpenNotification,
                                            onOpenFloatingWindow = currentOnOpenFloatingWindow,
                                            onOpenNotificationShade = currentOnOpenNotificationShade,
                                            onPageSelected = currentOnPageSelected,
                                            notificationsSize = currentNotifications.size,
                                            currentIndex = currentSelectedIndex
                                        )
                                    } else if (isDragging && currentSettings.enableSwipeActions && dragOffset > swipeDownThreshold) {
                                        val action = SwipeAction.fromId(currentSettings.swipeDownAction, SwipeAction.FloatingWindow)
                                        executeSwipeAction(
                                            action = action,
                                            currentNotification = currentNotification,
                                            context = context,
                                            onDismiss = currentOnDismiss,
                                            onDismissAll = currentOnDismissAll,
                                            onToggle = currentOnToggle,
                                            onOpenNotification = currentOnOpenNotification,
                                            onOpenFloatingWindow = currentOnOpenFloatingWindow,
                                            onOpenNotificationShade = currentOnOpenNotificationShade,
                                            onPageSelected = currentOnPageSelected,
                                            notificationsSize = currentNotifications.size,
                                            currentIndex = currentSelectedIndex
                                        )
                                    } else if (!isDragging || abs(dragOffset) < 10f * displayMetrics.density) {
                                        if (!isHoldRegistered) {
                                            if (currentNotification != null && currentNotification.mode != IslandMode.Music) {
                                                currentOnOpenNotification(currentNotification)
                                            } else {
                                                SmartIslandRepositories.notificationRepository(context).resetTimer()
                                            }
                                        }
                                    }
                                } else {
                                    // Collapsed state (In-Pill Gestures)
                                    if (!pillGestureTriggered) {
                                        val isPillSwipeEnabled = currentSettings.enablePillSwipeActions
                                        val pillThreshold = PILL_SWIPE_THRESHOLD_DP * displayMetrics.density
                                        val absX = abs(dragAccumulatorX)
                                        val absY = abs(dragAccumulatorY)

                                        if (isPillSwipeEnabled && (absX >= pillThreshold || absY >= pillThreshold)) {
                                            pillGestureTriggered = true
                                            if (absX > absY) {
                                                if (dragAccumulatorX < -pillThreshold) {
                                                    val action = SwipeAction.fromId(currentSettings.pillSwipeLeftAction, SwipeAction.PreviousNotification)
                                                    executeSwipeAction(
                                                        action = action,
                                                        currentNotification = currentNotification,
                                                        context = context,
                                                        onDismiss = currentOnDismiss,
                                                        onDismissAll = currentOnDismissAll,
                                                        onToggle = currentOnToggle,
                                                        onOpenNotification = currentOnOpenNotification,
                                                        onOpenFloatingWindow = currentOnOpenFloatingWindow,
                                                        onOpenNotificationShade = currentOnOpenNotificationShade,
                                                        onPageSelected = currentOnPageSelected,
                                                        notificationsSize = currentNotifications.size,
                                                        currentIndex = currentSelectedIndex
                                                    )
                                                } else if (dragAccumulatorX > pillThreshold) {
                                                    val action = SwipeAction.fromId(currentSettings.pillSwipeRightAction, SwipeAction.NextNotification)
                                                    executeSwipeAction(
                                                        action = action,
                                                        currentNotification = currentNotification,
                                                        context = context,
                                                        onDismiss = currentOnDismiss,
                                                        onDismissAll = currentOnDismissAll,
                                                        onToggle = currentOnToggle,
                                                        onOpenNotification = currentOnOpenNotification,
                                                        onOpenFloatingWindow = currentOnOpenFloatingWindow,
                                                        onOpenNotificationShade = currentOnOpenNotificationShade,
                                                        onPageSelected = currentOnPageSelected,
                                                        notificationsSize = currentNotifications.size,
                                                        currentIndex = currentSelectedIndex
                                                    )
                                                }
                                            } else {
                                                if (dragAccumulatorY < -pillThreshold) {
                                                    val action = SwipeAction.fromId(currentSettings.pillSwipeUpAction, SwipeAction.DismissCurrent)
                                                    executeSwipeAction(
                                                        action = action,
                                                        currentNotification = currentNotification,
                                                        context = context,
                                                        onDismiss = currentOnDismiss,
                                                        onDismissAll = currentOnDismissAll,
                                                        onToggle = currentOnToggle,
                                                        onOpenNotification = currentOnOpenNotification,
                                                        onOpenFloatingWindow = currentOnOpenFloatingWindow,
                                                        onOpenNotificationShade = currentOnOpenNotificationShade,
                                                        onPageSelected = currentOnPageSelected,
                                                        notificationsSize = currentNotifications.size,
                                                        currentIndex = currentSelectedIndex
                                                    )
                                                } else if (dragAccumulatorY > pillThreshold) {
                                                    val action = SwipeAction.fromId(currentSettings.pillSwipeDownAction, SwipeAction.Expand)
                                                    executeSwipeAction(
                                                        action = action,
                                                        currentNotification = currentNotification,
                                                        context = context,
                                                        onDismiss = currentOnDismiss,
                                                        onDismissAll = currentOnDismissAll,
                                                        onToggle = currentOnToggle,
                                                        onOpenNotification = currentOnOpenNotification,
                                                        onOpenFloatingWindow = currentOnOpenFloatingWindow,
                                                        onOpenNotificationShade = currentOnOpenNotificationShade,
                                                        onPageSelected = currentOnPageSelected,
                                                        notificationsSize = currentNotifications.size,
                                                        currentIndex = currentSelectedIndex
                                                    )
                                                }
                                            }
                                        } else {
                                            // Tap on collapsed pill: expands or reveals
                                            if (!isHoldRegistered) {
                                                if (settings.autoHidePill && isAutoHidden) {
                                                    isAutoHidden = false
                                                    userInteractionTimestamp = System.currentTimeMillis()
                                                } else if (currentNotifications.isNotEmpty() || currentSettings.enableAppShortcuts) {
                                                    currentOnToggle()
                                                }
                                            }
                                        }
                                    }
                                }
                                break
                            } else {
                                var dragAmountY = change.positionChange().y
                                var dragAmountX = change.positionChange().x
                                // A finger always drifts a little during a tap. Claiming (consuming) that drift
                                // cancels the tap of any button underneath, so hold back until the movement
                                // clearly exceeds touch slop, then replay what was held.
                                if (!isDragging) {
                                    slopAccumulatorY += dragAmountY
                                    slopAccumulatorX += dragAmountX
                                    if (abs(slopAccumulatorY) >= viewConfiguration.touchSlop ||
                                        abs(slopAccumulatorX) >= viewConfiguration.touchSlop
                                    ) {
                                        dragAmountY = slopAccumulatorY
                                        dragAmountX = slopAccumulatorX
                                    } else {
                                        dragAmountY = 0f
                                        dragAmountX = 0f
                                    }
                                }
                                if (abs(dragAmountY) > 0.5f || abs(dragAmountX) > 0.5f) {
                                    isDragging = true
                                    dragAccumulatorY += dragAmountY
                                    dragAccumulatorX += dragAmountX
                                    change.consume()
                                    if (abs(dragAccumulatorY) > 5f * displayMetrics.density || abs(dragAccumulatorX) > 5f * displayMetrics.density) {
                                        holdJob.cancel()
                                    }
                                    if (wasExpandedAtStart) {
                                        dragOffset = dragAccumulatorY.coerceIn(
                                            -DRAG_MAX_OFFSET_DP * displayMetrics.density,
                                            DRAG_MAX_OFFSET_DP * displayMetrics.density
                                        )
                                    } else {
                                        pillDragOffsetX = (dragAccumulatorX * 0.35f).coerceIn(
                                            -24f * displayMetrics.density,
                                            24f * displayMetrics.density
                                        )
                                        dragOffset = (dragAccumulatorY * 0.35f).coerceIn(
                                            -12f * displayMetrics.density,
                                            12f * displayMetrics.density
                                        )

                                        // Snappy immediate execution when swipe threshold is crossed while dragging
                                        if (!pillGestureTriggered && currentSettings.enablePillSwipeActions) {
                                            val pillThreshold = PILL_SWIPE_THRESHOLD_DP * displayMetrics.density
                                            val absX = abs(dragAccumulatorX)
                                            val absY = abs(dragAccumulatorY)

                                            if (absX >= pillThreshold || absY >= pillThreshold) {
                                                pillGestureTriggered = true
                                                holdJob.cancel()
                                                val currentNotification = currentNotifications.getOrNull(currentSelectedIndex)

                                                if (absX > absY) {
                                                    if (dragAccumulatorX < -pillThreshold) {
                                                        val action = SwipeAction.fromId(currentSettings.pillSwipeLeftAction, SwipeAction.PreviousNotification)
                                                        executeSwipeAction(
                                                            action = action,
                                                            currentNotification = currentNotification,
                                                            context = context,
                                                            onDismiss = currentOnDismiss,
                                                            onDismissAll = currentOnDismissAll,
                                                            onToggle = currentOnToggle,
                                                            onOpenNotification = currentOnOpenNotification,
                                                            onOpenFloatingWindow = currentOnOpenFloatingWindow,
                                                            onOpenNotificationShade = currentOnOpenNotificationShade,
                                                            onPageSelected = currentOnPageSelected,
                                                            notificationsSize = currentNotifications.size,
                                                            currentIndex = currentSelectedIndex
                                                        )
                                                    } else if (dragAccumulatorX > pillThreshold) {
                                                        val action = SwipeAction.fromId(currentSettings.pillSwipeRightAction, SwipeAction.NextNotification)
                                                        executeSwipeAction(
                                                            action = action,
                                                            currentNotification = currentNotification,
                                                            context = context,
                                                            onDismiss = currentOnDismiss,
                                                            onDismissAll = currentOnDismissAll,
                                                            onToggle = currentOnToggle,
                                                            onOpenNotification = currentOnOpenNotification,
                                                            onOpenFloatingWindow = currentOnOpenFloatingWindow,
                                                            onOpenNotificationShade = currentOnOpenNotificationShade,
                                                            onPageSelected = currentOnPageSelected,
                                                            notificationsSize = currentNotifications.size,
                                                            currentIndex = currentSelectedIndex
                                                        )
                                                    }
                                                } else {
                                                    if (dragAccumulatorY < -pillThreshold) {
                                                        val action = SwipeAction.fromId(currentSettings.pillSwipeUpAction, SwipeAction.DismissCurrent)
                                                        executeSwipeAction(
                                                            action = action,
                                                            currentNotification = currentNotification,
                                                            context = context,
                                                            onDismiss = currentOnDismiss,
                                                            onDismissAll = currentOnDismissAll,
                                                            onToggle = currentOnToggle,
                                                            onOpenNotification = currentOnOpenNotification,
                                                            onOpenFloatingWindow = currentOnOpenFloatingWindow,
                                                            onOpenNotificationShade = currentOnOpenNotificationShade,
                                                            onPageSelected = currentOnPageSelected,
                                                            notificationsSize = currentNotifications.size,
                                                            currentIndex = currentSelectedIndex
                                                        )
                                                    } else if (dragAccumulatorY > pillThreshold) {
                                                        val action = SwipeAction.fromId(currentSettings.pillSwipeDownAction, SwipeAction.Expand)
                                                        executeSwipeAction(
                                                            action = action,
                                                            currentNotification = currentNotification,
                                                            context = context,
                                                            onDismiss = currentOnDismiss,
                                                            onDismissAll = currentOnDismissAll,
                                                            onToggle = currentOnToggle,
                                                            onOpenNotification = currentOnOpenNotification,
                                                            onOpenFloatingWindow = currentOnOpenFloatingWindow,
                                                            onOpenNotificationShade = currentOnOpenNotificationShade,
                                                            onPageSelected = currentOnPageSelected,
                                                            notificationsSize = currentNotifications.size,
                                                            currentIndex = currentSelectedIndex
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        holdJob.cancel()
                        if (pillDragOffsetX != 0f) {
                            val startPillOffset = pillDragOffsetX
                            scope.launch {
                                androidx.compose.animation.core.Animatable(startPillOffset).animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                ) {
                                    pillDragOffsetX = value
                                }
                            }
                        }
                        if (dragOffset != 0f) {
                            val startDrag = dragOffset
                            scope.launch {
                                androidx.compose.animation.core.Animatable(startDrag).animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                ) {
                                    dragOffset = value
                                }
                            }
                        }
                    }
                },
            contentAlignment = Alignment.TopCenter
        ) {
            // Collapsed content layer (pinned to fixed pill bounds at top-center, cancelling yOffset)
            if (ambientMode) {
                Box(
                    modifier = Modifier
                        .width(pillWidthDp)
                        .height(settings.height.dp)
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val timeStr = remember {
                            val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                            sdf.format(java.util.Date())
                        }
                        Text(
                            text = timeStr,
                            color = Color(0x99FFFFFF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        if (notifications.isNotEmpty()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Notifications,
                                    contentDescription = null,
                                    tint = Color(0x99FFFFFF),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "${notifications.size}",
                                    color = Color(0x99FFFFFF),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            } else if (collapsedAlpha > 0f) {
                Box(
                    modifier = Modifier
                        .width(pillWidthDp)
                        .height(settings.height.dp)
                        .align(Alignment.TopCenter)
                        .graphicsLayer {
                            alpha = collapsedAlpha
                        }
                ) {
                    IslandCollapsedContent(
                        mode = activeMode,
                        notification = activeNotification,
                        collapsedAlpha = collapsedAlpha,
                        settings = settings,
                        stepAsideLeft = buddyAside && buddyAsideSideLeft,
                        stepAsideRight = buddyAside && !buddyAsideSideLeft
                    )
                    if (buddyOnPill || buddyAside) {
                        MascotLayer(
                            look = settings.buddyLook,
                            track = buddyTrack,
                            active = buddyOnPill && !expanded && !isHiding && collapsedAlpha > 0.99f,
                            controller = MascotRuntime.controller,
                            hidesWhenIdle = settings.buddyHidesWhenIdle
                        )
                    }
                }
            }

            // Expanded content layer — smoothly fade out while collapsing
            if (expanded || expandedAlpha > 0.01f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .graphicsLayer {
                            alpha = expandedAlpha
                            scaleX = contentScale
                            scaleY = contentScale
                            translationY = contentSlideY.toPx()
                        }
                ) {
                    IslandExpandedContent(
                        notifications = notifications,
                        launcherApps = launcherApps,
                        selectedIndex = selectedIndex,
                        onPageSelected = onPageSelected,
                        onOpenNotification = onOpenNotification,
                        onLaunchApp = onLaunchApp,
                        onCollapse = onToggleExpanded,
                        statusBarHeight = statusBarHeight.dp,
                        // Each mode owns its natural height. The launcher already
                        // supplies its own loading height and must not impose that
                        // minimum on compact call or battery content.
                        onHeightMeasured = { expandedHeight = it },
                        settings = settings,
                        onReplyStateChanged = onReplyStateChanged
                    )
                }
            }
        }

        // Collapsed: secondary circle. Expanded with 2: the same item morphs
        // into a full-size pill. Expanded with 3+: it stays the circle on the right.
        if (!settings.enableNotchMode && secondaryAlpha > 0f && secondaryNotification != null) {
            Box(
                modifier = Modifier
                    .absoluteOffset {
                        IntOffset(
                            secondaryOffset.roundToPx(),
                            0
                        )
                    }
                    .width(secondaryBubbleWidth)
                    .height(circleSize)
                    .graphicsLayer {
                        alpha = secondaryAlpha
                        scaleX = secondaryScale * switchScaleAnim.value
                        scaleY = secondaryScale * switchScaleAnim.value
                    }
                    .then(
                        if (settings.enableShadow && settings.shadowElevation > 0f) {
                            Modifier.shadow(
                                elevation = (settings.shadowElevation * 0.85f).dp,
                                shape = RoundedCornerShape(secondaryBubbleCorner),
                                clip = false,
                                ambientColor = Color.Black,
                                spotColor = Color.Black
                            )
                        } else Modifier
                    )
                    .clip(RoundedCornerShape(secondaryBubbleCorner))
                    .background(pillBackgroundColor.copy(alpha = settings.opacity))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (secondaryIndex >= 0) {
                            onPageSelected(secondaryIndex)
                        }
                        if (!currentExpanded) {
                            currentOnToggle()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = 1f - secondaryPillProgress },
                    contentAlignment = Alignment.Center
                ) {
                    SecondaryBubbleContent(
                        notification = secondaryNotification,
                        settings = settings
                    )
                }
                Box(
                    modifier = Modifier
                        .requiredWidth(miniPillWidth)
                        .height(circleSize)
                        .graphicsLayer { alpha = secondaryPillProgress },
                    contentAlignment = Alignment.Center
                ) {
                    IslandCollapsedContent(
                        mode = secondaryNotification.mode,
                        notification = secondaryNotification,
                        collapsedAlpha = 1f,
                        settings = settings
                    )
                }
            }
        }

        if (!settings.enableNotchMode && tertiaryAlpha > 0f && tertiaryNotification != null) {
            Box(
                modifier = Modifier
                    .absoluteOffset {
                        IntOffset(
                            (expandedCompactX - screenCenter + miniPillWidth / 2f).roundToPx(),
                            0
                        )
                    }
                    .width(miniPillWidth)
                    .height(circleSize)
                    .graphicsLayer {
                        alpha = tertiaryAlpha
                        scaleX = tertiaryScale * switchScaleAnim.value
                        scaleY = tertiaryScale * switchScaleAnim.value
                    }
                    .then(
                        if (settings.enableShadow && settings.shadowElevation > 0f) {
                            Modifier.shadow(
                                elevation = (settings.shadowElevation * 0.85f).dp,
                                shape = RoundedCornerShape(settings.cornerRadius.dp),
                                clip = false,
                                ambientColor = Color.Black,
                                spotColor = Color.Black
                            )
                        } else Modifier
                    )
                    .clip(RoundedCornerShape(settings.cornerRadius.dp))
                    .background(pillBackgroundColor.copy(alpha = settings.opacity))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (tertiaryIndex >= 0) {
                            onPageSelected(tertiaryIndex)
                        }
                        if (!currentExpanded) {
                            currentOnToggle()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                IslandCollapsedContent(
                    mode = tertiaryNotification.mode,
                    notification = tertiaryNotification,
                    collapsedAlpha = 1f,
                    settings = settings
                )
            }
        }
    }
}

@Composable
private fun SecondaryBubbleContent(
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
                    .background(IslandPalette.warning.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.FlashlightOn,
                    contentDescription = stringResource(R.string.a11y_flashlight),
                    tint = IslandPalette.warning,
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
            com.agupta07505.smartisland.ui.expanded.CallAvatar(notification = notification, size = 20.dp, settings = settings)
        }
        IslandMode.Music -> {
            val artwork = notification.largeIcon ?: notification.icon
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
            ScreenRecordingCollapsedGlyph(settings = settings)
        }
        IslandMode.Timer -> {
            TimerCollapsedGlyph(notification = notification, settings = settings)
        }
        IslandMode.Stopwatch -> {
            StopwatchCollapsedGlyph(notification = notification, settings = settings)
        }
        // Rule_Book §4.10, §4.3, §4.15/4.16: pulsing bell, breathing red dot, green / orange / split privacy dot.
        IslandMode.Alarm -> com.agupta07505.smartisland.ui.expanded.AlarmCollapsedGlyph()
        IslandMode.VoiceRecording -> com.agupta07505.smartisland.ui.expanded.VoiceRecordingDot(notification)
        IslandMode.PrivacyIndicator -> com.agupta07505.smartisland.ui.expanded.PrivacyIndicatorDot(notification)
        IslandMode.Notification, IslandMode.DownloadUpload, IslandMode.Empty -> {
            NotificationGlyph(notification = notification, settings = settings)
        }
    }
}

// Animation specs
internal const val EXPANDED_WIDTH_RATIO = 0.95f

internal fun calculateExpandedWidth(
    isLandscape: Boolean,
    screenWidthDp: Float,
    screenHeightDp: Float,
    ratio: Float = EXPANDED_WIDTH_RATIO
): Float {
    return if (isLandscape) {
        val portraitWidth = minOf(screenWidthDp, screenHeightDp)
        (portraitWidth * ratio).coerceIn(340f, 440f)
    } else {
        screenWidthDp * ratio
    }
}

internal enum class IslandSizeClass { Medium, Large }

internal const val MEDIUM_ISLAND_WIDTH_DP = 340f
internal const val LARGE_ISLAND_WIDTH_DP = 350f
internal const val MEDIUM_ISLAND_RADIUS_DP = 32f
internal const val LARGE_ISLAND_RADIUS_DP = 40f
internal const val LARGE_ISLAND_HEIGHT_DP = 300f

/**
 * In notch mode the expanded card starts below the lower of the status bar and the collapsed pill (whose
 * height setting covers the hardware notch), plus this gap so it never touches the cutout or the status icons.
 */
internal const val NOTCH_MODE_EXPANDED_CLEARANCE_DP = 8f

/** The idle shortcuts dashboard is Large; every active-task card is Medium. */
internal fun expandedSizeClass(idleDashboard: Boolean): IslandSizeClass =
    if (idleDashboard) IslandSizeClass.Large else IslandSizeClass.Medium

internal fun expandedWidthFor(sizeClass: IslandSizeClass, maxWidth: Dp): Dp {
    val target = if (sizeClass == IslandSizeClass.Large) LARGE_ISLAND_WIDTH_DP else MEDIUM_ISLAND_WIDTH_DP
    return target.dp.coerceAtMost(maxWidth)
}

/** Corner radius scales with the island: 32dp for Medium, 40dp for Large. */
internal fun expandedCornerRadiusFor(sizeClass: IslandSizeClass): Dp =
    (if (sizeClass == IslandSizeClass.Large) LARGE_ISLAND_RADIUS_DP else MEDIUM_ISLAND_RADIUS_DP).dp

internal fun calculateExpandedTopOffset(
    enableNotchMode: Boolean,
    hasCompanion: Boolean,
    statusBarHeightDp: Float,
    notchHeightDp: Float = 35f,
    circleSizeDp: Float = 34f,
    compactGapDp: Float = COMPACT_INDICATOR_GAP_DP
): Float {
    return if (enableNotchMode) {
        maxOf(statusBarHeightDp, notchHeightDp) + NOTCH_MODE_EXPANDED_CLEARANCE_DP
    } else if (hasCompanion) {
        maxOf(statusBarHeightDp, circleSizeDp + compactGapDp)
    } else {
        statusBarHeightDp
    }
}

internal fun calculateSecondaryExpandedOffset(
    secondaryIsPill: Boolean,
    isCircleLeft: Boolean,
    isFullWidth: Boolean,
    expandedCompactX: Float,
    screenCenter: Float,
    miniPillWidth: Float,
    circleSize: Float,
    compactGap: Float
): Float {
    return if (secondaryIsPill) {
        val secCenter = expandedCompactX + miniPillWidth / 2f
        if (isFullWidth) secCenter - screenCenter else 0f
    } else if (isCircleLeft) {
        val secCenter = expandedCompactX - compactGap - circleSize / 2f
        if (isFullWidth) secCenter - screenCenter else -(miniPillWidth + compactGap) / 2f
    } else {
        val secCenter = expandedCompactX + miniPillWidth + compactGap + circleSize / 2f
        if (isFullWidth) secCenter - screenCenter else ((miniPillWidth + compactGap) / 2f)
    }
}
private const val SWIPE_THRESHOLD_DP = 35f
private const val KINETIC_GLOW_SETTLE_FRAMES = 6
private const val PILL_SWIPE_THRESHOLD_DP = 16f
private const val DRAG_MAX_OFFSET_DP = 100f
private const val COMPACT_INDICATOR_GAP_DP = 8f
private const val HOLD_GESTURE_THRESHOLD_MS = 300L
/** A held touch this close to the buddy (dp beyond its edge) pats it. */
private const val BUDDY_PAT_SLACK_DP = 8f

private fun triggerHapticVibration(context: android.content.Context) {
    runCatching {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val vm = context.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager
            val vibrator = vm?.defaultVibrator
            if (vibrator?.hasVibrator() == true) {
                vibrator.vibrate(android.os.VibrationEffect.createOneShot(60L, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                return
            }
        }
        @Suppress("DEPRECATION")
        val vibrator = context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? android.os.Vibrator
        if (vibrator?.hasVibrator() == true) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(android.os.VibrationEffect.createOneShot(60L, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(60L)
            }
        }
    }
}

internal enum class CompactNotificationShape { MiniPill, Circle }

internal fun defaultEstimatedHeightForMode(mode: IslandMode?): Dp {
    return when (mode) {
        IslandMode.Music -> 168.dp
        IslandMode.Notification -> 165.dp
        IslandMode.IncomingCall, IslandMode.Battery -> 115.dp
        IslandMode.LiveActivity, IslandMode.Navigation -> 180.dp
        IslandMode.DownloadUpload, IslandMode.Hotspot -> 160.dp
        IslandMode.Bluetooth, IslandMode.Flashlight, IslandMode.ScreenRecording,
        IslandMode.Timer, IslandMode.Stopwatch -> 115.dp
        IslandMode.Alarm, IslandMode.VoiceRecording, IslandMode.PrivacyIndicator -> 130.dp
        IslandMode.Empty, null -> 290.dp
    }
}

internal fun compactNotificationShapes(
    notificationCount: Int,
    expanded: Boolean
): List<CompactNotificationShape> = when {
    notificationCount < 2 -> emptyList()
    !expanded -> listOf(CompactNotificationShape.Circle)
    notificationCount == 2 -> listOf(CompactNotificationShape.MiniPill)
    else -> listOf(CompactNotificationShape.MiniPill, CompactNotificationShape.Circle)
}

private fun trySkipMedia(
    context: android.content.Context?,
    notification: IslandNotification?,
    forward: Boolean
): Boolean {
    if (context == null) return false

    // 1. Direct notification action PendingIntent (e.g. Spotify, YouTube Music, podcasts)
    val actionSent = if (forward) {
        notification.trySendFirstAction(context, "next", "skip", "forward")
    } else {
        notification.trySendFirstAction(context, "previous", "prev", "rewind")
    }
    if (actionSent) return true

    // 2. Notification MediaSession token
    val token = notification?.mediaToken
    if (token != null) {
        val success = runCatching {
            val controller = android.media.session.MediaController(context, token)
            if (forward) controller.transportControls.skipToNext() else controller.transportControls.skipToPrevious()
            true
        }.getOrDefault(false)
        if (success) return true
    }

    // 3. Delegate to NotificationListenerService via SmartIslandCommand
    runCatching {
        val repo = SmartIslandRepositories.notificationRepository(context)
        if (forward) {
            repo.sendCommand(SmartIslandCommand.SkipNext(notification?.packageName))
        } else {
            repo.sendCommand(SmartIslandCommand.SkipPrevious(notification?.packageName))
        }
    }

    // 4. Fallback: AudioManager media key events
    runCatching {
        val audioManager = context.getSystemService(android.content.Context.AUDIO_SERVICE) as? android.media.AudioManager
        val keyCode = if (forward) android.view.KeyEvent.KEYCODE_MEDIA_NEXT else android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS
        val down = android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, keyCode)
        val up = android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, keyCode)
        audioManager?.dispatchMediaKeyEvent(down)
        audioManager?.dispatchMediaKeyEvent(up)
    }

    return true
}

private fun tryPlayPauseMedia(
    context: android.content.Context?,
    notification: IslandNotification?
) {
    if (context == null) return

    // 1. Direct notification action PendingIntent
    val actionSent = notification.trySendFirstAction(context, "play", "pause", "resume", "toggle")
    if (actionSent) return

    // 2. Notification MediaSession token
    val token = notification?.mediaToken
    if (token != null) {
        val success = runCatching {
            val controller = android.media.session.MediaController(context, token)
            if (notification.mediaIsPlaying) {
                controller.transportControls.pause()
            } else {
                controller.transportControls.play()
            }
            true
        }.getOrDefault(false)
        if (success) return
    }

    // 3. Delegate to NotificationListenerService via SmartIslandCommand
    runCatching {
        val repo = SmartIslandRepositories.notificationRepository(context)
        repo.sendCommand(SmartIslandCommand.PlayPause(notification?.packageName))
    }

    // 4. Fallback: AudioManager media key events
    runCatching {
        val audioManager = context.getSystemService(android.content.Context.AUDIO_SERVICE) as? android.media.AudioManager
        val keyCode = android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
        val down = android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, keyCode)
        val up = android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, keyCode)
        audioManager?.dispatchMediaKeyEvent(down)
        audioManager?.dispatchMediaKeyEvent(up)
    }
}

internal fun executeSwipeAction(
    action: SwipeAction,
    currentNotification: IslandNotification?,
    context: android.content.Context? = null,
    onDismiss: () -> Unit,
    onDismissAll: () -> Unit,
    onToggle: () -> Unit,
    onOpenNotification: (IslandNotification) -> Unit,
    onOpenFloatingWindow: () -> Unit,
    onOpenNotificationShade: () -> Unit,
    onPageSelected: (Int) -> Unit,
    notificationsSize: Int,
    currentIndex: Int
) {
    if (context != null && action != SwipeAction.None) {
        triggerHapticVibration(context)
    }
    when (action) {
        SwipeAction.DismissCurrent -> onDismiss()
        SwipeAction.DismissAll -> onDismissAll()
        SwipeAction.Collapse -> onToggle()
        SwipeAction.Expand -> onToggle()
        SwipeAction.OpenApp -> {
            if (currentNotification != null) {
                onOpenNotification(currentNotification)
            }
        }
        SwipeAction.FloatingWindow -> onOpenFloatingWindow()
        SwipeAction.NotificationShade -> onOpenNotificationShade()
        SwipeAction.NextPrevious, SwipeAction.NextNotification -> {
            if (notificationsSize > 1) {
                val nextIndex = (currentIndex + 1) % notificationsSize
                onPageSelected(nextIndex)
            } else {
                trySkipMedia(context, currentNotification, forward = true)
            }
        }
        SwipeAction.PreviousNotification -> {
            if (notificationsSize > 1) {
                val prevIndex = (currentIndex - 1 + notificationsSize) % notificationsSize
                onPageSelected(prevIndex)
            } else {
                trySkipMedia(context, currentNotification, forward = false)
            }
        }
        SwipeAction.NextTrack -> {
            trySkipMedia(context, currentNotification, forward = true)
        }
        SwipeAction.PreviousTrack -> {
            trySkipMedia(context, currentNotification, forward = false)
        }
        SwipeAction.PlayPause -> {
            tryPlayPauseMedia(context, currentNotification)
        }
        SwipeAction.None -> {
            // Disabled / No action
        }
    }
}

/** Light blue, the reference's primary colour. */
private val KineticGlowColor = Color(0xFF3186FF)

/**
 * Draws the glow as contiguous soft bands around the island's rounded rectangle, from its edge outward, each
 * fainter than the one inside it (a quadratic falloff), so it works the same on every Android version (no
 * hardware shadow-layer limits) and shows no gaps or rings. The island body is drawn on top.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawKineticGlow(radiusPx: Float, cornerPx: Float) {
    val steps = 14
    val band = radiusPx / steps
    for (i in steps downTo 1) {
        val centre = band * (i - 0.5f)
        val falloff = 1f - (i - 0.5f) / steps
        drawRoundRect(
            color = KineticGlowColor.copy(alpha = 0.40f * falloff * falloff),
            topLeft = androidx.compose.ui.geometry.Offset(-centre, -centre),
            size = androidx.compose.ui.geometry.Size(size.width + 2 * centre, size.height + 2 * centre),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerPx + centre, cornerPx + centre),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = band * 1.05f)
        )
    }
}
