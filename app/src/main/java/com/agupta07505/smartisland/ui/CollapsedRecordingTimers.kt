/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import android.os.SystemClock
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AvTimer
import androidx.compose.material.icons.rounded.HourglassBottom
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.TimerRings
import com.agupta07505.smartisland.util.OemDeviceRules
import com.agupta07505.smartisland.util.TimerClock
import com.agupta07505.smartisland.util.TimerStopwatchParser
import kotlinx.coroutines.delay
import com.agupta07505.smartisland.ui.IslandText as Text

@Composable
internal fun ScreenRecordingCollapsedGlyph(settings: SmartIslandSettings = SmartIslandSettings.Default,
        paused: Boolean = false) {
    val infiniteTransition = rememberInfiniteTransition(label = "collapsedRecordingPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val recordingColor = Color(settings.screenRecordingColor)

    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(recordingColor.copy(alpha = 0.25f)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .graphicsLayer { alpha = if (paused) 0.5f else pulseAlpha }
                .clip(CircleShape)
                .background(recordingColor)
        )
    }
}

@Composable
internal fun AudioRecordingCollapsedGlyph(settings: SmartIslandSettings = SmartIslandSettings.Default,
        paused: Boolean = false) {
    val infiniteTransition = rememberInfiniteTransition(label = "collapsedAudioPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "audioPulseAlpha"
    )
    val color = Color(settings.screenRecordingColor)
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.25f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Mic,
            contentDescription = null,
            tint = color,
            modifier = Modifier
                .size(13.dp)
                .graphicsLayer { alpha = if (paused) 0.5f else pulseAlpha }
        )
    }
}

@Composable
internal fun TimerCollapsedGlyph(notification: IslandNotification?,
        settings: SmartIslandSettings = SmartIslandSettings.Default) {
    val timerColor = Color(settings.timerColor)
    val isPaused = remember(notification?.key, notification?.actionIntents, notification?.text, notification?.title) {
        val resumeKeywords =
            listOf("resume", "start", "play", "continue", "unpause", "reanudar", "reprendre", "weiter", "riprendi", "continuar", "शुरू", "继续", "再開", "возобновить")
        val pausedKeywords =
            listOf("paused", "pause", "en pause", "pausado", "pausada", "angehalten", "sospeso", "sospesa", "रोक दिया गया", "已暂停", "一時停止中", "приостановлено")
        val actions = notification?.actionIntents.orEmpty()
        actions.any { act ->
            val t = act.title.lowercase()
            resumeKeywords.any { t.contains(it) }
        } || pausedKeywords.any {
            notification?.text?.contains(it, ignoreCase = true) == true ||
            notification?.title?.contains(it, ignoreCase = true) == true
        }
    }
    val timerFraction = rememberTimerFraction(notification, isPaused)
    val infiniteTransition = rememberInfiniteTransition(label = "timerPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "timerPulseScale"
    )

    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(timerColor.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center
    ) {
        if (timerFraction != null) {
            SmoothCircularRingProgress(
                progress = timerFraction,
                color = timerColor,
                strokeWidthDp = GLYPH_RING_STROKE_DP,
                trackColor = timerColor.copy(alpha = 0.25f),
                modifier = Modifier.matchParentSize()
            )
        }
        Icon(
            imageVector = Icons.Rounded.HourglassBottom,
            contentDescription = "Timer",
            tint = timerColor,
            modifier = Modifier
                .size(13.dp)
                .graphicsLayer {
                    if (!isPaused) {
                        scaleX = pulseScale
                        scaleY = pulseScale
                    }
                }
        )
    }
}

@Composable
internal fun StopwatchCollapsedGlyph(notification: IslandNotification?,
        settings: SmartIslandSettings = SmartIslandSettings.Default) {
    val stopwatchColor = Color(settings.stopwatchColor)
    val seconds = rememberStopwatchSeconds(notification)

    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(stopwatchColor.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center
    ) {
        SmoothCircularRingProgress(
            progress = (seconds % 60L) / 60f,
            color = stopwatchColor,
            strokeWidthDp = GLYPH_RING_STROKE_DP,
            trackColor = stopwatchColor.copy(alpha = 0.25f),
            modifier = Modifier.matchParentSize()
        )
        Icon(
            imageVector = Icons.Rounded.AvTimer,
            contentDescription = "Stopwatch",
            tint = stopwatchColor,
            modifier = Modifier.size(12.dp)
        )
    }
}

@Composable
internal fun TimerCountdown(notification: IslandNotification?, color: Color) {
    val isMiuiRom = OemDeviceRules.isMiuiOrHyperOs()
    val isPaused = remember(notification?.key, notification?.actionIntents, notification?.text, notification?.title) {
        val resumeKeywords =
            listOf("resume", "start", "play", "continue", "unpause", "reanudar", "reprendre", "weiter", "riprendi", "continuar", "शुरू", "继续", "再開", "возобновить")
        val pausedKeywords =
            listOf("paused", "pause", "en pause", "pausado", "pausada", "angehalten", "sospeso", "sospesa", "रोक दिया गया", "已暂停", "一時停止中", "приостановлено")
        val actions = notification?.actionIntents.orEmpty()
        actions.any { act ->
            val t = act.title.lowercase()
            resumeKeywords.any { t.contains(it) }
        } || pausedKeywords.any {
            notification?.text?.contains(it, ignoreCase = true) == true ||
            notification?.title?.contains(it, ignoreCase = true) == true
        }
    }

    val targetTime = notification?.timeMillis ?: remember { System.currentTimeMillis() + 300000L }
    // Exact cards truncate to whole seconds like the shade
    val exact = !isMiuiRom && notification?.exactClock == true

    var remainingSec by remember(notification?.key) {
        val parsed = notification?.let { TimerStopwatchParser.parseTimerRemainingSeconds(it) }
        val rem = if (isMiuiRom) {
            notification?.timerRemainingSeconds
                ?: if (parsed != null && parsed > 0L) {
                    parsed
                } else if (targetTime > System.currentTimeMillis()) {
                    ((targetTime - System.currentTimeMillis() + 500L) / 1000L).coerceAtLeast(0L)
                } else {
                    0L
                }
        } else {
            if (exact && !isPaused && targetTime > System.currentTimeMillis()) {
                TimerClock.secondsLeft(targetTime - System.currentTimeMillis())
            } else if (parsed != null && parsed > 0) {
                parsed
            } else if (targetTime > System.currentTimeMillis()) {
                ((targetTime - System.currentTimeMillis() + 500L) / 1000L).coerceAtLeast(0L)
            } else {
                0L
            }
        }
        mutableLongStateOf(rem)
    }

    // The paused remaining time the listener froze
    val frozenSec = if (isPaused && notification?.recordingPaused == true) {
        notification.recordingFrozenMs?.let {
            TimerClock.displaySeconds(it, exact, 500L)
        }
    } else {
        null
    }

    LaunchedEffect(notification?.text, notification?.title, notification?.timerRemainingSeconds) {
        if (isPaused && notification != null) {
            if (isMiuiRom) {
                val pausedRemaining = notification.timerRemainingSeconds
                    ?: TimerStopwatchParser.parseTimerRemainingSeconds(notification)

                if (pausedRemaining != null && pausedRemaining >= 0L) {
                    remainingSec = pausedRemaining
                }
            } else {
                if (frozenSec == null) {
                    val parsed = TimerStopwatchParser.parseTimerRemainingSeconds(notification)
                    if (parsed != null && parsed > 0) {
                        remainingSec = parsed
                    }
                }
            }
        }
    }

    LaunchedEffect(notification?.key, targetTime, isPaused, notification?.timerRemainingSeconds) {
        if (isMiuiRom) {
            if (isPaused) {
                notification?.timerRemainingSeconds?.let {
                    remainingSec = it
                }
            } else {
                while (true) {
                    val now = System.currentTimeMillis()
                    val rem = if (targetTime > now) {
                        ((targetTime - now + 999L) / 1000L).coerceAtLeast(0L)
                    } else {
                        0L
                    }
                    remainingSec = rem
                    if (rem <= 0L) break
                    delay(100L)
                }
            }
        } else {
            if (!isPaused) {
                while (true) {
                    val now = System.currentTimeMillis()
                    val remMs = (targetTime - now).coerceAtLeast(0L)
                    val rem = TimerClock.displaySeconds(remMs, exact, 500L)
                    remainingSec = rem
                    if (exact) {
                        if (remMs <= 0L) break
                        delay(TimerClock.msToNextTick(remMs))
                    } else {
                        if (rem <= 0L) break
                        delay(500L)
                    }
                }
            }
        }
    }

    val shownSec = if (isMiuiRom) remainingSec else (frozenSec ?: remainingSec)
    val text = remember(shownSec) {
        TimerStopwatchParser.formatTime(shownSec)
    }

    FitSlotText(text = text, color = color, fontWeight = FontWeight.Bold, compact = compactClock(text), roll = true)
}

@Composable
internal fun StopwatchTimer(notification: IslandNotification?, color: Color) {
    if (OemDeviceRules.isMiuiOrHyperOs()) {
        val startTime = notification?.timeMillis
            ?: remember { System.currentTimeMillis() }

        val frozenElapsedSeconds = notification?.stopwatchElapsedSeconds
        val chronometerBase = notification?.stopwatchChronometerBase

        val isPaused = remember(
            notification?.key,
            notification?.actionIntents,
            notification?.title,
            notification?.text,
            frozenElapsedSeconds
        ) {
            frozenElapsedSeconds != null ||
                notification?.actionIntents?.any {
                    it.title.contains("resume", ignoreCase = true) ||
                        it.title.contains("start", ignoreCase = true)
                } == true ||
                notification?.title?.contains("paused", ignoreCase = true) == true ||
                notification?.text?.contains("paused", ignoreCase = true) == true
        }

        var elapsedSeconds by remember(
            notification?.key,
            startTime,
            frozenElapsedSeconds,
            chronometerBase
        ) {
            mutableLongStateOf(
                frozenElapsedSeconds
                    ?: chronometerBase?.let { base ->
                        (
                            (SystemClock.elapsedRealtime() - base) / 1000L
                        ).coerceAtLeast(0L)
                    }
                    ?: ((System.currentTimeMillis() - startTime) / 1000L)
                        .coerceAtLeast(0L)
            )
        }

        LaunchedEffect(
            notification?.key,
            startTime,
            isPaused,
            frozenElapsedSeconds,
            chronometerBase
        ) {
            if (frozenElapsedSeconds != null) {
                elapsedSeconds = frozenElapsedSeconds
            } else if (!isPaused) {
                while (true) {
                    val clockElapsed = chronometerBase?.let { base ->
                        (
                            (SystemClock.elapsedRealtime() - base) / 1000L
                        ).coerceAtLeast(0L)
                    }

                    elapsedSeconds = clockElapsed
                        ?: ((System.currentTimeMillis() - startTime) / 1000L)
                            .coerceAtLeast(0L)

                    delay(100L)
                }
            }
        }

        val text = remember(elapsedSeconds) {
            TimerStopwatchParser.formatTime(elapsedSeconds)
        }

        FitSlotText(text = text, color = color, fontWeight = FontWeight.Bold, compact = compactClock(text), roll = true)
    } else {
        // Same clock as the expanded card
        val seconds = rememberRecordingSeconds(notification)
        FitSlotText(
            text = TimerStopwatchParser.formatTime(seconds),
            color = if (notification?.recordingFrozenMs != null) color.copy(alpha = 0.6f) else color,
            fontWeight = FontWeight.Bold,
            compact = compactClock(TimerStopwatchParser.formatTime(seconds)),
            roll = true
        )
    }
}

@Composable
private fun rememberStopwatchSeconds(notification: IslandNotification?): Long {
    if (!OemDeviceRules.isMiuiOrHyperOs()) return rememberRecordingSeconds(notification)
    val frozen = notification?.stopwatchElapsedSeconds
    val base = notification?.stopwatchChronometerBase
    val start = notification?.timeMillis ?: remember { System.currentTimeMillis() }
    fun runningSeconds(): Long = base?.let { ((SystemClock.elapsedRealtime() - it) / 1000L).coerceAtLeast(0L) }
        ?: ((System.currentTimeMillis() - start) / 1000L).coerceAtLeast(0L)
    var elapsed by remember(notification?.key, frozen, base) { mutableLongStateOf(frozen ?: runningSeconds()) }
    LaunchedEffect(notification?.key, frozen, base, start) {
        if (frozen != null) {
            elapsed = frozen
        } else {
            while (true) {
                elapsed = runningSeconds()
                delay(500L)
            }
        }
    }
    return elapsed
}

@Composable
private fun rememberTimerFraction(notification: IslandNotification?, paused: Boolean): Float? {
    var fraction by remember(notification?.key) { mutableStateOf(notification?.let { TimerRings.fraction(it, paused) }) }
    LaunchedEffect(notification?.key, notification?.timeMillis, paused, notification?.recordingFrozenMs,
        notification?.timerRemainingSeconds) {
        while (true) {
            notification?.let { card -> TimerRings.fraction(card, paused)?.let { fraction = it } }
            if (paused) break
            delay(500L)
        }
    }
    return fraction
}

@Composable
internal fun FitSlotText(text: String, color: Color, fontWeight: FontWeight, compact: String? = null,
        roll: Boolean = false) {
    val measurer = rememberTextMeasurer()
    val base = LocalTextStyle.current
    BoxWithConstraints {
        val maxPx = constraints.maxWidth
        val fit = remember(text, compact, maxPx, fontWeight, base, roll) {
            fun fits(s: String, sp: Float): Boolean = measurer.measure(
                text = s,
                style = base.copy(fontSize = sp.sp, fontWeight = fontWeight,
                    fontFeatureSettings = if (roll) "tnum" else null),
                softWrap = false,
                maxLines = 1
            ).size.width <= maxPx
            var sp = 11f
            while (sp > 8f && !fits(text, sp)) sp -= 0.5f
            if (compact == null || fits(text, sp)) {
                FitResult(sp, false)
            } else {
                var csp = 11f
                while (csp > 8f && !fits(compact, csp)) csp -= 0.5f
                FitResult(csp, true)
            }
        }
        val sizeSp by animateFloatAsState(
            targetValue = fit.sizeSp,
            animationSpec = tween(120),
            label = "slotFitSize"
        )
        Crossfade(
            targetState = fit.compact,
            animationSpec = tween(140),
            label = "slotFitSwap"
        ) { showCompact ->
            val shown = if (showCompact) compact ?: text else text
            if (roll) {
                RollingText(text = shown, color = color, sizeSp = sizeSp, fontWeight = fontWeight)
            } else {
                Text(
                    text = shown,
                    color = color,
                    fontSize = sizeSp.sp,
                    fontWeight = fontWeight,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

private data class FitResult(val sizeSp: Float, val compact: Boolean)

@Composable
private fun RollingText(text: String, color: Color, sizeSp: Float, fontWeight: FontWeight) {
    val style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
    Row {
        text.forEachIndexed { index, ch ->
            key(text.length - index) {
                if (ch.isDigit()) {
                    AnimatedContent(
                        targetState = ch,
                        transitionSpec = {
                            (slideInVertically(tween(220)) { it } + fadeIn(tween(160)))
                                .togetherWith(
                                    slideOutVertically(tween(220)) { -it } + fadeOut(tween(120))
                                )
                        },
                        label = "digitRoll"
                    ) { digit ->
                        Text(
                            text = digit.toString(),
                            color = color,
                            fontSize = sizeSp.sp,
                            fontWeight = fontWeight,
                            maxLines = 1,
                            softWrap = false,
                            style = style
                        )
                    }
                } else {
                    Text(
                        text = ch.toString(),
                        color = color,
                        fontSize = sizeSp.sp,
                        fontWeight = fontWeight,
                        maxLines = 1,
                        softWrap = false,
                        style = style
                    )
                }
            }
        }
    }
}

@Composable
internal fun RecordingTimerFit(notification: IslandNotification?, color: Color) {
    val seconds = rememberRecordingSeconds(notification)
    val text = formatRecordingTime(seconds)
    FitSlotText(
        text = text,
        color = if (notification?.recordingFrozenMs != null) color.copy(alpha = 0.6f) else color,
        fontWeight = FontWeight.SemiBold,
        compact = compactClock(text),
        roll = true
    )
}
