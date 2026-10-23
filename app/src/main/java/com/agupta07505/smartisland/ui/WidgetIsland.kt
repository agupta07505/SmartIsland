/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.IdleWidgetKind
import com.agupta07505.smartisland.model.WidgetIslandContent
import com.agupta07505.smartisland.model.WidgetIslandTimer
import com.agupta07505.smartisland.model.WIDGET_ISLAND_CORNER_RADIUS_DP
import com.agupta07505.smartisland.model.widgetIslandCentre
import com.agupta07505.smartisland.model.widgetIslandMustClose
import com.agupta07505.smartisland.model.widgetIslandSize
import com.agupta07505.smartisland.util.AnimationSpeed
import com.agupta07505.smartisland.util.HapticKind
import com.agupta07505.smartisland.util.HapticStrength
import com.agupta07505.smartisland.util.IslandHaptics

private const val WIDGET_ISLAND_STIFFNESS = 420f
private const val WIDGET_ISLAND_DAMPING = 0.82f

private const val WIDGET_ISLAND_BELOW_GAP_DP = 1f

private const val WIDGET_ISLAND_CONTENT_FROM = 0.55f

private const val WIDGET_ISLAND_PILL_BACK_AT = 0.02f

private const val WIDGET_ISLAND_SWIPE_DP = 28f

/** The semi-expanded idle widget island. */
@Composable
internal fun WidgetIslandLayer(
    open: Boolean,
    expanded: Boolean,
    hasCards: Boolean,
    /** A call or live event is on the island: it closes the widget island ([com.agupta07505.smartisland.model.cardClosesWidgetIsland]). */
    importantCard: Boolean,
    /** Counts the closes that must skip the closing animation (shade, screen off, lock, rotation). */
    snapCount: Int,
    settings: SmartIslandSettings,
    islandLayout: IslandLayout,
    ui: IslandInteractionState,
    screenWidthDp: Float,
    /** Landscape: the camera sits in a corner, not under the island, so the island needs no strip above its content. */
    isLandscape: Boolean,
    animSpeed: AnimationSpeed,
    onOpenChange: (Boolean) -> Unit,
    onShownChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val frame = ui.idleFrameNow
    val context = LocalContext.current
    // Plain memory, not state: the last frame keeps the island's content alive while it shrinks after the pill lost
    // its widget, and while a low-priority card sits on the pill underneath (the idle widget is off the pill then,
    // the island stays).
    val lastFrame = remember { arrayOfNulls<IdleWidgetFrame>(1) }
    if (frame != null) lastFrame[0] = frame
    val source = frame ?: lastFrame[0]
    // The pill's pause hook and the swipe hook read this: while the island is open the pill does not switch by
    // itself, and a swipe moves the island.
    SideEffect { ui.widgetIslandOpen = open }
    DisposableEffect(ui) { onDispose { ui.widgetIslandOpen = false } }

    // The island follows the widget on the pill (a swipe on the pill steps both). Its content has to exist (charger
    // plugged, an alarm still to come, ...).
    val hasContent = source == null || WidgetIslandContent.hasContent(
        kind = source.kind,
        charging = source.data.charging,
        nextAlarmMs = source.data.nextAlarmMs,
        nowMs = source.data.nowMs,
        hasWeather = source.data.weather != null,
        notificationCount = source.data.notificationSummary.total,
        modeActive = source.data.mode.active
    )
    // Only calls and live events close it ([cardClosesWidgetIsland]); other cards wait behind the island, and the
    // idle widget leaving the pill for them does not count as losing the widget.
    val waitingCard = hasCards && !importantCard
    val widgetAvailable = frame != null || (waitingCard && settings.idleWidgetsEnabled)
    val mustClose = widgetIslandMustClose(
        expanded = expanded,
        hasImportantCard = importantCard,
        widgetAvailable = widgetAvailable,
        hasContent = hasContent,
        compact = frame?.compact == true
    )
    LaunchedEffect(open, mustClose) { if (open && mustClose) onOpenChange(false) }

    // Auto close: after the set time without a touch. Any touch on the island or the pill (ui.touch) starts the count
    // again.
    val lastTouch = ui.userInteractionTimestamp
    LaunchedEffect(open, lastTouch, settings.widgetIslandCloseSeconds) {
        val delayMs = WidgetIslandTimer.closeDelayMs(settings.widgetIslandCloseSeconds)
        if (open && delayMs != null) {
            kotlinx.coroutines.delay(delayMs)
            onOpenChange(false)
        }
    }

    val strengthNow = androidx.compose.runtime.rememberUpdatedState(settings.hapticStrength)
    val progress = remember { Animatable(0f) }
    val stiffness = animSpeed.stiffness(WIDGET_ISLAND_STIFFNESS)
    LaunchedEffect(open) {
        progress.animateTo(
            if (open) 1f else 0f,
            spring(dampingRatio = WIDGET_ISLAND_DAMPING, stiffness = stiffness)
        )
    }
    // While the island is out of the pill the pill underneath is not drawn: the island is the cutout, grown. Flips at
    // the first and last pixel of growth.
    SideEffect { ui.widgetIslandCovers = { progress.value > WIDGET_ISLAND_PILL_BACK_AT } }
    DisposableEffect(ui) { onDispose { ui.widgetIslandCovers = { false } } }
    val shownChangeNow = androidx.compose.runtime.rememberUpdatedState(onShownChange)
    LaunchedEffect(progress, ui) {
        // The spring can swing a little past 0 on the way home: the pill stays hidden until it has really stopped, or
        // it would blink on and off.
        snapshotFlow { progress.value > 0.001f || progress.isRunning }.collect {
            ui.widgetIslandShown = it
            shownChangeNow.value(it)
        }
    }
    DisposableEffect(ui) {
        onDispose {
            ui.widgetIslandShown = false
            shownChangeNow.value(false)
        }
    }
    // The shade, the screen turning off or locking and a rotation close it with no closing animation (declared after
    // the effect above: it wins).
    LaunchedEffect(snapCount) { if (snapCount > 0) progress.snapTo(0f) }

    val kind = source?.kind
    if (source == null || kind == null) return

    // The notification island lists apps: it grows to the rows it has (the other islands keep the plain size).
    val listHeight = if (kind == com.agupta07505.smartisland.model.IdleWidgetKind.Notifications) {
        com.agupta07505.smartisland.model.NotificationCounts.islandContentHeightDp(
            com.agupta07505.smartisland.model.NotificationCounts.rows(source.data.notificationSummary)
        )
    } else if (kind == com.agupta07505.smartisland.model.IdleWidgetKind.Weather) {
        // The weather island grows with the extra rows the person picked (and that have a value now).
        val reading = source.data.weather
        if (reading == null) {
            0f
        } else {
            com.agupta07505.smartisland.model.WeatherDisplay.islandContentHeightDp(
                com.agupta07505.smartisland.model.WeatherDisplay.availableReadouts(
                    settings.weatherPrefs().effectiveReadouts(), reading, System.currentTimeMillis()
                )
            )
        }
    } else if (kind == com.agupta07505.smartisland.model.IdleWidgetKind.Date) {
        // The date island grows to also name the next calendar event.
        val next = source.data.nextEvent
        if (next != null && com.agupta07505.smartisland.model.CalendarRules.islandShows(next.beginMs, source.data.nowMs)) {
            com.agupta07505.smartisland.model.CalendarRules.ISLAND_EVENT_HEIGHT_DP
        } else {
            0f
        }
    } else {
        0f
    }
    val target = widgetIslandSize(settings.width, settings.height, screenWidthDp, listHeight)
    // Starts as the pill's own rectangle and moves down to sit below the cutout as it grows (portrait); landscape stays on the pill.
    val notchDp = if (isLandscape) 0f else settings.height + WIDGET_ISLAND_BELOW_GAP_DP
    val totalHeightDp = target.heightDp
    // The pill's centre from the screen centre, from the layout's own main-centre (right for the full-width window
    // the open island runs in).
    val pillCentre = islandLayout.mainCenter.value - screenWidthDp / 2f
    val islandCentre = widgetIslandCentre(pillCentre, target.widthDp, screenWidthDp)
    val shellColor = islandLayout.pillBackgroundColor.copy(alpha = settings.opacity.coerceIn(0f, 1f))
    val wide = remember(source.data, source.fahrenheit, kind) { source.asWide(kind) }

    Box(
        modifier = modifier
            .layout { measurable, _ ->
                val p = progress.value.coerceAtLeast(0f)
                if (p <= 0.001f && !progress.isRunning) {
                    layout(0, 0) {}
                } else {
                    val w = (lerpF(settings.width, target.widthDp, p).dp.toPx()).toInt().coerceAtLeast(0)
                    val h = (lerpF(settings.height, totalHeightDp, p).dp.toPx()).toInt().coerceAtLeast(0)
                    val placeable = measurable.measure(Constraints.fixed(w, h))
                    layout(w, h) { placeable.place(0, 0) }
                }
            }
            .graphicsLayer {
                val p = progress.value.coerceAtLeast(0f)
                translationX = lerpF(pillCentre, islandCentre, p).dp.toPx()
                translationY = lerpF(0f, notchDp, p).dp.toPx()
                val radius = lerpF(settings.cornerRadius, WIDGET_ISLAND_CORNER_RADIUS_DP, p).dp.toPx()
                shape = androidx.compose.foundation.shape.RoundedCornerShape(radius)
                clip = true
            }
            .drawBehind { drawRect(shellColor) }
            // A tap on the island itself is the island's: it must not fall through to the outside tap that closes it.
            .pointerInput(Unit) { detectTapGestures(onPress = { ui.touch() }) { } }
            // The island covers the pill, so a sideways swipe on it never reaches the pill's own handler: it steps
            // the widgets from here (left = next, right = previous, like on the pill).
            .pointerInput(Unit) {
                var total = 0f
                val threshold = WIDGET_ISLAND_SWIPE_DP.dp.toPx()
                detectHorizontalDragGestures(
                    onDragStart = {
                        total = 0f
                        ui.touch()
                    },
                    onDragEnd = {
                        if (kotlin.math.abs(total) >= threshold && ui.idleWidgetCycle?.invoke(total < 0f) == true) {
                            IslandHaptics.play(context, HapticStrength.fromId(strengthNow.value), HapticKind.Swipe)
                        }
                    },
                    onDragCancel = { total = 0f },
                    onHorizontalDrag = { _, dx -> total += dx }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .requiredSize(target.widthDp.dp, target.heightDp.dp)
                .graphicsLayer {
                    alpha = ((progress.value - WIDGET_ISLAND_CONTENT_FROM) / (1f - WIDGET_ISLAND_CONTENT_FROM)).coerceIn(0f, 1f)
                },
            contentAlignment = Alignment.Center
        ) {
            IdleWidgetIslandContent(
                frame = wide,
                batteryMetric = settings.widgetIslandBatteryMetric,
                paused = ui.idleCyclePaused,
                onTogglePause = {
                    ui.touch()
                    ui.idleCyclePaused = !ui.idleCyclePaused
                    IslandHaptics.play(context, HapticStrength.fromId(settings.hapticStrength), HapticKind.Swipe)
                }
            )
        }
    }
}

private fun lerpF(from: Float, to: Float, t: Float): Float = from + (to - from) * t

/**
 * Fills the overlay window while the widget island is open: a tap anywhere outside it closes it. The island itself consumes its own taps,
 * and the service gives the window the whole frame as touchable region while it is open (see SmartIslandOverlayService).
 */
internal fun Modifier.widgetIslandOutsideTap(active: Boolean, onTap: () -> Unit): Modifier =
    if (active) pointerInput(active) { detectTapGestures { onTap() } } else this

/**
 * Gives the pill's tap handling and the outside tap their calls, [IslandInteractionState.onWidgetGlyphTap] and
 * [IslandInteractionState.onWidgetValueTap]: it buzzes once like every other pill tap and flips the island (open
 * becomes closed and the other way round).
 */
@Composable
internal fun WidgetIslandWiring(
    ui: IslandInteractionState,
    open: Boolean,
    onOpenChange: (Boolean) -> Unit,
    settings: SmartIslandSettings
) {
    val context = LocalContext.current
    val openNow = androidx.compose.runtime.rememberUpdatedState(open)
    val changeNow = androidx.compose.runtime.rememberUpdatedState(onOpenChange)
    val strengthNow = androidx.compose.runtime.rememberUpdatedState(settings.hapticStrength)
    val toggle: () -> Unit = toggle@{
        // A widget with nothing to say (battery off the charger, no alarm set) has no island to open.
        if (!openNow.value) {
            val frame = ui.idleFrameNow
            val data = frame?.data
            if (frame == null || data == null || !WidgetIslandContent.hasContent(
                    kind = frame.kind,
                    charging = data.charging,
                    nextAlarmMs = data.nextAlarmMs,
                    nowMs = System.currentTimeMillis(),
                    hasWeather = data.weather != null,
                    notificationCount = data.notificationSummary.total,
                    modeActive = data.mode.active
                )
            ) return@toggle
        }
        ui.touch()
        IslandHaptics.play(context, HapticStrength.fromId(strengthNow.value), HapticKind.Swipe)
        changeNow.value(!openNow.value)
    }
    ui.onWidgetGlyphTap = toggle
    // The value slot does the same for now; its own hook, so it can be given another job later.
    ui.onWidgetValueTap = toggle
}
