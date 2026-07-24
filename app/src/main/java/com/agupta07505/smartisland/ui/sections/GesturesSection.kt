/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.sections

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun GesturesSection() {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Swipe Up", "Swipe Down", "Swipe Left/Right")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Tab Row for selecting gestures
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth()
        ) {
            tabs.forEachIndexed { index, label ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = label,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    }
                )
            }
        }

        when (selectedTab) {
            0 -> SwipeUpGuide()
            1 -> SwipeDownGuide()
            2 -> SwipeHorizontalGuide()
        }
    }
}

// ==========================================
// 1. SWIPE UP: DISMISS NOTIFICATION
// ==========================================
@Composable
private fun SwipeUpGuide() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Description Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.ArrowUpward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Dismiss active notification",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "Swipe UP on the expanded island card to dismiss the notification. It will collapse and slide out of view smoothly, clearing it from your active list.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Looping Animation Demo
        GestureDemoCard(title = "Animation Guide") {
            val infiniteTransition = rememberInfiniteTransition(label = "swipeUp")
            
            // Loop progress from 0f to 1f over 3 seconds
            val progress by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(3000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "progress"
            )

            // Calculate animated values from loop progress
            val islandHeight: Dp
            val islandAlpha: Float
            val fingerYOffset: Float
            val fingerAlpha: Float
            
            val dragStartPx = with(LocalDensity.current) { 60.dp.toPx() }
            val dragEndPx = with(LocalDensity.current) { -60.dp.toPx() }

            when {
                // Phase 1 (0.0 to 0.2): Rest on expanded state
                progress < 0.2f -> {
                    islandHeight = 80.dp
                    islandAlpha = 1f
                    fingerYOffset = dragStartPx
                    fingerAlpha = 0f
                }
                // Phase 2 (0.2 to 0.3): Finger fades in
                progress < 0.3f -> {
                    val p = (progress - 0.2f) / 0.1f
                    islandHeight = 80.dp
                    islandAlpha = 1f
                    fingerYOffset = dragStartPx
                    fingerAlpha = p
                }
                // Phase 3 (0.3 to 0.6): Finger drags upward, island compresses slightly
                progress < 0.6f -> {
                    val p = (progress - 0.3f) / 0.3f
                    // Drag finger up from start to end
                    fingerYOffset = dragStartPx - ((dragStartPx - dragEndPx) * p)
                    fingerAlpha = 1f
                    // Compress island height from 80.dp to 70.dp
                    islandHeight = (80 - (10 * p)).dp
                    islandAlpha = 1f
                }
                // Phase 4 (0.6 to 0.8): Swipe released -> Island collapses and disappears, finger fades out
                progress < 0.8f -> {
                    val p = (progress - 0.6f) / 0.2f
                    fingerYOffset = dragEndPx
                    fingerAlpha = 1f - p
                    // Island height shrinks to collapsed 34.dp
                    islandHeight = (70 - (36 * p)).dp
                    // Fade out
                    islandAlpha = 1f - p
                }
                // Phase 5 (0.8 to 1.0): Offscreen / Rest state before resetting
                else -> {
                    islandHeight = 80.dp
                    islandAlpha = 0f
                    fingerYOffset = dragStartPx
                    fingerAlpha = 0f
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(Color.Black.copy(alpha = 0.05f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.TopCenter
            ) {
                // Simulated status bar
                SimulatedStatusBar()

                // Simulated Island
                Box(
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .width(220.dp)
                        .height(islandHeight)
                        .graphicsLayer { alpha = islandAlpha }
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.Black)
                        .padding(12.dp)
                ) {
                    if (islandHeight > 45.dp) {
                        MockNotificationContent(title = "Telegram", text = "Swipe up to dismiss this message")
                    }
                }

                // Animated finger dot
                if (fingerAlpha > 0f) {
                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                translationY = fingerYOffset
                                alpha = fingerAlpha
                            }
                            .padding(top = 16.dp) // align relative to status bar
                            .size(34.dp)
                            .border(1.5.dp, Color.White, CircleShape)
                            .background(Color.White.copy(alpha = 0.45f), CircleShape)
                    )
                }
            }
        }

        // Try it yourself Card (Interactive Playground)
        GesturePlaygroundCard(title = "Try it yourself") {
            var expanded by remember { mutableStateOf(true) }
            var dragOffset by remember { mutableStateOf(0f) }
            val density = LocalDensity.current

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(Color.Black.copy(alpha = 0.05f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.TopCenter
            ) {
                SimulatedStatusBar()

                if (expanded) {
                    Box(
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .width(220.dp)
                            .height(if (dragOffset < 0f) (80.dp + (dragOffset / density.density).dp).coerceAtLeast(34.dp) else 80.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.Black)
                            .pointerInput(Unit) {
                                detectVerticalDragGestures(
                                    onDragStart = { dragOffset = 0f },
                                    onVerticalDrag = { change, dragAmount ->
                                        change.consume()
                                        dragOffset += dragAmount
                                    },
                                    onDragEnd = {
                                        val thresholdPx = with(density) { -60.dp.toPx() }
                                        if (dragOffset < thresholdPx) {
                                            // Swipe up threshold reached
                                            expanded = false
                                        }
                                        dragOffset = 0f
                                    },
                                    onDragCancel = { dragOffset = 0f }
                                )
                            }
                            .padding(12.dp)
                    ) {
                        MockNotificationContent(title = "Messenger", text = "Drag up on this card to dismiss!")
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = "Cleared",
                            tint = Color(0xFF0F9F6E),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Notification dismissed!",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F9F6E),
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = { expanded = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Reset preview", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 2. SWIPE DOWN: FLOATING WINDOWS
// ==========================================
@Composable
private fun SwipeDownGuide() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Description Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.ArrowDownward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Open app in floating window",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = "Swipe DOWN on the expanded island card to launch the notification app in a floating window (Freeform Multi-Window mode). This allows you to reply or interact without leaving your current app.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Looping Animation Demo
        GestureDemoCard(title = "Animation Guide") {
            val infiniteTransition = rememberInfiniteTransition(label = "swipeDown")
            val progress by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(3500, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "progress"
            )

            val islandHeight: Dp
            val fingerYOffset: Float
            val fingerAlpha: Float
            val windowScale: Float
            val windowAlpha: Float

            val dragStartPx = with(LocalDensity.current) { 30.dp.toPx() }
            val dragEndPx = with(LocalDensity.current) { 130.dp.toPx() }

            when {
                // Phase 1 (0.0 to 0.15): Rest on expanded state
                progress < 0.15f -> {
                    islandHeight = 80.dp
                    fingerYOffset = dragStartPx
                    fingerAlpha = 0f
                    windowScale = 0.5f
                    windowAlpha = 0f
                }
                // Phase 2 (0.15 to 0.25): Finger fades in
                progress < 0.25f -> {
                    val p = (progress - 0.15f) / 0.1f
                    islandHeight = 80.dp
                    fingerYOffset = dragStartPx
                    fingerAlpha = p
                    windowScale = 0.5f
                    windowAlpha = 0f
                }
                // Phase 3 (0.25 to 0.55): Finger drags downward
                progress < 0.55f -> {
                    val p = (progress - 0.25f) / 0.3f
                    fingerYOffset = dragStartPx + ((dragEndPx - dragStartPx) * p)
                    fingerAlpha = 1f
                    islandHeight = (80 + (16 * p)).dp
                    windowScale = 0.5f
                    windowAlpha = 0f
                }
                // Phase 4 (0.55 to 0.85): Swipe released -> Island collapses, floating window scales up and fades in
                progress < 0.85f -> {
                    val p = (progress - 0.55f) / 0.3f
                    fingerYOffset = dragEndPx
                    fingerAlpha = 1f - p
                    islandHeight = (96 - (62 * p)).dp
                    windowScale = 0.5f + (0.5f * p)
                    windowAlpha = p
                }
                // Phase 5 (0.85 to 1.0): Display floating window before reset
                else -> {
                    islandHeight = 34.dp
                    fingerYOffset = dragEndPx
                    fingerAlpha = 0f
                    windowScale = 1.0f
                    windowAlpha = 1f
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(Color.Black.copy(alpha = 0.05f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.TopCenter
            ) {
                SimulatedStatusBar()

                // Simulated Island
                Box(
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .width(if (islandHeight < 40.dp) 112.dp else 220.dp)
                        .height(islandHeight)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.Black)
                        .padding(12.dp)
                ) {
                    if (islandHeight > 45.dp) {
                        MockNotificationContent(title = "WhatsApp", text = "Swipe down to open floating chat")
                    }
                }

                // Simulated Floating Window
                if (windowAlpha > 0f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(top = 34.dp)
                            .size(width = 160.dp, height = 110.dp)
                            .graphicsLayer {
                                scaleX = windowScale
                                scaleY = windowScale
                                alpha = windowAlpha
                            }
                            .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                    ) {
                        // Title bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(22.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(6.dp).background(Color.Red, CircleShape))
                            Spacer(Modifier.width(4.dp))
                            Box(modifier = Modifier.size(6.dp).background(Color.Yellow, CircleShape))
                            Spacer(Modifier.width(4.dp))
                            Box(modifier = Modifier.size(6.dp).background(Color.Green, CircleShape))
                            Spacer(Modifier.weight(1f))
                            Text("WhatsApp", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }

                        // Content body
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 26.dp, start = 8.dp, end = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(modifier = Modifier.size(width = 110.dp, height = 8.dp).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f), RoundedCornerShape(2.dp)))
                            Box(modifier = Modifier.size(width = 80.dp, height = 8.dp).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f), RoundedCornerShape(2.dp)))
                            Spacer(Modifier.weight(1f))
                            // Reply button mockup
                            Box(
                                modifier = Modifier
                                    .align(Alignment.End)
                                    .size(width = 44.dp, height = 16.dp)
                                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Send", color = Color.White, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(4.dp))
                        }
                    }
                }

                // Animated finger dot
                if (fingerAlpha > 0f) {
                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                translationY = fingerYOffset
                                alpha = fingerAlpha
                            }
                            .padding(top = 16.dp)
                            .size(34.dp)
                            .border(1.5.dp, Color.White, CircleShape)
                            .background(Color.White.copy(alpha = 0.45f), CircleShape)
                    )
                }
            }
        }

        // Try it yourself Card (Interactive Playground)
        GesturePlaygroundCard(title = "Try it yourself") {
            var expanded by remember { mutableStateOf(true) }
            var windowOpen by remember { mutableStateOf(false) }
            var dragOffset by remember { mutableStateOf(0f) }
            val density = LocalDensity.current

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(Color.Black.copy(alpha = 0.05f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.TopCenter
            ) {
                SimulatedStatusBar()

                if (expanded) {
                    Box(
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .width(220.dp)
                            .height(if (dragOffset > 0f) (80.dp + (dragOffset / density.density).dp).coerceAtMost(100.dp) else 80.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.Black)
                            .pointerInput(Unit) {
                                detectVerticalDragGestures(
                                    onDragStart = { dragOffset = 0f },
                                    onVerticalDrag = { change, dragAmount ->
                                        change.consume()
                                        dragOffset += dragAmount
                                    },
                                    onDragEnd = {
                                        val thresholdPx = with(density) { 60.dp.toPx() }
                                        if (dragOffset > thresholdPx) {
                                            expanded = false
                                            windowOpen = true
                                        }
                                        dragOffset = 0f
                                    },
                                    onDragCancel = { dragOffset = 0f }
                                )
                            }
                            .padding(12.dp)
                    ) {
                        MockNotificationContent(title = "SMS Message", text = "Drag DOWN on this card to open in popup!")
                    }
                } else {
                    // Collapsed island
                    Box(
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .size(width = 112.dp, height = 34.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color.Black)
                    )

                    if (windowOpen) {
                        // Interactive Floating Window
                        Card(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(top = 34.dp)
                                .size(width = 180.dp, height = 120.dp),
                            shape = RoundedCornerShape(10.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(Modifier.fillMaxSize()) {
                                // Titlebar
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Messages (Floating)", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.weight(1f))
                                    IconButton(
                                        onClick = {
                                            windowOpen = false
                                            expanded = true
                                        },
                                        modifier = Modifier.size(14.dp)
                                    ) {
                                        Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.Red, modifier = Modifier.size(10.dp))
                                    }
                                }

                                // Body
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("Incoming SMS details...", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface)
                                    Text("Sender: +1 555-0199", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.weight(1f))
                                    OutlinedButton(
                                        onClick = {
                                            windowOpen = false
                                            expanded = true
                                        },
                                        modifier = Modifier.align(Alignment.CenterHorizontally).height(24.dp).padding(horizontal = 4.dp)
                                    ) {
                                        Text("Close and Reset", fontSize = 8.sp, modifier = Modifier.padding(bottom = 2.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 3. HORIZONTAL SWIPE: PAGINATION
// ==========================================
@Composable
private fun SwipeHorizontalGuide() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Description Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.SwapHoriz,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Switch between active notifications",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = "When you have multiple active items in the Smart Island stack, swipe LEFT or RIGHT on the expanded island card to navigate and switch pages between them.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Looping Animation Demo
        GestureDemoCard(title = "Animation Guide") {
            val infiniteTransition = rememberInfiniteTransition(label = "swipeHorizontal")
            val progress by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(4000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "progress"
            )

            val fingerXOffset: Float
            val fingerAlpha: Float
            val pageTranslationX: Float
            val isPageTwo: Boolean

            val pageWidthPx = with(LocalDensity.current) { 196.dp.toPx() }
            val fingerStartPx = with(LocalDensity.current) { 60.dp.toPx() }
            val fingerEndPx = with(LocalDensity.current) { -60.dp.toPx() }

            when {
                // Phase 1 (0.0 to 0.15): Rest on page 1
                progress < 0.15f -> {
                    fingerXOffset = fingerStartPx
                    fingerAlpha = 0f
                    pageTranslationX = 0f
                    isPageTwo = false
                }
                // Phase 2 (0.15 to 0.25): Finger fades in
                progress < 0.25f -> {
                    val p = (progress - 0.15f) / 0.1f
                    fingerXOffset = fingerStartPx
                    fingerAlpha = p
                    pageTranslationX = 0f
                    isPageTwo = false
                }
                // Phase 3 (0.25 to 0.45): Finger swipes left, dragging page content
                progress < 0.45f -> {
                    val p = (progress - 0.25f) / 0.2f
                    fingerXOffset = fingerStartPx - ((fingerStartPx - fingerEndPx) * p)
                    fingerAlpha = 1f
                    pageTranslationX = -pageWidthPx * p
                    isPageTwo = false
                }
                // Phase 4 (0.45 to 0.55): Finger fades out, Page 2 active
                progress < 0.55f -> {
                    val p = (progress - 0.45f) / 0.1f
                    fingerXOffset = fingerEndPx
                    fingerAlpha = 1f - p
                    pageTranslationX = -pageWidthPx
                    isPageTwo = true
                }
                // Phase 5 (0.55 to 0.65): Finger appears on left
                progress < 0.65f -> {
                    val p = (progress - 0.55f) / 0.1f
                    fingerXOffset = fingerEndPx
                    fingerAlpha = p
                    pageTranslationX = -pageWidthPx
                    isPageTwo = true
                }
                // Phase 6 (0.65 to 0.85): Finger swipes right, dragging page content back
                progress < 0.85f -> {
                    val p = (progress - 0.65f) / 0.2f
                    fingerXOffset = fingerEndPx + ((fingerStartPx - fingerEndPx) * p)
                    fingerAlpha = 1f
                    pageTranslationX = -pageWidthPx + (pageWidthPx * p)
                    isPageTwo = true
                }
                // Phase 7 (0.85 to 0.95): Finger fades out
                progress < 0.95f -> {
                    val p = (progress - 0.85f) / 0.1f
                    fingerXOffset = fingerStartPx
                    fingerAlpha = 1f - p
                    pageTranslationX = 0f
                    isPageTwo = false
                }
                // Phase 8 (0.95 to 1.0): Display page 1 at rest before reset
                else -> {
                    fingerXOffset = fingerStartPx
                    fingerAlpha = 0f
                    pageTranslationX = 0f
                    isPageTwo = false
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(Color.Black.copy(alpha = 0.05f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.TopCenter
            ) {
                SimulatedStatusBar()

                // Simulated Stack concentric arcs representation
                Box(
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .width(228.dp)
                        .height(88.dp)
                        .border(1.5.dp, Color.Black.copy(alpha = 0.25f), RoundedCornerShape(26.dp))
                )

                // Simulated Island Container
                Box(
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .width(220.dp)
                        .height(80.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.Black)
                        .padding(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                translationX = pageTranslationX
                            }
                    ) {
                        Row(modifier = Modifier.width(440.dp)) {
                            // Page 1
                            Box(modifier = Modifier.width(196.dp).fillMaxHeight()) {
                                MockNotificationContent(title = "Slack (1/2)", text = "Hey, check this release build ASAP")
                            }
                            // Page 2
                            Box(modifier = Modifier.width(196.dp).fillMaxHeight()) {
                                MockNotificationContent(title = "Spotify (2/2)", text = "Animesh - Playing Music track")
                            }
                        }
                    }

                    // Pager Page indicator dots at the bottom
                    Row(
                        modifier = Modifier.align(Alignment.BottomCenter),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(modifier = Modifier.size(4.dp).background(if (!isPageTwo) Color.White else Color.White.copy(alpha = 0.4f), CircleShape))
                        Box(modifier = Modifier.size(4.dp).background(if (isPageTwo) Color.White else Color.White.copy(alpha = 0.4f), CircleShape))
                    }
                }

                // Animated finger dot
                if (fingerAlpha > 0f) {
                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                translationX = fingerXOffset
                                alpha = fingerAlpha
                            }
                            .padding(top = 46.dp)
                            .size(34.dp)
                            .border(1.5.dp, Color.White, CircleShape)
                            .background(Color.White.copy(alpha = 0.45f), CircleShape)
                    )
                }
            }
        }

        // Try it yourself Card (Interactive Playground)
        GesturePlaygroundCard(title = "Try it yourself") {
            var currentPage by remember { mutableStateOf(0) }
            var dragOffset by remember { mutableStateOf(0f) }
            val density = LocalDensity.current
            val pageWidthPx = with(density) { 196.dp.toPx() }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(Color.Black.copy(alpha = 0.05f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.TopCenter
            ) {
                SimulatedStatusBar()

                Box(
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .width(220.dp)
                        .height(80.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.Black)
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragStart = { dragOffset = 0f },
                                onHorizontalDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOffset += dragAmount
                                },
                                onDragEnd = {
                                    if (dragOffset < -pageWidthPx / 4f && currentPage == 0) {
                                        currentPage = 1
                                    } else if (dragOffset > pageWidthPx / 4f && currentPage == 1) {
                                        currentPage = 0
                                    }
                                    dragOffset = 0f
                                },
                                onDragCancel = { dragOffset = 0f }
                            )
                        }
                        .padding(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                val pageBaseX = if (currentPage == 0) 0f else -pageWidthPx
                                val constrainedDrag = if (currentPage == 0) dragOffset.coerceAtMost(0f) else dragOffset.coerceAtLeast(0f)
                                translationX = pageBaseX + constrainedDrag.coerceIn(-pageWidthPx, pageWidthPx)
                            }
                    ) {
                        Row(modifier = Modifier.width(440.dp)) {
                            // Page 1
                            Box(modifier = Modifier.width(196.dp).fillMaxHeight()) {
                                MockNotificationContent(title = "Email (1/2)", text = "Swipe left to check the next notification")
                            }
                            // Page 2
                            Box(modifier = Modifier.width(196.dp).fillMaxHeight()) {
                                MockNotificationContent(title = "Calendar (2/2)", text = "Swipe right to return to email card")
                            }
                        }
                    }

                    // Pager Page indicator dots at the bottom
                    Row(
                        modifier = Modifier.align(Alignment.BottomCenter),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(modifier = Modifier.size(4.dp).background(if (currentPage == 0) Color.White else Color.White.copy(alpha = 0.4f), CircleShape))
                        Box(modifier = Modifier.size(4.dp).background(if (currentPage == 1) Color.White else Color.White.copy(alpha = 0.4f), CircleShape))
                    }
                }
            }
        }
    }
}

// ==========================================
// COMMON HELPERS
// ==========================================

@Composable
private fun GestureDemoCard(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun GesturePlaygroundCard(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun SimulatedStatusBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .background(Color.Black.copy(alpha = 0.05f))
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("9:41 AM", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.SignalCellular4Bar, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), modifier = Modifier.size(10.dp))
            Icon(Icons.Rounded.Wifi, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), modifier = Modifier.size(10.dp))
            Icon(Icons.Rounded.Battery5Bar, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), modifier = Modifier.size(10.dp))
        }
    }
}

@Composable
private fun MockNotificationContent(title: String, text: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title.firstOrNull()?.toString() ?: "N",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = text,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 11.sp,
            lineHeight = 14.sp
        )
    }
}
