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
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawTransform
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.util.AnimationSpeed
import com.agupta07505.smartisland.util.FaceIdController
import com.agupta07505.smartisland.util.SystemAnimScale
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.pow

internal object FaceIdShell {
    var covering by mutableStateOf(false)
}

private val FACE_CARD_W = 176.dp
private val FACE_CARD_H = 148.dp

private const val SQUARE_SIDE_OF_CUT_W = 2f / 3f

private const val FACE_FILL = 0.56f

private const val FACE_HEAD_DP_AT_SCALE_1 = 88f
internal val FACE_ID_NAVY = Color(0xFF22346C)

/** Accept timings in ms at 1x animation speed. */
internal const val FACE_TO_TICK_MS = 300
internal const val TICK_BEAT_MS = 80
internal const val TICK_SPLIT_MS = 370
internal const val ARC_TURN_MS = 500
internal const val ARC_FADE_MS = 230

/** Fade-out time when the scan is cancelled. */
internal const val CANCEL_FADE_MS = 180

/** No-face sequence timings in ms from scan start, at 1x speed. */
internal const val BLINK_1_AT_MS = 800
internal const val BLINK_2_AT_MS = 1700
internal const val BLINK_CLOSE_MS = 70
internal const val BLINK_OPEN_MS = 90
private const val EYES_SHUT_MS = 200
private const val CROSS_MS = 650
internal const val CROSS_HOLD_MS = 150
internal const val SHAKE_MS = 500
internal const val SHAKE_CYCLES = 4
internal const val SHAKE_AMPLITUDE_DP = 4f
internal const val NO_FACE_CLOSE_MS = 250

internal const val ARC_TURN_DIRECTION = -1f
private const val FACE_ID_HEAD_END_SCALE = 0.75f

internal const val FACE_ID_SCAN_SPEEDUP = 2.7f

private const val CARD_OPEN_MS = 350

internal const val SCAN_SETTLE_MS = 120

internal const val BLINK_PERIOD_MS = BLINK_2_AT_MS - BLINK_1_AT_MS

internal const val FACE_ID_FADE_IN_END = 1.25f
internal const val FACE_ID_EYES_START = 1.35f
internal const val FACE_ID_EYES_END = 2.05f
internal const val FACE_ID_SCAN_END = 2.1f

internal fun faceSmooth(a: Float, b: Float, x: Float): Float {
    val t = ((x - a) / (b - a)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

internal fun faceEaseInOut(p: Float): Float =
    if (p < 0.5f) 4f * p * p * p else 1f - (-2f * p + 2f).pow(3) / 2f

internal fun facePathOf(points: FloatArray): Path = Path().apply {
    moveTo(points[0], points[1])
    var i = 2
    while (i + 1 < points.size) {
        lineTo(points[i], points[i + 1])
        i += 2
    }
    close()
}

internal fun faceIdFillPath(path: Path, points: FloatArray) {
    path.rewind()
    path.moveTo(points[0], points[1])
    var i = 2
    while (i + 1 < points.size) {
        path.lineTo(points[i], points[i + 1])
        i += 2
    }
    path.close()
}

private fun faceIdPlace(p: FloatArray, scale: Float, dy: Float): FloatArray {
    val out = FloatArray(p.size)
    var i = 0
    while (i + 1 < p.size) {
        out[i] = FACE_ID_HEAD_CX + scale * (p[i] - FACE_ID_HEAD_CX)
        out[i + 1] = FACE_ID_HEAD_CY + dy + scale * (p[i + 1] - FACE_ID_HEAD_CY)
        i += 2
    }
    return out
}

internal fun DrawScope.drawEye(cx: Float, open: Float, alpha: Float = 1f) {
    val cy = FACE_ID_HEAD_CY + FACE_ID_EYE_DY
    val top = cy + FACE_ID_EYE_R * (0.5f - 1.5f * open)
    clipRect(
        left = cx - FACE_ID_EYE_R - 1f,
        top = top,
        right = cx + FACE_ID_EYE_R + 1f,
        bottom = cy + FACE_ID_EYE_R + 1f
    ) {
        drawCircle(FACE_ID_NAVY.copy(alpha = alpha), radius = FACE_ID_EYE_R, center = Offset(cx, cy))
    }
}

@Composable
internal fun FaceIdLayer(
    settings: SmartIslandSettings,
    pillColor: Color,
    centerOffsetX: Dp,
    isLandscape: Boolean,
    topOffset: Dp = 0.dp,
    modifier: Modifier = Modifier
) {
    val state by FaceIdController.state.collectAsState()
    if (state.active) {
        key(state.session) {
            if (state.style == FaceIdController.Style.Cutout) {
                FaceIdCutoutCard(state.session, settings, centerOffsetX, isLandscape, topOffset, modifier)
            } else {
                FaceIdCard(state.session, settings, pillColor, centerOffsetX, isLandscape, topOffset, modifier)
            }
        }
    }
}

@Composable
private fun FaceIdCard(
    session: Int,
    settings: SmartIslandSettings,
    pillColor: Color,
    centerOffsetX: Dp,
    isLandscape: Boolean,
    topOffset: Dp,
    modifier: Modifier
) {
    val speed = AnimationSpeed.NORMAL
    DisposableEffect(Unit) {
        FaceIdShell.covering = true
        onDispose { FaceIdShell.covering = false }
    }
    val open = remember { Animatable(0f) }
    val scanT = remember { Animatable(0f) }
    val blink = remember { Animatable(0f) }
    val fade = remember { Animatable(0f) }
    // 0 = face, 1 = tick
    val morph = remember { Animatable(0f) }
    val disc = remember { facePathOf(FACE_ID_DISC) }
    val hole = remember { facePathOf(FACE_ID_HOLE) }
    val outlines = remember { faceIdMorphOutlines() }
    val morphPoints = remember { FloatArray(outlines.first.size) }
    val morphPath = remember { Path() }
    // 0 = tick, 1 = arcs
    val splitT = remember { Animatable(0f) }
    val arcFade = remember { Animatable(0f) }
    // fingerprint unlock: face to fingerprint, scan line place, fingerprint to tick
    val fpIn = remember { Animatable(0f) }
    val fpScan = remember { Animatable(FP_NO_SCAN) }
    val fpTick = remember { Animatable(0f) }
    val fpTickOutlines = remember { faceIdFingerprintTickOutlines() }
    val fpTickPoints = remember { FloatArray(fpTickOutlines.first.size) }
    val fpTickPath = remember { Path() }
    val quit = remember { Animatable(0f) }
    // 0..1 = half a turn around the edge
    val turnT = remember { Animatable(0f) }
    val tickPath = remember { facePathOf(FACE_ID_TICK) }
    // no-face ending: face to cross, then shake
    val crossT = remember { Animatable(0f) }
    val shake = remember { Animatable(0f) }
    val crossOutlines = remember { faceIdCrossMorphOutlines() }
    val crossPoints = remember { FloatArray(crossOutlines.first.size) }
    val crossMorphPath = remember { Path() }
    val crossPath = remember { facePathOf(FACE_ID_CROSS) }
    // collapsed cutout size; radius clamped to half the smaller side
    val cutW = settings.width
    val cutH = settings.height
    val cutR = minOf(settings.cornerRadius, cutW / 2f, cutH / 2f)
    val unitsPerDp = FACE_ID_DESIGN_W / FACE_CARD_W.value
    // square sits flush below the cutout
    val cutWf = cutW.toFloat()
    val cutHf = cutH.toFloat()
    val side = cutWf * SQUARE_SIDE_OF_CUT_W
    val squareTop = cutHf
    val squareR = minOf(cutR.toFloat(), side / 2f)
    val faceDiameterDp = side * FACE_FILL
    val faceA = faceDiameterDp / FACE_HEAD_DP_AT_SCALE_1
    val faceDy = (squareTop + side / 2f) * unitsPerDp - FACE_ID_HEAD_CY
    val canvasH = maxOf(FACE_CARD_H.value, squareTop + side + 8f).dp
    val track = remember(cutW, cutH, cutR) {
        FaceIdEdgeTrack(
            cx = FACE_ID_HEAD_CX,
            cy = cutH * unitsPerDp / 2f,
            w = cutW * unitsPerDp,
            h = cutH * unitsPerDp,
            r = cutR * unitsPerDp,
            thickness = FaceIdArcThickness.Sliver.dp * unitsPerDp,
            reach = FACE_ID_ARC_REACH_DP * unitsPerDp,
            inset = FACE_ID_ARC_INSET_DP * unitsPerDp
        )
    }
    val split = remember(track) { faceIdSplitOutlines(track) }
    val leftFromAtFace = remember(split, faceA, faceDy) { faceIdPlace(split.leftFrom, faceA, faceDy) }
    val rightFromAtFace = remember(split, faceA, faceDy) { faceIdPlace(split.rightFrom, faceA, faceDy) }
    val leftPoints = remember(split) { FloatArray(split.leftFrom.size) }
    val rightPoints = remember(split) { FloatArray(split.rightFrom.size) }
    val arcPoints = remember(track) { FloatArray(FACE_ID_ARC_OUTLINE_POINTS * 2) }
    val leftPath = remember { Path() }
    val rightPath = remember { Path() }

    LaunchedEffect(session) {
        FaceIdController.state.first { it.session == session && it.cancelled }
        coroutineScope {
            launch { open.animateTo(0f, tween(speed.durationMs(TICK_SPLIT_MS), easing = FastOutSlowInEasing)) }
            fade.animateTo(1f, tween(speed.durationMs(TICK_SPLIT_MS), easing = FastOutSlowInEasing))
        }
        FaceIdController.finish(session)
    }

    LaunchedEffect(session) {
        if (SystemAnimScale.animationsOff()) {
            FaceIdController.finish(session)
            return@LaunchedEffect
        }
        fun finalMs(time: Int): Int = speed.durationMs(time)
        fun scanMs(base: Int): Int = speed.durationMs((base / FACE_ID_SCAN_SPEEDUP).toInt().coerceAtLeast(1))
        suspend fun blinkOnce() {
            blink.animateTo(1f, tween(finalMs(BLINK_CLOSE_MS), easing = LinearEasing))
            blink.animateTo(0f, tween(finalMs(BLINK_OPEN_MS), easing = LinearEasing))
        }
        val normal = launch {
            open.animateTo(1f, tween(finalMs(CARD_OPEN_MS), easing = FastOutSlowInEasing))
            val scanJob = launch { scanT.animateTo(FACE_ID_SCAN_END, tween(scanMs((FACE_ID_SCAN_END * 1000f).toInt()), easing = LinearEasing)) }
            val blinkJob = launch {
                // blink clock starts at the system's real scan start (app lock shows the card earlier)
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
            if (accepted) {
                morph.animateTo(1f, tween(finalMs(FACE_TO_TICK_MS), easing = FastOutSlowInEasing))
                delay(finalMs(TICK_BEAT_MS).toLong())
                if (FaceIdController.state.value.style == FaceIdController.Style.SquareTick) {
                    // stop on the tick: the square collapses into the cutout, no arcs
                    val closing = launch { open.animateTo(0f, tween(finalMs(TICK_SPLIT_MS), easing = FastOutSlowInEasing)) }
                    fade.animateTo(1f, tween(finalMs(TICK_SPLIT_MS), easing = FastOutSlowInEasing))
                    closing.join()
                    FaceIdController.finish(session)
                    return@launch
                }
                // tick splits into arcs while the card closes
                val closing = launch { open.animateTo(0f, tween(finalMs(TICK_SPLIT_MS), easing = FastOutSlowInEasing)) }
                splitT.animateTo(1f, tween(finalMs(TICK_SPLIT_MS), easing = FastOutSlowInEasing))
                closing.join()
                // arcs swap sides, then fade
                turnT.animateTo(1f, tween(finalMs(ARC_TURN_MS), easing = FastOutSlowInEasing))
                arcFade.animateTo(1f, tween(finalMs(ARC_FADE_MS), easing = LinearEasing))
                FaceIdController.finish(session)
                return@launch
            }
            // no face: eyes shut, cross, shake, close
            blink.animateTo(1f, tween(finalMs(EYES_SHUT_MS), easing = FastOutSlowInEasing))
            crossT.animateTo(1f, tween(finalMs(CROSS_MS), easing = FastOutSlowInEasing))
            delay(finalMs(CROSS_HOLD_MS).toLong())
            shake.animateTo(1f, tween(SHAKE_MS, easing = LinearEasing))
            val closing = launch { open.animateTo(0f, tween(finalMs(NO_FACE_CLOSE_MS), easing = FastOutSlowInEasing)) }
            fade.animateTo(1f, tween(finalMs(NO_FACE_CLOSE_MS), easing = FastOutSlowInEasing))
            closing.join()
            FaceIdController.finish(session)
        }
        var fingerprintStarted = false
        val fingerprintJob = launch {
            val direct = FaceIdController.state.first { it.session == session && it.fingerprint }.direct
            fingerprintStarted = true
            normal.cancelAndJoin()
            blink.snapTo(0f)
            shake.snapTo(0f)
            fade.snapTo(0f)
            if (direct) {
                fpIn.snapTo(1f)
                launch { open.animateTo(1f, tween(finalMs(CARD_OPEN_MS), easing = FastOutSlowInEasing)) }
                launch { scanT.animateTo(FACE_ID_EYES_START, tween(finalMs(FPD_FADE_IN_MS), easing = LinearEasing)) }
            } else {
                coroutineScope {
                    launch { fpIn.animateTo(1f, tween(finalMs(FP_MORPH_IN_MS), easing = LinearEasing)) }
                    launch { if (open.value < 1f) open.animateTo(1f, tween(finalMs(FP_MORPH_IN_MS), easing = FastOutSlowInEasing)) }
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
            fpTick.animateTo(1f, tween(finalMs(FP_TO_TICK_MS), easing = FastOutSlowInEasing))
            delay(finalMs(FP_HOLD_MS).toLong())
            if (FaceIdController.state.value.style == FaceIdController.Style.SquareTick) {
                val closing = launch { open.animateTo(0f, tween(finalMs(TICK_SPLIT_MS), easing = FastOutSlowInEasing)) }
                fade.animateTo(1f, tween(finalMs(TICK_SPLIT_MS), easing = FastOutSlowInEasing))
                closing.join()
            } else {
                val closing = launch { open.animateTo(0f, tween(finalMs(TICK_SPLIT_MS), easing = FastOutSlowInEasing)) }
                splitT.animateTo(1f, tween(finalMs(TICK_SPLIT_MS), easing = FastOutSlowInEasing))
                closing.join()
                turnT.animateTo(1f, tween(finalMs(ARC_TURN_MS), easing = FastOutSlowInEasing))
                arcFade.animateTo(1f, tween(finalMs(ARC_FADE_MS), easing = LinearEasing))
            }
            FaceIdController.finish(session)
        }
        normal.join()
        if (fingerprintStarted) fingerprintJob.join() else fingerprintJob.cancel()
    }

    val o = open.value
    val sqW = lerp(cutW.dp, side.dp, o)
    val sqH = lerp(cutH.dp, side.dp, o)
    val sqY = lerp(0.dp, squareTop.dp, o)
    val sqR = lerp(cutR.dp, squareR.dp, o)
    val sqAlpha = settings.opacity.coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .graphicsLayer {
                translationX = centerOffsetX.toPx()
                translationY = topOffset.toPx()
                alpha = 1f - quit.value
            }
            .size(cutW.dp, cutH.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .wrapContentSize(Alignment.TopCenter, unbounded = true)
                .offset(y = sqY)
                .requiredSize(sqW, sqH)
                .clip(RoundedCornerShape(sqR))
                .background(pillColor.copy(alpha = sqAlpha))
        )
        Box(
            modifier = Modifier
                .wrapContentSize(Alignment.TopCenter, unbounded = true)
                .offset(y = sqY)
                .requiredSize(sqW, sqH)
                .then(if (splitT.value > 0f) Modifier else Modifier.clip(RoundedCornerShape(sqR))),
            contentAlignment = Alignment.TopCenter
        ) {
        Canvas(
            modifier = Modifier
                // pin the oversized canvas to the cutout's top centre
                .wrapContentSize(Alignment.TopCenter, unbounded = true)
                .offset(y = -sqY)
                .requiredSize(FACE_CARD_W, canvasH)
                .graphicsLayer {
                    val t = scanT.value
                    alpha = (faceSmooth(0f, FACE_ID_FADE_IN_END, t) * (1f - fade.value) * (1f - arcFade.value)).coerceIn(0f, 1f)
                    compositingStrategy = CompositingStrategy.Offscreen
                    // settles from slightly large; shrinks a bit on fade-out
                    val settle = 1.12f - 0.12f * (1f - (1f - faceSmooth(0f, 1.4f, t)).pow(3))
                    val s = settle * (1f - 0.06f * fade.value)
                    scaleX = s
                    scaleY = s
                    // no-face shake
                    translationX = kotlin.math.sin(2.0 * Math.PI * SHAKE_CYCLES * shake.value).toFloat() * SHAKE_AMPLITUDE_DP.dp.toPx()
                }
        ) {
            val k = size.width / FACE_ID_DESIGN_W
            withTransform({ scale(k, k, pivot = Offset.Zero) }) {
                val m = morph.value
                val fpi = fpIn.value
                val headAlpha = ((1f - m) * (1f - fpi)).coerceIn(0f, 1f)
                val headScale = 1f - (1f - FACE_ID_HEAD_END_SCALE) * m
                val headPivot = Offset(FACE_ID_HEAD_CX, FACE_ID_HEAD_CY)
                val faceT: DrawTransform.() -> Unit = {
                    translate(0f, faceDy)
                    scale(faceA, faceA, pivot = headPivot)
                }
                withTransform({
                    faceT()
                    scale(headScale, headScale, pivot = headPivot)
                }) {
                    if (headAlpha > 0f) drawPath(disc, FACE_ID_NAVY.copy(alpha = headAlpha))
                }
                val sp = splitT.value
                if (sp >= 1f) {
                    val turn = ARC_TURN_DIRECTION * turnT.value * track.perimeter / 2f
                    track.leftOutlineInto(turn, arcPoints)
                    faceIdFillPath(leftPath, arcPoints)
                    drawPath(leftPath, Color.White)
                    track.rightOutlineInto(turn, arcPoints)
                    faceIdFillPath(rightPath, arcPoints)
                    drawPath(rightPath, Color.White)
                } else if (sp > 0f) {
                    faceIdBlend(leftFromAtFace, split.leftTo, sp, leftPoints)
                    faceIdFillPath(leftPath, leftPoints)
                    drawPath(leftPath, Color.White)
                    faceIdBlend(rightFromAtFace, split.rightTo, sp, rightPoints)
                    faceIdFillPath(rightPath, rightPoints)
                    drawPath(rightPath, Color.White)
                } else if (fpi > 0f) {
                    withTransform({ faceT() }) {
                        if (fpi < 1f) {
                            val glyphAlpha = Color.White.copy(alpha = 1f - fpi)
                            if (crossT.value >= 1f) {
                                drawPath(crossPath, glyphAlpha)
                            } else if (crossT.value > 0f) {
                                faceIdBlend(crossOutlines.first, crossOutlines.second, crossT.value, crossPoints)
                                faceIdFillPath(crossMorphPath, crossPoints)
                                drawPath(crossMorphPath, glyphAlpha)
                            } else {
                                drawPath(hole, glyphAlpha)
                            }
                        }
                        val tp = fpTick.value
                        val toTick = faceSmooth(0f, FP_TICK_FADE, tp)
                        drawFingerprint(alpha = fpi * (1f - toTick), scan = fpScan.value)
                        if (tp > 0f) {
                            faceIdBlend(fpTickOutlines.first, fpTickOutlines.second, tp, fpTickPoints)
                            faceIdFillPath(fpTickPath, fpTickPoints)
                            drawPath(fpTickPath, Color.White.copy(alpha = toTick))
                        }
                    }
                } else if (crossT.value >= 1f) {
                    withTransform({ faceT() }) { drawPath(crossPath, Color.White) }
                } else if (crossT.value > 0f) {
                    faceIdBlend(crossOutlines.first, crossOutlines.second, crossT.value, crossPoints)
                    faceIdFillPath(crossMorphPath, crossPoints)
                    withTransform({ faceT() }) { drawPath(crossMorphPath, Color.White) }
                } else if (m <= 0f) {
                    withTransform({ faceT() }) { drawPath(hole, Color.White) }
                } else if (m >= 1f) {
                    withTransform({ faceT() }) { drawPath(tickPath, Color.White) }
                } else {
                    faceIdBlend(outlines.first, outlines.second, m, morphPoints)
                    faceIdFillPath(morphPath, morphPoints)
                    withTransform({ faceT() }) { drawPath(morphPath, Color.White) }
                }
                val t = scanT.value
                val eyeAlpha = headAlpha * (1f - crossT.value)
                if (eyeAlpha > 0f && t > FACE_ID_EYES_START - 0.02f) {
                    val shut = 1.3f * blink.value
                    withTransform({
                        faceT()
                        scale(headScale, headScale, pivot = headPivot)
                    }) {
                        drawEye(FACE_ID_HEAD_CX - FACE_ID_EYE_DX, faceEaseInOut(faceSmooth(FACE_ID_EYES_START, FACE_ID_EYES_END, t)) - shut, eyeAlpha)
                        drawEye(FACE_ID_HEAD_CX + FACE_ID_EYE_DX, faceEaseInOut(faceSmooth(FACE_ID_EYES_START + 0.05f, FACE_ID_EYES_END + 0.05f, t)) - shut, eyeAlpha)
                    }
                }
            }
        }
        }
    }
}
