/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.expanded

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BrightnessHigh
import androidx.compose.material.icons.rounded.BrightnessLow
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.Sos
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.ExternalTorchPattern
import com.agupta07505.smartisland.model.TorchBeam
import com.agupta07505.smartisland.model.TorchCardSize
import com.agupta07505.smartisland.model.TorchCardState
import com.agupta07505.smartisland.model.TorchChip
import com.agupta07505.smartisland.model.TorchHint
import com.agupta07505.smartisland.model.TorchPattern
import com.agupta07505.smartisland.model.TorchPatternClock
import com.agupta07505.smartisland.model.TorchStrobeSlider
import com.agupta07505.smartisland.model.TorchView
import com.agupta07505.smartisland.ui.bounceClick
import com.agupta07505.smartisland.util.TorchController
import com.agupta07505.smartisland.ui.IslandText as Text

private val WARN = Color(0xFFFF8658)
private val ON_BEAM = Color(0xFF2A1B00)
private val POWER_SIZE = 64.dp
private val POWER_OVERLAP = 8.dp
// The flashlight glyph is drawn on a 24-unit grid: its lens is the flat top edge of the head, 2 units down from the
// icon's top and 10 units wide (7 to 17). The beam starts exactly there, as wide as the lens
private val HEAD_WIDTH = POWER_SIZE * 10 / 24
private val HEAD_SINK = POWER_SIZE * 2 / 24
private val THUMB_INSET = 11.dp
private val BEAM_ICON_SIZE = 17.dp
private val GATE_MIN_HEIGHT = 176.dp
private val BEAM_ICON_TOP = 4.dp
private val BEAM_ICON_GAP = 2.dp
private val SLIDER_LABELS = listOf(1, 5, 10, 15, 20, 25, 30)

@Composable
fun FlashlightExpanded(
    bottomPadding: Dp,
    settings: SmartIslandSettings = SmartIslandSettings.Default
) {
    val state by TorchController.state.collectAsState()
    val beamColor = Color(settings.flashlightColor)
    val height = TorchCardSize.heightDp(LocalConfiguration.current.screenHeightDp.toFloat()).dp

    DisposableEffect(Unit) {
        onDispose {
            TorchController.setFingerDown(false)
            if (TorchController.state.value.gateShown) TorchController.gateCancel()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        // Keep the card open while a finger is down, including a quick tap.
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        TorchController.setFingerDown(event.changes.any { it.pressed })
                    }
                }
            }
            .padding(start = 16.dp, top = 14.dp, end = 16.dp, bottom = bottomPadding)
    ) {
        TorchHeader(state, beamColor)
        Stage(state, beamColor, Modifier.weight(1f))
        Box(
            Modifier.fillMaxWidth().then(
                if (state.view == TorchView.Strobe) Modifier.heightIn(min = GATE_MIN_HEIGHT) else Modifier
            ),
            contentAlignment = Alignment.BottomStart
        ) {
            if (state.view == TorchView.Strobe) StrobeControls(state, beamColor) else MainControls(state, beamColor)
            if (state.gateShown) StrobeGate(beamColor, Modifier.fillMaxWidth().heightIn(min = GATE_MIN_HEIGHT))
        }
    }
}

@Composable
private fun TorchHeader(state: TorchCardState, beamColor: Color) {
    val palette = LocalIslandPalette.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 6.dp, end = 0.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Rounded.FlashlightOn, null, tint = palette.textMuted, modifier = Modifier.size(18.dp))
            Text("Flashlight", color = palette.textMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        Text(
            text = chipText(state.chip),
            color = if (state.active) beamColor else palette.text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(palette.tint(0.1f))
                .padding(horizontal = 11.dp, vertical = 5.dp)
        )
    }
}

private fun chipText(chip: TorchChip): String = when (chip) {
    TorchChip.Off -> "Off"
    TorchChip.On -> "On"
    is TorchChip.OnAtLevel -> "On · Level ${chip.level}/${chip.max}"
    TorchChip.Sos -> "SOS"
    is TorchChip.Strobe -> "Strobe · ${chip.hz} Hz"
    TorchChip.StrobePaused -> "Strobe · paused"
}

private fun hintText(hint: TorchHint): String = when (hint) {
    TorchHint.TapToTurnOn -> "Tap the light to turn it on"
    TorchHint.TapToStop -> "Tap the light to stop"
    TorchHint.DragBeam -> "Drag along the beam to set brightness"
    TorchHint.NoBrightnessSteps -> "This phone has no brightness steps"
    TorchHint.OtherApp -> "Another app is running this"
}

/** The beam with its brightness ticks, and the light icon that is the power button. */
@Composable
private fun Stage(state: TorchCardState, beamColor: Color, modifier: Modifier) {
    val palette = LocalIslandPalette.current
    Box(modifier.fillMaxWidth()) {
        BeamZone(state, beamColor, Modifier.fillMaxSize().padding(top = 8.dp, bottom = POWER_SIZE - POWER_OVERLAP))
        val iconColor by animateColorAsState(if (state.active) beamColor else palette.textMuted, tween(60), label = "torchIcon")
        Icon(
            Icons.Rounded.FlashlightOn,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = POWER_OVERLAP)
                .size(POWER_SIZE)
                // Answers the touch itself (a press scale) instead of only after the finger lifts and the colour fades
                .bounceClick { TorchController.powerTap() }
                .semantics {
                    contentDescription = "Flashlight"
                    role = Role.Button
                    onClick { TorchController.powerTap(); true }
                }
        )
    }
}

/** Dragging along the beam sets the brightness while the torch is steadily on and the phone has steps. */
@Composable
private fun BeamZone(state: TorchCardState, beamColor: Color, modifier: Modifier) {
    val palette = LocalIslandPalette.current
    val canAdjust by rememberUpdatedState(state.canAdjust)
    val maxLevel by rememberUpdatedState(state.maxLevel)
    val level by rememberUpdatedState(state.level)

    var patternLit by remember { mutableStateOf(true) }
    val pattern = state.pattern
    val strobeHz by rememberUpdatedState(state.hz)
    LaunchedEffect(pattern) {
        if (pattern == null) return@LaunchedEffect
        patternLit = true
        val start = withFrameNanos { it }
        while (true) {
            withFrameNanos { now ->
                patternLit = TorchPatternClock.lit(pattern, strobeHz, (now - start) / 1_000_000L)
            }
        }
    }
    // The beam lights quickly and fades out slower. Reach and strength follow the brightness step only, they are not
    // also ramped up from 0: multiplied with the fade that made the beam crawl in behind the icon
    val steadyAlpha by animateFloatAsState(
        if (state.on) 1f else 0f,
        if (state.on) tween(90, easing = LinearOutSlowInEasing) else tween(220),
        label = "beamFade"
    )
    val external = state.external
    val externalLit = TorchController.externalLit.collectAsState()
    // Read inside the draw lambda: another app's strobe flips this dozens of times a second, and each flip must only
    // redraw the beam, not recompose the whole card
    val beamVisible: () -> Float = {
        when {
            pattern != null -> if (patternLit) 1f else 0f
            external != null -> if (externalLit.value) 1f else 0f
            else -> steadyAlpha
        }
    }
    val reach by animateFloatAsState(
        TorchBeam.reach(state.beamLevel, state.maxLevel), tween(220), label = "beamReach"
    )
    val strength by animateFloatAsState(
        TorchBeam.alpha(state.beamLevel, state.maxLevel), tween(220), label = "beamAlpha"
    )
    val showTicks = state.supportsBrightness && state.active && pattern == null && external == null && state.view == TorchView.Main

    BoxWithConstraints(
        modifier = modifier
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (!canAdjust) return@awaitEachGesture
                    down.consume()
                    fun apply(y: Float) = TorchController.setLevel(
                        TorchBeam.levelAt(size.height - y, size.height.toFloat(), maxLevel)
                    )
                    apply(down.position.y)
                    do {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        change.consume()
                        if (!change.changedToUp()) apply(change.position.y)
                    } while (!change.changedToUp())
                }
            }
            .semantics {
                contentDescription = "Brightness"
                if (canAdjust) {
                    progressBarRangeInfo = ProgressBarRangeInfo(level.toFloat(), 1f..maxLevel.toFloat(), maxLevel - 1)
                    setProgress { value ->
                        TorchController.setLevel(value.toInt())
                        true
                    }
                }
            }
    ) {
        val zoneHeight = maxHeight
        val tickOnColor = beamColor
        val tickOffColor = palette.tint(0.2f)
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val visible = beamVisible()
            if (visible > 0.01f && reach > 0.01f) {
                val cone = Path().apply {
                    moveTo(w * 0.15f, 0f)
                    lineTo(w * 0.85f, 0f)
                    val tip = h + HEAD_SINK.toPx()
                    lineTo(w / 2 + HEAD_WIDTH.toPx() / 2, tip)
                    lineTo(w / 2 - HEAD_WIDTH.toPx() / 2, tip)
                    close()
                }
                val reachPx = h * reach
                val a = strength * visible
                drawPath(
                    cone,
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to beamColor.copy(alpha = 0f),
                            0.55f to beamColor.copy(alpha = a * 0.22f),
                            0.82f to beamColor.copy(alpha = a * 0.55f),
                            1f to beamColor.copy(alpha = a)
                        ),
                        startY = h - reachPx,
                        endY = h
                    )
                )
            }
            if (showTicks) {
                val markerY = h - h * reach
                drawLine(
                    beamColor.copy(alpha = 0.95f), Offset(w * 0.14f, markerY), Offset(w * 0.86f, markerY),
                    strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(0f, 7.dp.toPx()))
                )
                for (i in 1..maxLevel) {
                    val y = h - h * TorchBeam.reach(i, maxLevel) - 1.dp.toPx()
                    if (!TorchBeam.tickClearsIcon(y, (BEAM_ICON_TOP + BEAM_ICON_SIZE + BEAM_ICON_GAP).toPx())) continue
                    val on = i <= level
                    drawLine(
                        if (on) tickOnColor else tickOffColor,
                        Offset(8.dp.toPx(), y), Offset(8.dp.toPx() + (if (on) 14.dp else 9.dp).toPx(), y),
                        strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round
                    )
                }
            }
        }
        if (showTicks) {
            Icon(
                Icons.Rounded.BrightnessHigh, null, tint = palette.textMuted,
                modifier = Modifier.offset(x = 7.dp, y = BEAM_ICON_TOP).size(BEAM_ICON_SIZE)
            )
            Icon(
                Icons.Rounded.BrightnessLow, null, tint = palette.textMuted,
                modifier = Modifier
                    // The lambda overload: zoneHeight comes from the layout constraints, which are state
                    .offset { IntOffset(7.dp.roundToPx(), (zoneHeight - zoneHeight * TorchBeam.reach(1, maxLevel) + 6.dp).roundToPx()) }
                    .size(BEAM_ICON_SIZE)
            )
        }
    }
}

@Composable
private fun MainControls(state: TorchCardState, beamColor: Color) {
    val palette = LocalIslandPalette.current
    Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            ModePill(Icons.Rounded.FlashOn, "Strobe", state.external is ExternalTorchPattern.Strobe, beamColor) { TorchController.strobeTap() }
            ModePill(Icons.Rounded.Sos, "SOS", state.pattern == TorchPattern.Sos || state.external == ExternalTorchPattern.Sos, beamColor) { TorchController.sosTap() }
        }
        Text(
            text = hintText(state.hint),
            color = palette.textMuted,
            fontSize = 11.5.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ModePill(icon: ImageVector, label: String, active: Boolean, beamColor: Color, onClick: () -> Unit) {
    val palette = LocalIslandPalette.current
    val content = if (active) ON_BEAM else palette.text
    Row(
        modifier = Modifier
            .height(44.dp)
            .clip(RoundedCornerShape(50))
            .background(if (active) beamColor else palette.tint(0.1f))
            .border(1.dp, if (active) Color.Transparent else palette.tint(0.12f), RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(start = 13.dp, end = 17.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = content, modifier = Modifier.size(20.dp))
        Text(label, color = content, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun StrobeControls(state: TorchCardState, beamColor: Color) {
    val palette = LocalIslandPalette.current
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(palette.tint(0.1f))
                    .border(1.dp, palette.tint(0.12f), CircleShape)
                    .clickable { TorchController.back() }
                    .semantics { contentDescription = "Back to flashlight" },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = palette.text, modifier = Modifier.size(20.dp))
            }
            Text("Strobe", color = palette.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text("${state.hz}", color = palette.text, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
            Text("Hz", color = palette.textMuted, fontSize = 13.sp, modifier = Modifier.offset(x = (-7).dp))
        }
        StrobeSlider(state.hz, beamColor)
        Text(
            text = strobeNote(state),
            color = palette.textTertiary,
            fontSize = 12.sp,
            lineHeight = 17.sp
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Rounded.Warning, null, tint = WARN, modifier = Modifier.padding(top = 1.dp).size(15.dp))
            Text(
                "Flashing between 3 and 30 times a second can trigger seizures in people with photosensitive epilepsy.",
                color = palette.textMuted, fontSize = 11.sp, lineHeight = 15.sp
            )
        }
    }
}

private fun strobeNote(state: TorchCardState): String {
    val tail = when {
        state.hz <= 10 -> "Smooth on every phone."
        state.hz <= TorchCardState.RELIABLE_HZ -> "Smooth on most phones."
        else -> "Extended range: some phones skip flashes above ${TorchCardState.RELIABLE_HZ} Hz."
    }
    val start = if (state.pattern == TorchPattern.Strobe) "" else " Tap the light to start."
    return "${TorchCardState.cycleMs(state.hz)} ms per cycle. $tail$start"
}

/** The 1 to 30 Hz track: a plain zone up to 20 Hz, a hatched warning zone above, the fill, the thumb and the labels. */
@Composable
private fun StrobeSlider(hz: Int, beamColor: Color) {
    val palette = LocalIslandPalette.current
    val currentHz by rememberUpdatedState(hz)
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(46.dp)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    val inset = THUMB_INSET.toPx()
                    fun apply(x: Float) =
                        TorchController.setHz(TorchStrobeSlider.hzAt((x - inset) / (size.width - 2 * inset)))
                    apply(down.position.x)
                    do {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        change.consume()
                        if (!change.changedToUp()) apply(change.position.x)
                    } while (!change.changedToUp())
                }
            }
            .semantics {
                contentDescription = "Strobe frequency in hertz"
                progressBarRangeInfo = ProgressBarRangeInfo(
                    currentHz.toFloat(), TorchCardState.MIN_HZ.toFloat()..TorchCardState.MAX_HZ.toFloat(),
                    TorchCardState.MAX_HZ - TorchCardState.MIN_HZ - 1
                )
                setProgress { value ->
                    TorchController.setHz(value.toInt())
                    true
                }
            }
    ) {
        val trackWidth = maxWidth - THUMB_INSET * 2
        val extended = hz > TorchCardState.RELIABLE_HZ
        Canvas(Modifier.fillMaxSize()) {
            val inset = THUMB_INSET.toPx()
            val width = size.width - 2 * inset
            val y = 14.dp.toPx()
            val stroke = 6.dp.toPx()
            val reliableX = inset + width * TorchStrobeSlider.reliableFraction
            val thumbX = inset + width * TorchStrobeSlider.fractionOf(hz)
            drawLine(palette.tint(0.16f), Offset(inset, y), Offset(reliableX, y), stroke)
            drawLine(WARN.copy(alpha = 0.4f), Offset(reliableX, y), Offset(inset + width, y), stroke)
            drawLine(if (extended) WARN else beamColor, Offset(inset, y), Offset(thumbX, y), stroke, StrokeCap.Round)
            drawCircle(Color.White, radius = 11.dp.toPx(), center = Offset(thumbX, y))
        }
        SLIDER_LABELS.forEach { value ->
            Text(
                text = "$value",
                color = if (value == TorchCardState.RELIABLE_HZ) WARN else palette.textMuted,
                fontSize = 10.5.sp,
                fontWeight = if (value == TorchCardState.RELIABLE_HZ) FontWeight.SemiBold else FontWeight.Normal,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .width(24.dp)
                    .offset(
                        x = THUMB_INSET + trackWidth * TorchStrobeSlider.fractionOf(value) - 12.dp,
                        y = 30.dp
                    )
            )
        }
    }
}

@Composable
private fun StrobeGate(beamColor: Color, modifier: Modifier) {
    val palette = LocalIslandPalette.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(palette.pick(Color(0xFF0B0D10), Color(0xFFE4E2E6)))
            .border(1.dp, palette.tint(0.12f), RoundedCornerShape(24.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.Warning, null, tint = WARN, modifier = Modifier.size(20.dp))
            Text("Flashing light warning", color = palette.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
        Text(
            "Strobe flashes the light up to 30 times a second. This can trigger seizures in people with photosensitive epilepsy. Start only if it is safe for everyone nearby.",
            color = palette.textSecondary, fontSize = 12.sp, lineHeight = 16.sp,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GateButton("Cancel", false, beamColor, Modifier.weight(1f)) { TorchController.gateCancel() }
            GateButton("Start strobe", true, beamColor, Modifier.weight(1f)) { TorchController.gateAccept() }
        }
    }
}

@Composable
private fun GateButton(label: String, primary: Boolean, beamColor: Color, modifier: Modifier, onClick: () -> Unit) {
    val palette = LocalIslandPalette.current
    Box(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(50))
            .background(if (primary) beamColor else palette.tint(0.1f))
            .border(1.dp, if (primary) Color.Transparent else palette.tint(0.12f), RoundedCornerShape(50))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (primary) ON_BEAM else palette.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}
