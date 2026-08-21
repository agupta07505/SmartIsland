/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.sections

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GesturesSection() {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        "1. Tap",
        "2. Swipe Up",
        "3. Hold + Swipe Up",
        "4. Swipe Down",
        "5. Swipe Left/Right"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Quick Reference Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Gesture,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Smart Island Gesture Guide",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "5 intuitive gestures to control notifications, calls & multitasking",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Summary Row Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GestureSummaryBadge(icon = Icons.Rounded.TouchApp, label = "Tap", sub = "Open App", color = Color(0xFF38BDF8), modifier = Modifier.weight(1f))
                    GestureSummaryBadge(icon = Icons.Rounded.ArrowUpward, label = "Swipe ↑", sub = "Dismiss", color = Color(0xFFEF4444), modifier = Modifier.weight(1f))
                    GestureSummaryBadge(icon = Icons.Rounded.DeleteSweep, label = "Hold+↑", sub = "Clear All", color = Color(0xFFF59E0B), modifier = Modifier.weight(1f))
                    GestureSummaryBadge(icon = Icons.Rounded.ArrowDownward, label = "Swipe ↓", sub = "Floating", color = Color(0xFF10B981), modifier = Modifier.weight(1f))
                    GestureSummaryBadge(icon = Icons.Rounded.Swipe, label = "Swipe ↔", sub = "Switch", color = Color(0xFFA855F7), modifier = Modifier.weight(1f))
                }
            }
        }

        // Scrollable Tabs for Selecting Each Gesture Guide
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            edgePadding = 0.dp,
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
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                )
            }
        }

        // Detailed Text Step-by-Step Instructions for Selected Gesture
        when (selectedTab) {
            0 -> GestureDetailCard(
                gestureNumber = "Gesture 1 of 5",
                gestureName = "Single Tap & Hold",
                actionBadge = "Open App / Expand",
                badgeColor = Color(0xFF38BDF8),
                icon = Icons.Rounded.TouchApp,
                overview = "Tap the island to immediately launch the active app (Spotify, WhatsApp, Phone, Clock, Maps), or hold / swipe down to expand rich controls in place.",
                steps = listOf(
                    "Open App (Single Tap): Tap once on the collapsed pill to instantly launch the corresponding active application.",
                    "Open Shortcuts: When no alerts are active, a single tap opens your customizable quick-launch app shortcuts.",
                    "Expand Island (Hold / Long-Press): Press and hold for 300ms (or swipe down) to expand the full interactive card with media scrubbers, timers, and action buttons without leaving your current app.",
                    "Collapse Back: When expanded, tap anywhere on the empty screen background to smoothly collapse the island back."
                ),
                proTip = "Tapping secondary split bubbles (like music artwork or timer circle) opens that specific app directly!"
            )
            1 -> GestureDetailCard(
                gestureNumber = "Gesture 2 of 5",
                gestureName = "Quick Swipe Up",
                actionBadge = "Dismiss Current Notification",
                badgeColor = Color(0xFFEF4444),
                icon = Icons.Rounded.ArrowUpward,
                overview = "Dismiss the currently active notification card from the island stack without clearing other pending notifications.",
                steps = listOf(
                    "Touch Position: Touch anywhere on the expanded notification card.",
                    "Finger Motion: Quickly flick or swipe your finger upward toward the top bezel of your device.",
                    "Threshold: Drag upward by at least 48dp and release your finger.",
                    "Visual Response: The card animates upward with momentum and leaves the screen. If more notifications exist, the next one smoothly slides forward."
                ),
                proTip = "If you release your finger before reaching the swipe threshold, spring physics smoothly restores the card back to center."
            )
            2 -> GestureDetailCard(
                gestureNumber = "Gesture 3 of 5",
                gestureName = "Hold + Swipe Up",
                actionBadge = "Clear ALL Notifications",
                badgeColor = Color(0xFFF59E0B),
                icon = Icons.Rounded.DeleteSweep,
                overview = "Clear all active notifications from Smart Island at once in a single fast motion without dismissing cards one by one.",
                steps = listOf(
                    "Touch Position: Touch and hold your finger on the expanded island card.",
                    "Hold Duration: Keep your finger down for 300ms until you feel a distinct haptic vibration pulse.",
                    "Finger Motion: As soon as you feel the vibration, immediately swipe your finger upward toward the top of the screen and release.",
                    "Visual Response: All pending notifications in the stack are dismissed simultaneously, and the island collapses cleanly."
                ),
                proTip = "The haptic vibration confirms that 'Clear All' mode is engaged. Swiping up before the vibration only dismisses the single active notification."
            )
            3 -> GestureDetailCard(
                gestureNumber = "Gesture 4 of 5",
                gestureName = "Swipe Down",
                actionBadge = "Open in Floating Window",
                badgeColor = Color(0xFF10B981),
                icon = Icons.Rounded.ArrowDownward,
                overview = "Launch the notification's app directly into a freeform floating window overlay for seamless multitasking.",
                steps = listOf(
                    "Touch Position: Touch the expanded notification card.",
                    "Finger Motion: Drag or swipe downward toward the center of your screen by at least 48dp.",
                    "Release: Release your finger once the downward drag threshold is reached.",
                    "Visual Response: SmartIsland triggers a freeform floating window for the target application over your current app."
                ),
                proTip = "Freeform floating window mode works best when Shizuku service is running or on Android ROMs with native freeform multi-window enabled."
            )
            4 -> GestureDetailCard(
                gestureNumber = "Gesture 5 of 5",
                gestureName = "Swipe Left / Right",
                actionBadge = "Switch Between Notifications",
                badgeColor = Color(0xFFA855F7),
                icon = Icons.Rounded.Swipe,
                overview = "Browse through multiple active notifications or toggle between media playback and notifications in your stack.",
                steps = listOf(
                    "Prerequisite: 2 or more notifications or an active media session are present in Smart Island.",
                    "Touch Position: Place your finger on the expanded card.",
                    "Finger Motion: Swipe horizontally to the LEFT to view the next notification, or swipe to the RIGHT to return to the previous one.",
                    "Snapping: The horizontal pager automatically snaps cleanly to the centered card with smooth physics."
                ),
                proTip = "You can swipe between cards as fast as you like — the updated pager uses settled-page synchronization to ensure cards never get stuck midway."
            )
        }
    }
}

@Composable
private fun GestureSummaryBadge(
    icon: ImageVector,
    label: String,
    sub: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
            Text(sub, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun GestureDetailCard(
    gestureNumber: String,
    gestureName: String,
    actionBadge: String,
    badgeColor: Color,
    icon: ImageVector,
    overview: String,
    steps: List<String>,
    proTip: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(badgeColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = null, tint = badgeColor, modifier = Modifier.size(24.dp))
                    }
                    Column {
                        Text(
                            text = gestureNumber,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor
                        )
                        Text(
                            text = gestureName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = badgeColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = actionBadge,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Overview
            Text(
                text = overview,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            // Step-by-Step Instructions
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "How to Perform This Gesture:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                steps.forEachIndexed { index, step ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(badgeColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${index + 1}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = badgeColor
                            )
                        }
                        Text(
                            text = step,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 18.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Pro Tip Box
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Rounded.Lightbulb,
                        contentDescription = "Pro Tip",
                        tint = Color(0xFFFACC15),
                        modifier = Modifier.size(18.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "PRO TIP",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFFACC15)
                        )
                        Text(
                            text = proTip,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}
