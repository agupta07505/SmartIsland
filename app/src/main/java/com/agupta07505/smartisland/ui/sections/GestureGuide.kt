/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.sections

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Gesture
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Swipe
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.ui.res.stringResource
import com.agupta07505.smartisland.ui.glass.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.SwipeAction

// What each gesture does right now.

private data class GestureLine(val icon: ImageVector, val gesture: String, val result: String)

private data class GestureGroup(val title: String, val note: String?, val lines: List<GestureLine>)

@Composable
private fun actionText(id: String, default: SwipeAction, enabled: Boolean): String =
    if (!enabled) stringResource(R.string.gesture_001) else getSwipeActionTitle(SwipeAction.fromId(id, default))

@Composable
private fun gestureGroups(settings: SmartIslandSettings): List<GestureGroup> {
    val pill = settings.enablePillSwipeActions
    val card = settings.enableSwipeActions
    val longPress = SwipeAction.fromId(settings.longPressAction, SwipeAction.TogglePill)
    val longPressTitle = getSwipeActionTitle(longPress)
    val longPressText = if (settings.enableAppShortcuts) {
        stringResource(R.string.gesture_002, longPressTitle)
    } else {
        stringResource(R.string.gesture_x02, longPressTitle)
    }
    return listOf(
        GestureGroup(
            title = stringResource(R.string.gesture_004),
            note = stringResource(R.string.gesture_005),
            lines = listOf(
                GestureLine(Icons.Rounded.TouchApp, stringResource(R.string.gesture_006)),
                GestureLine(
                    Icons.Rounded.TouchApp, stringResource(R.string.gesture_008),
                    stringResource(R.string.gesture_009)
                ),
                GestureLine(Icons.Rounded.Swipe, stringResource(R.string.gesture_010), actionText(settings.pillSwipeLeftAction, SwipeAction.PreviousNotification, pill)),
                GestureLine(Icons.Rounded.Swipe, stringResource(R.string.gesture_011), actionText(settings.pillSwipeRightAction, SwipeAction.NextNotification, pill)),
                GestureLine(Icons.Rounded.ArrowUpward, stringResource(R.string.gesture_075)),
                GestureLine(Icons.Rounded.Swipe, stringResource(R.string.gesture_077)),
                GestureLine(Icons.Rounded.TouchApp, stringResource(R.string.gesture_097)),
                GestureLine(Icons.Rounded.TouchApp, stringResource(R.string.gesture_099)),
                GestureLine(Icons.Rounded.TouchApp, stringResource(R.string.gesture_101)),
            )
        ),
        GestureGroup(
            title = stringResource(R.string.gesture_103),
            note = null,
            lines = listOf(
                GestureLine(Icons.Rounded.TouchApp, stringResource(R.string.gesture_104)),
                GestureLine(Icons.Rounded.ArrowUpward, stringResource(R.string.gesture_106)),
            )
        ),
    )
}

/** The full list of gestures, grouped by where they happen. Shows the actions as they are set right now. */
@Composable
fun GestureReferenceCard(settings: SmartIslandSettings) {
    val groups = gestureGroups(settings)
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconBadge(Icons.Rounded.Gesture, size = 36)
                Column {
                    Text(stringResource(R.string.gesture_108), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        stringResource(R.string.gesture_109),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            groups.forEachIndexed { index, group ->
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Text(group.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                group.note?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                group.lines.forEach { line ->
                    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        IconBadge(line.icon, size = 30)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(line.gesture, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(line.result, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 17.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IconBadge(icon: ImageVector, size: Int) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size((size * 0.55f).dp))
    }
}

// The tour for new users: a few steps, one idea each, in the order a person meets them.

private data class TourStep(val icon: ImageVector, val title: String, val points: List<String>, val tip: String)

@Composable
private fun tourSteps(settings: SmartIslandSettings): List<TourStep> {
    val left = getSwipeActionTitle(SwipeAction.fromId(settings.pillSwipeLeftAction, SwipeAction.PreviousNotification))
    val right = getSwipeActionTitle(SwipeAction.fromId(settings.pillSwipeRightAction, SwipeAction.NextNotification))
    val up = getSwipeActionTitle(SwipeAction.fromId(settings.pillSwipeUpAction, SwipeAction.DismissCurrent))
    val cardUp = getSwipeActionTitle(SwipeAction.fromId(settings.swipeUpAction, SwipeAction.DismissCurrent))
    val cardHold = getSwipeActionTitle(SwipeAction.fromId(settings.swipeHoldUpAction, SwipeAction.DismissAll))
    return listOf(
        TourStep(
            Icons.Rounded.Gesture, stringResource(R.string.gesture_110),
            listOf(
                stringResource(R.string.gesture_x07),
                stringResource(R.string.gesture_112),
                stringResource(R.string.gesture_113)
            ),
            stringResource(R.string.gesture_114)
        ),
        TourStep(
            Icons.Rounded.TouchApp, stringResource(R.string.gesture_115),
            listOf(
                stringResource(R.string.gesture_116),
                stringResource(R.string.gesture_117),
                stringResource(R.string.gesture_118),
                stringResource(R.string.gesture_119),
                stringResource(R.string.gesture_120)
            ),
            stringResource(R.string.gesture_121)
        ),
        TourStep(
            Icons.Rounded.Swipe, stringResource(R.string.gesture_122),
            listOf(
                stringResource(R.string.gesture_123, left),
                stringResource(R.string.gesture_124, right),
                stringResource(R.string.gesture_125, up),
                stringResource(R.string.gesture_126)
            ),
            stringResource(R.string.gesture_127)
        ),
        TourStep(
            Icons.Rounded.TouchApp, stringResource(R.string.gesture_015),
            listOf(
                if (settings.enableAppShortcuts) {
                    stringResource(R.string.gesture_129)
                } else {
                    stringResource(R.string.gesture_130)
                },
                stringResource(R.string.gesture_128),
                stringResource(R.string.gesture_132)
            ),
            stringResource(R.string.gesture_133)
        ),
        TourStep(
            Icons.Rounded.ArrowUpward, stringResource(R.string.gesture_134),
            listOf(
                stringResource(R.string.gesture_135, cardUp),
                stringResource(R.string.gesture_x08, cardHold),
                stringResource(R.string.gesture_137),
                stringResource(R.string.gesture_138)
            ),
            stringResource(R.string.gesture_139)
        ),
        TourStep(
            Icons.Rounded.Apps, stringResource(R.string.gesture_140),
            listOf(
                stringResource(R.string.gesture_141),
                stringResource(R.string.gesture_142),
                stringResource(R.string.gesture_143),
                stringResource(R.string.gesture_144)
            ),
            stringResource(R.string.gesture_145)
        ),
        TourStep(
            Icons.Rounded.Tune, stringResource(R.string.gesture_146),
            listOf(
                stringResource(R.string.gesture_147),
                stringResource(R.string.gesture_148),
                stringResource(R.string.gesture_149)
            ),
            stringResource(R.string.gesture_150)
        ),
    )
}

/**
 * The new-user tour: a dialog of steps with Back / Next. Reached from the welcome dialog and from the top of the
 * Gestures screen. [onFinish] is called by Done, by Skip and by tapping outside.
 */
@Composable
fun NewUserTour(settings: SmartIslandSettings, onFinish: () -> Unit) {
    val steps = tourSteps(settings)
    var index by rememberSaveable { mutableIntStateOf(0) }
    val step = steps[index.coerceIn(0, steps.lastIndex)]
    val last = index >= steps.lastIndex
    Dialog(onDismissRequest = onFinish, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(12.dp),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(modifier = Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.gesture_x09, index + 1, steps.size),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    if (!last) TextButton(onClick = onFinish) { Text("Skip") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    steps.indices.forEach { i ->
                        Box(
                            Modifier
                                .height(4.dp)
                                .weight(1f)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    if (i <= index) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconBadge(step.icon, size = 48)
                    Text(step.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    step.points.forEach { point ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                            Box(
                                Modifier
                                    .padding(top = 7.dp)
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                            Text(point, style = MaterialTheme.typography.bodyMedium, lineHeight = 20.sp, modifier = Modifier.weight(1f))
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                            Icon(Icons.Rounded.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Text(step.tip, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 17.sp)
                        }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (index > 0) {
                        TextButton(onClick = { index -= 1 }) { Text("Back") }
                    }
                    Spacer(Modifier.weight(1f))
                    Button(onClick = { if (last) onFinish() else index += 1 }, shape = RoundedCornerShape(14.dp)) {
                        Text(if (last) stringResource(R.string.gesture_x10), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/** The card at the top of the Gestures screen that opens the tour. */
@Composable
fun NewUserTourCard(onStart: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onStart),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconBadge(Icons.Rounded.Lightbulb, size = 40)
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.gesture_152), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    stringResource(R.string.gesture_153),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text("Start", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
    }
}
