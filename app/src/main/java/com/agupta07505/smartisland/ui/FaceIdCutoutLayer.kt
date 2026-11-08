/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.IslandEvent
import com.agupta07505.smartisland.util.AnimationSpeed
import com.agupta07505.smartisland.util.FaceIdController
import com.agupta07505.smartisland.util.SystemAnimScale
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin

private val CUTOUT_LEFT_PAD = 8.dp
private val CUTOUT_RIGHT_PAD = 12.dp
private const val CUTOUT_GLYPH_MAX_DP = 22f
private const val CUTOUT_GLYPH_OF_HEIGHT = 0.7f

private const val FACE_HEAD_UNITS = 110f

private const val CUTOUT_CARD_W_DP = 176f

private const val FP_CUTOUT_SCALE = 1.4f

private const val RESULT_IN_MS = 300
private const val RESULT_HOLD_MS = 350
private const val RESULT_START_SCALE = 0.7f

private const val RING_TURN_MS = 900
private const val RING_STROKE_OF_SIZE = 0.11f
private const val RING_MIN_SWEEP = 70f
private const val RING_MAX_SWEEP = 150f
private const val RING_TRACK_ALPHA = 0.18f

private class CutoutAnims {
    val scanT = Animatable(0f)
    val blink = Animatable(0f)
    val fade = Animatable(0f)
    val quit = Animatable(0f)
    val spin = Animatable(0f)
    val result = Animatable(0f)
    var gotFace by mutableStateOf(false)
    val splitT = Animatable(0f)
    val turnT = Animatable(0f)
    // Fingerprint unlock: face glyph to fingerprint, scan line place
    val fpIn = Animatable(0f)
    val fpScan = Animatable(FP_NO_SCAN)
    val arcFade = Animatable(0f)
    val shake = Animatable(0f)
    var fingerprintSplit by mutableStateOf(false)
}

private class CutoutPaths {
    val tick = facePathOf(FACE_ID_TICK)
    val cross = facePathOf(FACE_ID_CROSS)
    val disc = facePathOf(FACE_ID_DISC)
    val hole = facePathOf(FACE_ID_HOLE)
}

private class CutoutArcs(
    val track: FaceIdEdgeTrack,
    faceScale: Float,
    faceX: Float,
    tickScale: Float,
    tickX: Float,
    tickY: Float
) {
    // face outline becomes the left arc, tick becomes the right arc
    private val faceOutline = faceIdResample(FACE_ID_HOLE, FACE_ID_SPLIT_POINTS)
    private val tickOutline = faceIdResample(FACE_ID_TICK, FACE_ID_SPLIT_POINTS)
    private val arcs = run {
        val buffer = FloatArray(FACE_ID_ARC_OUTLINE_POINTS * 2)
        track.leftOutlineInto(0f, buffer)
        val left = faceIdResample(buffer, FACE_ID_SPLIT_POINTS)
        track.rightOutlineInto(0f, buffer)
        left to faceIdResample(buffer, FACE_ID_SPLIT_POINTS)
    }
    private val fpOutline = faceIdResample(FP_OUTLINE, FACE_ID_SPLIT_POINTS)
    val leftTo = faceIdAlign(faceOutline, arcs.first)
    val leftToFp = faceIdAlign(fpOutline, arcs.first)
    val leftFromFp = placeAt(fpOutline, faceScale * FP_CUTOUT_SCALE, faceX, tickY)
    val rightTo = faceIdAlign(tickOutline, arcs.second)
    val leftFrom = placeAt(faceOutline, faceScale, faceX, tickY)
    val rightFrom = placeAt(tickOutline, tickScale, tickX, tickY)
    val leftPoints = FloatArray(leftFrom.size)
    val rightPoints = FloatArray(rightFrom.size)
    val arcPoints = FloatArray(FACE_ID_ARC_OUTLINE_POINTS * 2)
    val leftPath = Path()
    val rightPath = Path()
}

private suspend fun CutoutAnims.playQuit(session: Int, speed: AnimationSpeed) {
    FaceIdController.state.first { it.session == session && it.cancelled }
    quit.animateTo(1f, tween(speed.durationMs(CANCEL_FADE_MS), easing = LinearEasing))
    FaceIdController.finish(session)
}

private suspend fun CutoutAnims.playSpin(speed: AnimationSpeed) {
    if (SystemAnimScale.animationsOff()) return
    spin.animateTo(
        360f,
        infiniteRepeatable(tween(speed.durationMs(RING_TURN_MS), easing = LinearEasing), RepeatMode.Restart)
    )
}

private suspend fun CutoutAnims.playScan(session: Int, speed: AnimationSpeed) {
    if (SystemAnimScale.animationsOff()) {
        FaceIdController.finish(session)
        return
    }
    coroutineScope {
        val normal = launch { playVerdict(session, speed) }
        var fingerprintStarted = false
        val fingerprintJob = launch {
            val direct = FaceIdController.state.first { it.session == session && it.fingerprint }.direct
            fingerprintStarted = true
            normal.cancelAndJoin()
            blink.snapTo(0f)
            shake.snapTo(0f)
            fade.snapTo(0f)
            playFingerprint(direct, session, speed)
        }
        normal.join()
        if (fingerprintStarted) fingerprintJob.join() else fingerprintJob.cancel()
    }
}

private suspend fun CutoutAnims.playVerdict(session: Int, speed: AnimationSpeed) {
    fun finalMs(time: Int): Int = speed.durationMs(time)
    fun scanMs(base: Int): Int = speed.durationMs((base / FACE_ID_SCAN_SPEEDUP).toInt().coerceAtLeast(1))
    suspend fun blinkOnce() {
        blink.animateTo(1f, tween(finalMs(BLINK_CLOSE_MS), easing = LinearEasing))
        blink.animateTo(0f, tween(finalMs(BLINK_OPEN_MS), easing = LinearEasing))
    }
    coroutineScope {
        val scanJob = launch {
            scanT.animateTo(FACE_ID_SCAN_END, tween(scanMs((FACE_ID_SCAN_END * 1000f).toInt()), easing = LinearEasing))
        }
        val blinkJob = launch {
            FaceIdController.state.first { it.session == session && it.anchored }
            delay(finalMs(BLINK_1_AT_MS).toLong())
            while (true) {
                blinkOnce()
                delay(finalMs(BLINK_PERIOD_MS - BLINK_CLOSE_MS - BLINK_OPEN_MS).toLong())
            }
        }
        val verdict = withTimeoutOrNull(finalMs(FaceIdController.state.value.waitMs).toLong()) {
            FaceIdController.state.first { it.session == session && (it.accepted || it.rejected) }
        }
        val accepted = verdict?.accepted == true
        blinkJob.cancelAndJoin()
        if (accepted && blink.value > 0f) blink.animateTo(0f, tween(finalMs(BLINK_OPEN_MS), easing = LinearEasing))
        if (scanT.value < FACE_ID_SCAN_END) {
            scanJob.cancelAndJoin()
            scanT.animateTo(FACE_ID_SCAN_END, tween(finalMs(SCAN_SETTLE_MS), easing = LinearEasing))
        }
        gotFace = accepted
        result.animateTo(1f, tween(finalMs(RESULT_IN_MS), easing = FastOutSlowInEasing))
        delay(finalMs(RESULT_HOLD_MS).toLong())
        if (accepted) {
            splitT.animateTo(1f, tween(finalMs(TICK_SPLIT_MS), easing = FastOutSlowInEasing))
            turnT.animateTo(1f, tween(finalMs(ARC_TURN_MS), easing = FastOutSlowInEasing))
            arcFade.animateTo(1f, tween(finalMs(ARC_FADE_MS), easing = LinearEasing))
        } else {
            delay(finalMs(CROSS_HOLD_MS).toLong())
            shake.animateTo(1f, tween(SHAKE_MS, easing = LinearEasing))
            fade.animateTo(1f, tween(finalMs(NO_FACE_CLOSE_MS), easing = FastOutSlowInEasing))
        }
        FaceIdController.finish(session)
    }
}

private suspend fun CutoutAnims.playFingerprint(direct: Boolean, session: Int, speed: AnimationSpeed) {
    fun finalMs(time: Int): Int = speed.durationMs(time)
    coroutineScope {
        if (direct) {
            fpIn.snapTo(1f)
            launch { scanT.animateTo(FACE_ID_EYES_START, tween(finalMs(FPD_FADE_IN_MS), easing = LinearEasing)) }
        } else {
            coroutineScope {
                launch { result.animateTo(0f, tween(finalMs(FP_MORPH_IN_MS), easing = LinearEasing)) }
                launch { fpIn.animateTo(1f, tween(finalMs(FP_MORPH_IN_MS), easing = LinearEasing)) }
                launch {
                    if (scanT.value < FACE_ID_EYES_START) {
                        scanT.animateTo(FACE_ID_EYES_START, tween(finalMs(FP_MORPH_IN_MS), easing = LinearEasing))
                    }
                }
            }
        }
        fpScan.snapTo(0f)
        fpScan.animateTo(1f, tween(finalMs(FP_SWEEP_UP_MS), easing = LinearEasing))
        fpScan.animateTo(0f, tween(finalMs(FP_SWEEP_DOWN_MS), easing = LinearEasing))
        fpScan.snapTo(FP_NO_SCAN)
        gotFace = true
        result.animateTo(1f, tween(finalMs(RESULT_IN_MS), easing = FastOutSlowInEasing))
        delay(finalMs(RESULT_HOLD_MS).toLong())
        fingerprintSplit = true
        splitT.animateTo(1f, tween(finalMs(TICK_SPLIT_MS), easing = FastOutSlowInEasing))
        turnT.animateTo(1f, tween(finalMs(ARC_TURN_MS), easing = FastOutSlowInEasing))
        arcFade.animateTo(1f, tween(finalMs(ARC_FADE_MS), easing = LinearEasing))
        FaceIdController.finish(session)
    }
}

private fun GraphicsLayerScope.applyCutoutFaceLayer(a: CutoutAnims) {
    val t = a.scanT.value
    alpha = (faceSmooth(0f, FACE_ID_FADE_IN_END, t) * (1f - a.fade.value)).coerceIn(0f, 1f)
    compositingStrategy = CompositingStrategy.Offscreen
    val settle = 1.12f - 0.12f * (1f - (1f - faceSmooth(0f, 1.4f, t)).pow(3))
    val s = settle * (1f - 0.06f * a.fade.value)
    scaleX = s
    scaleY = s
}

private fun DrawScope.drawCutoutFace(a: CutoutAnims, p: CutoutPaths, faceColor: Color = FACE_ID_NAVY) {
    val k = size.width / FACE_HEAD_UNITS
    withTransform({
        translate(size.width / 2f - FACE_ID_HEAD_CX * k, size.height / 2f - FACE_ID_HEAD_CY * k)
        scale(k, k, pivot = Offset.Zero)
    }) {
        val sp = a.splitT.value
        val fpi = a.fpIn.value
        drawPath(p.disc, faceColor.copy(alpha = (1f - sp) * (1f - fpi)))
        if (sp <= 0f) drawPath(p.hole, Color.White.copy(alpha = 1f - fpi))
        if (fpi > 0f && sp < 1f) {
            withTransform({ scale(FP_CUTOUT_SCALE, FP_CUTOUT_SCALE,
                pivot = Offset(FACE_ID_HEAD_CX, FACE_ID_HEAD_CY)) }) {
                drawFingerprint(alpha = fpi * (1f - sp), scan = a.fpScan.value)
            }
        }
        val t = a.scanT.value
        val eyeAlpha = (1f - sp) * (1f - fpi)
        if (t > FACE_ID_EYES_START - 0.02f && eyeAlpha > 0f) {
            val shut = 1.3f * a.blink.value
            drawEye(FACE_ID_HEAD_CX - FACE_ID_EYE_DX, faceEaseInOut(faceSmooth(FACE_ID_EYES_START, FACE_ID_EYES_END,
                t)) - shut, eyeAlpha, color = faceColor)
            drawEye(FACE_ID_HEAD_CX + FACE_ID_EYE_DX, faceEaseInOut(faceSmooth(FACE_ID_EYES_START + 0.05f,
                FACE_ID_EYES_END + 0.05f, t)) - shut, eyeAlpha, color = faceColor)
        }
    }
}

private fun DrawScope.drawCutoutRing(spin: Float) {
    val stroke = size.width * RING_STROKE_OF_SIZE
    val inset = stroke / 2f
    val arcSize = Size(size.width - stroke, size.height - stroke)
    drawArc(
        color = Color.White.copy(alpha = RING_TRACK_ALPHA),
        startAngle = 0f,
        sweepAngle = 360f,
        useCenter = false,
        topLeft = Offset(inset, inset),
        size = arcSize,
        style = Stroke(stroke)
    )
    val breathe = (sin(spin * 2f * PI.toFloat() / 180f) + 1f) / 2f
    drawArc(
        color = Color.White,
        startAngle = spin - 90f,
        sweepAngle = RING_MIN_SWEEP + (RING_MAX_SWEEP - RING_MIN_SWEEP) * breathe,
        useCenter = false,
        topLeft = Offset(inset, inset),
        size = arcSize,
        style = Stroke(stroke, cap = StrokeCap.Round)
    )
}

private fun GraphicsLayerScope.applyCutoutResultLayer(a: CutoutAnims) {
    alpha = if (a.splitT.value > 0f) 0f else (a.result.value * (1f - a.fade.value)).coerceIn(0f, 1f)
    val s = RESULT_START_SCALE + (1f - RESULT_START_SCALE) * a.result.value
    scaleX = s
    scaleY = s
}

private fun DrawScope.drawCutoutResult(mark: Path) {
    val k = size.width / FACE_HEAD_UNITS
    withTransform({
        translate(size.width / 2f - FACE_ID_HEAD_CX * k, size.height / 2f - FACE_ID_HEAD_CY * k)
        scale(k, k, pivot = Offset.Zero)
    }) {
        drawPath(mark, Color.White)
    }
}

private fun DrawScope.drawCutoutArcs(a: CutoutAnims, arcs: CutoutArcs) {
    val track = arcs.track
    val k = size.width / FACE_ID_DESIGN_W
    withTransform({ scale(k, k, pivot = Offset.Zero) }) {
        val sp = a.splitT.value
        if (sp >= 1f) {
            val turn = ARC_TURN_DIRECTION * a.turnT.value
            track.leftOutlineInto(turn * track.leftSwap, arcs.arcPoints)
            faceIdFillPath(arcs.leftPath, arcs.arcPoints)
            drawPath(arcs.leftPath, Color.White)
            track.rightOutlineInto(turn * track.rightSwap, arcs.arcPoints)
            faceIdFillPath(arcs.rightPath, arcs.arcPoints)
            drawPath(arcs.rightPath, Color.White)
        } else {
            val fingerprint = a.fingerprintSplit
            if (fingerprint) faceIdBlend(arcs.leftFromFp, arcs.leftToFp, sp,
                arcs.leftPoints) else faceIdBlend(arcs.leftFrom, arcs.leftTo, sp, arcs.leftPoints)
            faceIdFillPath(arcs.leftPath, arcs.leftPoints)
            drawPath(arcs.leftPath, Color.White.copy(alpha = if (fingerprint) faceSmooth(0f, 0.4f, sp) else 1f))
            faceIdBlend(arcs.rightFrom, arcs.rightTo, sp, arcs.rightPoints)
            faceIdFillPath(arcs.rightPath, arcs.rightPoints)
            drawPath(arcs.rightPath, Color.White)
        }
    }
}

@Composable
internal fun FaceIdCutoutCard(
    session: Int,
    settings: SmartIslandSettings,
    centerOffsetX: Dp,
    isLandscape: Boolean,
    topOffset: Dp,
    modifier: Modifier
) {
    val speed = AnimationSpeed.NORMAL
    val anims = remember { CutoutAnims() }
    val paths = remember { CutoutPaths() }
    // The colour the user gave the Face ID event, the navy it always had without one
    val faceColor = remember(settings.eventColors) {
        settings.eventColors[IslandEvent.FACE_UNLOCK.id]?.let { Color(it) } ?: FACE_ID_NAVY
    }
    val glyph = minOf(CUTOUT_GLYPH_MAX_DP, settings.height * CUTOUT_GLYPH_OF_HEIGHT).dp
    val cutW = settings.width.toFloat()
    val cutH = settings.height.toFloat()
    val notch = settings.enableNotchMode
    val cutR = faceIdCutoutCornerRadius(settings.cornerRadius, cutW, cutH, notch)
    val unitsPerDp = FACE_ID_DESIGN_W / CUTOUT_CARD_W_DP
    // Right glyph centre in design units, relative to the arc canvas
    val tickScale = glyph.value * unitsPerDp / FACE_HEAD_UNITS
    val faceScale = tickScale
    val faceX = FACE_ID_HEAD_CX + (CUTOUT_LEFT_PAD.value + glyph.value / 2f - cutW / 2f) * unitsPerDp
    val tickX = FACE_ID_HEAD_CX + (cutW / 2f - CUTOUT_RIGHT_PAD.value - glyph.value / 2f) * unitsPerDp
    val tickY = cutH * unitsPerDp / 2f
    val track = remember(cutW, cutH, cutR, notch) {
        FaceIdEdgeTrack(
            cx = FACE_ID_HEAD_CX,
            cy = cutH * unitsPerDp / 2f,
            w = cutW * unitsPerDp,
            h = cutH * unitsPerDp,
            r = cutR * unitsPerDp,
            thickness = FaceIdArcThickness.Sliver.dp * unitsPerDp,
            reach = FACE_ID_ARC_REACH_DP * unitsPerDp,
            inset = FACE_ID_ARC_INSET_DP * unitsPerDp,
            flatTop = notch,
            hiddenRise = FACE_ID_NOTCH_RISE_DP * unitsPerDp
        )
    }
    val arcs = remember(track, faceScale, faceX, tickScale, tickX, tickY) {
        CutoutArcs(track, faceScale, faceX, tickScale, tickX, tickY)
    }

    LaunchedEffect(session) { anims.playQuit(session, speed) }
    LaunchedEffect(session) { anims.playSpin(speed) }
    LaunchedEffect(session) { anims.playScan(session, speed) }

    Box(
        modifier = modifier
            .graphicsLayer {
                translationX = centerOffsetX.toPx()
                translationY = topOffset.toPx()
                alpha = 1f - anims.quit.value
                translationX += sin(2.0 * PI * SHAKE_CYCLES * anims.shake.value)
                    .toFloat() * SHAKE_AMPLITUDE_DP.dp.toPx()
            }
            .size(settings.width.dp, settings.height.dp)
    ) {
        Canvas(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = CUTOUT_LEFT_PAD)
                .size(glyph)
                .graphicsLayer { applyCutoutFaceLayer(anims) }
        ) {
            drawCutoutFace(anims, paths, faceColor)
        }

        Canvas(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = CUTOUT_RIGHT_PAD)
                .size(glyph)
                .graphicsLayer {
                    alpha = (faceSmooth(0.3f, FACE_ID_FADE_IN_END,
                        anims.scanT.value) * (1f - anims.result.value) * (1f - anims.fade.value))
                        .coerceIn(0f, 1f)
                }
        ) {
            drawCutoutRing(anims.spin.value)
        }

        Canvas(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = CUTOUT_RIGHT_PAD)
                .size(glyph)
                .graphicsLayer { applyCutoutResultLayer(anims) }
        ) {
            drawCutoutResult(if (anims.gotFace) paths.tick else paths.cross)
        }

        if (anims.splitT.value > 0f) {
            Canvas(
                modifier = Modifier
                    .wrapContentSize(Alignment.TopCenter, unbounded = true)
                    .requiredSize(CUTOUT_CARD_W_DP.dp, settings.height.dp)
                    .graphicsLayer { alpha = 1f - anims.arcFade.value }
            ) {
                drawCutoutArcs(anims, arcs)
            }
        }
    }
}

private fun placeAt(p: FloatArray, scale: Float, x: Float, y: Float): FloatArray {
    val out = FloatArray(p.size)
    var i = 0
    while (i + 1 < p.size) {
        out[i] = x + scale * (p[i] - FACE_ID_HEAD_CX)
        out[i + 1] = y + scale * (p[i + 1] - FACE_ID_HEAD_CY)
        i += 2
    }
    return out
}
