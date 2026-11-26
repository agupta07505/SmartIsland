/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.service

import android.content.Context
import android.content.res.Configuration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import com.agupta07505.smartisland.ui.IslandViewModel
import com.agupta07505.smartisland.ui.OverlayIsland
import com.agupta07505.smartisland.ui.expanded.LocalReorderPinnedApps
import com.agupta07505.smartisland.ui.islandTextFamily
import com.agupta07505.smartisland.util.CameraCutoutDetector
import com.agupta07505.smartisland.util.CutoutAdjust
import kotlinx.coroutines.flow.StateFlow

/**
 * Compose content of the island window: the island's font, the fade and ring shown while a cutout slider is dragged
 * in settings, and [OverlayIsland] itself. Whatever the window's owner decides comes in as a parameter, so nothing
 * is read from the service. [statusBarHeight] is a function so it is read during composition, as it was when this
 * sat in the service
 */
@Composable
internal fun OverlayIslandContent(
    context: Context,
    viewModel: IslandViewModel,
    isTouchableRegionSupported: StateFlow<Boolean>,
    statusBarHeight: () -> Float,
    launcher: OverlayAppLauncher,
    onReorderPinnedApps: (List<String>) -> Unit,
    onOpenNotificationShade: () -> Unit,
) {
    val fullWidth by isTouchableRegionSupported.collectAsState()
    val fontSettings by viewModel.settings.collectAsState()
    val islandFont = remember(
        fontSettings.islandFont, fontSettings.fontMixEnabled,
        fontSettings.fontMixLight, fontSettings.fontMixRegular, fontSettings.fontMixMedium
    ) {
        // 1 font, or up to 3 mixed by weight (Settings > Island font > Mix fonts)
        islandTextFamily(context, fontSettings)
    }
    CompositionLocalProvider(LocalReorderPinnedApps provides onReorderPinnedApps) {
        ProvideTextStyle(TextStyle(fontFamily = islandFont)) {
            // settings screen: a cutout slider is dragged, the island fades and the hole is ringed
            val cutoutAdjusting by CutoutAdjust.dragging.collectAsState()
            val adjustAlpha by animateFloatAsState(
                if (cutoutAdjusting) CutoutAdjust.ISLAND_ALPHA else 1f, tween(150), label = "cutoutAdjust"
            )
            val adjustConfig = LocalConfiguration.current
            val adjustView = LocalView.current
            val adjustHole = if (cutoutAdjusting) {
                CameraCutoutDetector.areaFor(
                    fontSettings, CameraCutoutDetector.detect(context),
                    adjustConfig.orientation == Configuration.ORIENTATION_LANDSCAPE,
                    adjustConfig.screenWidthDp.toFloat(), assumeCentre = true
                )
            } else null
            Box(
                modifier = Modifier
                    .drawWithContent {
                        drawContent()
                        if (adjustHole != null) {
                            val origin = IntArray(2).also { adjustView.getLocationOnScreen(it) }
                            val d = density
                            val centre = Offset(
                                adjustConfig.screenWidthDp * d / 2f + adjustHole.centreXDp * d - origin[0],
                                (adjustHole.topDp + adjustHole.heightDp / 2f) * d - origin[1]
                            )
                            drawCircle(
                                color = Color(0xFF4FC3F7), radius = maxOf(adjustHole.widthDp,
                                    adjustHole.heightDp) / 2f * d + 3f * d,
                                center = centre, style = Stroke(width = 2f * d)
                            )
                        }
                    }
                    .graphicsLayer { alpha = adjustAlpha }
            ) {
                OverlayIsland(
                    viewModel = viewModel,
                    statusBarHeight = statusBarHeight(),
                    onOpenNotification = { notification -> launcher.openNotification(notification) },
                    onLaunchApp = { packageName -> launcher.launchApp(packageName) },
                    onAppAction = { packageName, action -> launcher.launchAppAction(packageName, action) },
                    onOpenFloatingWindow = { launcher.openCurrentNotificationInFloatingWindow() },
                    onOpenNotificationShade = onOpenNotificationShade,
                    isFullWidth = fullWidth
                )
            }
        }
    }
}
