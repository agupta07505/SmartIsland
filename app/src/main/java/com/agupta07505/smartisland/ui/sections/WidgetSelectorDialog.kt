/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.model.IdleSchedule
import com.agupta07505.smartisland.model.IdleWidgetKind
import com.agupta07505.smartisland.model.IdleWidgetOff
import com.agupta07505.smartisland.model.IdleWidgets
import kotlin.math.abs

@Composable
internal fun WidgetSelectorDialog(
    storedIds: String,
    pinnedFirst: String,
    onChange: (String) -> Unit,
    /** Saves the order together with the pinned widget (empty = none). */
    onOrder: (ids: String, pinned: String) -> Unit,
    onDismiss: () -> Unit,
    onOpenWeather: () -> Unit = {},
    onOpenNetSpeed: () -> Unit = {}
) {
    val selected = remember(storedIds) { IdleWidgets.parse(storedIds) }
    val pinned = remember(selected, pinnedFirst) { IdleWidgetKind.fromId(pinnedFirst)?.takeIf { it in selected } }
    val others = IdleWidgetKind.values().filter { it !in selected }
    // The dragged row follows the finger on the rows as they rest
    val draggedState = remember { mutableStateOf<IdleWidgetKind?>(null) }
    val dragOffsetYState = remember { mutableFloatStateOf(0f) }
    val dragTargetState = remember { mutableStateOf<IdleWidgetKind?>(null) }
    val bounds = remember { HashMap<String, Rect>() }

    fun dropFor(kind: IdleWidgetKind, offsetY: Float): IdleWidgetKind? {
        val from = bounds[kind.id] ?: return null
        val centerY = from.center.y + offsetY
        var best: IdleWidgetKind? = null
        var bestDistance = 0f
        for (other in selected) {
            if (other == kind || other == pinned) continue
            val rect = bounds[other.id] ?: continue
            val crossed = if (rect.center.y > from.center.y) centerY >= rect.center.y else centerY <= rect.center.y
            val distance = abs(rect.center.y - from.center.y)
            if (crossed && distance > bestDistance) {
                best = other
                bestDistance = distance
            }
        }
        return best
    }

    fun toggle(kind: IdleWidgetKind) {
        val next = IdleWidgets.toggle(storedIds, kind)
        if (kind == pinned && next != storedIds) onOrder(next, "") else onChange(next)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.padding(horizontal = 12.dp),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        icon = {
            Icon(
                imageVector = Icons.Rounded.Widgets,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        },
        title = { WidgetSelectorTitle(selected = selected) },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                WidgetSelectorQuickButtons(onChange = onChange, onOrder = onOrder)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    selected.forEach { kind ->
                        key(kind.id) {
                            SelectedKindRow(
                                kind = kind,
                                selected = selected,
                                pinned = pinned,
                                storedIds = storedIds,
                                draggedState = draggedState,
                                dragOffsetYState = dragOffsetYState,
                                dragTargetState = dragTargetState,
                                bounds = bounds,
                                dropFor = { k, y -> dropFor(k, y) },
                                toggle = { toggle(it) },
                                onOrder = onOrder,
                                onOpenWeather = onOpenWeather,
                                onOpenNetSpeed = onOpenNetSpeed
                            )
                        }
                    }
                    others.forEach { kind ->
                        key(kind.id) {
                            OtherKindRow(kind = kind, toggle = { toggle(it) })
                        }
                    }
                }
                Text(
                    text = stringResource(R.string.idle_select_order_note),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.idle_select_last_note),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.idle_select_done),
                fontWeight = FontWeight.Bold) }
        }
    )
}

@Composable
private fun WidgetSelectorTitle(selected: List<IdleWidgetKind>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.idle_select_dialog_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        CountBadge("${selected.size} / ${IdleWidgetKind.values().size}")
    }
}

@Composable
private fun WidgetSelectorQuickButtons(
    onChange: (String) -> Unit,
    onOrder: (ids: String, pinned: String) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = { onChange(IdleWidgetOff.allIds()) }) {
            Text(stringResource(R.string.idle_select_all), fontWeight = FontWeight.SemiBold)
        }
        TextButton(onClick = { onOrder(IdleWidgets.DEFAULT_IDS, "") }) {
            Text(stringResource(R.string.idle_select_reset), fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SelectedKindRow(
    kind: IdleWidgetKind,
    selected: List<IdleWidgetKind>,
    pinned: IdleWidgetKind?,
    storedIds: String,
    draggedState: MutableState<IdleWidgetKind?>,
    dragOffsetYState: MutableFloatState,
    dragTargetState: MutableState<IdleWidgetKind?>,
    bounds: HashMap<String, Rect>,
    dropFor: (IdleWidgetKind, Float) -> IdleWidgetKind?,
    toggle: (IdleWidgetKind) -> Unit,
    onOrder: (ids: String, pinned: String) -> Unit,
    onOpenWeather: () -> Unit,
    onOpenNetSpeed: () -> Unit
) {
    val dragged by draggedState
    val dragOffsetY by dragOffsetYState
    val dragTarget by dragTargetState
    val isDragged = dragged == kind
    val isPinned = pinned == kind
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { bounds[kind.id] = it.boundsInParent() }
            .zIndex(if (isDragged) 1f else 0f)
            .graphicsLayer { translationY = if (isDragged) dragOffsetY else 0f }
            .clip(RoundedCornerShape(10.dp))
            .background(
                when {
                    isDragged -> MaterialTheme.colorScheme.surfaceVariant
                    dragTarget == kind -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    else -> Color.Transparent
                }
            )
            .clickable(enabled = dragged == null) { toggle(kind) }
            .padding(vertical = 4.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        KindDragHandle(
            kind = kind,
            selected = selected,
            pinned = pinned,
            storedIds = storedIds,
            isPinned = isPinned,
            draggedState = draggedState,
            dragOffsetYState = dragOffsetYState,
            dragTargetState = dragTargetState,
            dropFor = dropFor,
            onOrder = onOrder
        )
        Spacer(Modifier.width(6.dp))
        KindIcon(kind, checked = true)
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(kindTitle(kind)),
            modifier = Modifier.weight(1f),
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2
        )
        // weather's and Network speed's settings tabs
        if (kind == IdleWidgetKind.Weather) {
            IconButton(onClick = onOpenWeather, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = stringResource(R.string.weather_cfg_open),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        if (kind == IdleWidgetKind.NetSpeed) {
            IconButton(onClick = onOpenNetSpeed, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = stringResource(R.string.net_cfg_open),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        // pin: this widget always comes first
        PinToggleButton(kind = kind, isPinned = isPinned, pinned = pinned, storedIds = storedIds, onOrder = onOrder)
        // The last widget stays ticked: the master switch is what turns the widgets off
        Checkbox(
            checked = true,
            enabled = selected.size > 1 && dragged == null,
            onCheckedChange = { toggle(kind) }
        )
    }
}

@Composable
private fun KindDragHandle(
    kind: IdleWidgetKind,
    selected: List<IdleWidgetKind>,
    pinned: IdleWidgetKind?,
    storedIds: String,
    isPinned: Boolean,
    draggedState: MutableState<IdleWidgetKind?>,
    dragOffsetYState: MutableFloatState,
    dragTargetState: MutableState<IdleWidgetKind?>,
    dropFor: (IdleWidgetKind, Float) -> IdleWidgetKind?,
    onOrder: (ids: String, pinned: String) -> Unit
) {
    var dragged by draggedState
    var dragOffsetY by dragOffsetYState
    var dragTarget by dragTargetState
    if (isPinned || selected.size < 2) {
        Spacer(Modifier.width(24.dp))
    } else {
        Icon(
            imageVector = Icons.Rounded.DragHandle,
            contentDescription = stringResource(R.string.idle_select_drag, stringResource(kindTitle(kind))),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(24.dp)
                .pointerInput(kind, storedIds, pinned) {
                    detectDragGestures(
                        onDragStart = {
                            dragged = kind
                            dragOffsetY = 0f
                            dragTarget = null
                        },
                        onDrag = { change, amount ->
                            change.consume()
                            dragOffsetY += amount.y
                            dragTarget = dropFor(kind, dragOffsetY)
                        },
                        onDragEnd = {
                            val target = dropFor(kind, dragOffsetY)
                            dragged = null
                            dragOffsetY = 0f
                            dragTarget = null
                            if (target != null) {
                                val moved = IdleSchedule.move(storedIds, kind, target)
                                if (moved != storedIds) onOrder(moved, pinned?.id ?: "")
                            }
                        },
                        onDragCancel = {
                            dragged = null
                            dragOffsetY = 0f
                            dragTarget = null
                        }
                    )
                }
        )
    }
}

@Composable
private fun PinToggleButton(
    kind: IdleWidgetKind,
    isPinned: Boolean,
    pinned: IdleWidgetKind?,
    storedIds: String,
    onOrder: (ids: String, pinned: String) -> Unit
) {
    IconButton(
        onClick = {
            val next = IdleSchedule.togglePinned(pinned?.id ?: "", kind)
            onOrder(if (next.isEmpty()) storedIds else IdleSchedule.moveToFront(storedIds, kind), next)
        },
        modifier = Modifier.size(36.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.PushPin,
            contentDescription = stringResource(
                if (isPinned) R.string.idle_select_unpin else R.string.idle_select_pin,
                stringResource(kindTitle(kind))
            ),
            tint = if (isPinned) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun OtherKindRow(kind: IdleWidgetKind, toggle: (IdleWidgetKind) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { toggle(kind) }
            .padding(vertical = 6.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        KindIcon(kind, checked = false)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(kindTitle(kind)),
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(kindDesc(kind)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Checkbox(checked = false, onCheckedChange = { toggle(kind) })
    }
}

@Composable
internal fun KindIcon(kind: IdleWidgetKind, checked: Boolean) {
    val tint = kindTint(kind)
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (checked) tint.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = kindIcon(kind),
            contentDescription = null,
            tint = if (checked) tint else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
internal fun CountBadge(text: String) {
    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
