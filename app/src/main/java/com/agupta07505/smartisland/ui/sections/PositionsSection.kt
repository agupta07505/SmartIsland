/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.sections

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.data.NoCutoutPrompt
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.SmartIslandSettingsRepository
import com.agupta07505.smartisland.model.CameraSide
import com.agupta07505.smartisland.model.CutoutArea
import com.agupta07505.smartisland.model.CutoutFit
import com.agupta07505.smartisland.model.PresetPlace
import com.agupta07505.smartisland.model.QuickPresets
import com.agupta07505.smartisland.model.SideCamera
import com.agupta07505.smartisland.ui.components.CollapsibleCard
import com.agupta07505.smartisland.ui.rememberCameraCutoutArea
import com.agupta07505.smartisland.ui.rememberLiveSaver
import com.agupta07505.smartisland.util.CameraCutoutDetector
import kotlinx.coroutines.launch

@Composable
fun PositionsSection(
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository,
    onNavigateToBackup: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val configuration = LocalConfiguration.current

    val localWidthState = remember { mutableFloatStateOf(settings.width) }
    var localWidth by localWidthState
    val localContentPercentState =
        remember(settings.contentPillWidthPercent) { mutableFloatStateOf(settings.contentPillWidthPercent) }
    val localHeightState = remember { mutableFloatStateOf(settings.height) }
    var localHeight by localHeightState
    val localXOffsetState = remember { mutableFloatStateOf(settings.xOffset) }
    var localXOffset by localXOffsetState
    val localYOffsetState = remember { mutableFloatStateOf(settings.yOffset) }
    var localYOffset by localYOffsetState
    val localCutoutXState = remember { mutableFloatStateOf(settings.cameraCutoutX) }
    var localCutoutX by localCutoutXState
    val localCutoutYState = remember { mutableFloatStateOf(settings.cameraCutoutY) }
    var localCutoutY by localCutoutYState
    val localCutoutWidthState = remember { mutableFloatStateOf(settings.cameraCutoutWidth) }
    var localCutoutWidth by localCutoutWidthState
    val localCutoutHeightState = remember { mutableFloatStateOf(settings.cameraCutoutHeight) }
    var localCutoutHeight by localCutoutHeightState
    // Pill size and position: live save while dragging
    var pillDragging by remember { mutableStateOf(false) }
    LaunchedEffect(settings.width, settings.height, settings.xOffset, settings.yOffset, pillDragging) {
        if (!pillDragging) {
            localWidth = settings.width
            localHeight = settings.height
            localXOffset = settings.xOffset
            localYOffset = settings.yOffset
        }
    }
    val livePillBox = rememberLiveSaver<List<Float>> {
        repository.setPosition(width = it[0], height = it[1], xOffset = it[2], yOffset = it[3])
    }
    val livePill = { w: Float, h: Float, x: Float, y: Float ->
        pillDragging = true
        livePillBox(listOf(w, h, x, y))
    }
    val savePill = {
        scope.launch {
            repository.setPosition(width = localWidth, height = localHeight, xOffset = localXOffset,
                yOffset = localYOffset)
            pillDragging = false
        }
        Unit
    }
    // Manual camera cutout: the sliders save live while dragged
    val cutoutDraggingState = remember { mutableStateOf(false) }
    var cutoutDragging by cutoutDraggingState
    LaunchedEffect(settings.cameraCutoutX, settings.cameraCutoutY, settings.cameraCutoutWidth,
        settings.cameraCutoutHeight, cutoutDragging) {
        if (!cutoutDragging) {
            localCutoutX = settings.cameraCutoutX
            localCutoutY = settings.cameraCutoutY
            localCutoutWidth = settings.cameraCutoutWidth
            localCutoutHeight = settings.cameraCutoutHeight
        }
    }
    val liveCutoutArea = rememberLiveSaver<CutoutArea> {
        repository.setCameraCutoutArea(it.centreXDp, it.topDp, it.widthDp, it.heightDp)
    }
    val localCornerRadiusState = remember(settings.cornerRadius) { mutableFloatStateOf(settings.cornerRadius) }
    val localShadowElevationState = remember(settings.shadowElevation) { mutableFloatStateOf(settings.shadowElevation) }

    val screenWidthDp = configuration.screenWidthDp.toFloat()
    val detectedCutout = remember(context, configuration, CameraCutoutDetector.generation) {
        CameraCutoutDetector.detect(context)
    }
    val presetHole = rememberCameraCutoutArea(settings, false).let { hole ->
        val width = teardropDefaultWidthDp(settings, context)
        if (hole != null && width != null) hole.copy(widthDp = width) else hole
    }
    val manualHole = settings.cameraCutoutManual
    fun movesHole(place: PresetPlace) = CutoutFit.presetMovesHole(presetHole, place, manualHole)
    val centrePlan = QuickPresets.plan(screenWidthDp, presetHole, 112f, PresetPlace.Centre,
        movesHole(PresetPlace.Centre))
    val widePlan = QuickPresets.plan(screenWidthDp, presetHole, 150f, PresetPlace.Centre, movesHole(PresetPlace.Centre))
    val leftPlan = QuickPresets.plan(screenWidthDp, presetHole, CutoutFit.SIDE_BASE_WIDTH_DP, PresetPlace.Left,
        movesHole(PresetPlace.Left))
    val rightPlan = QuickPresets.plan(screenWidthDp, presetHole, CutoutFit.SIDE_BASE_WIDTH_DP, PresetPlace.Right,
        movesHole(PresetPlace.Right))
    val holeSize = CutoutFit.sizeFor(presetHole)
    var showRedetectConfirm by remember { mutableStateOf(false) }
    val cameraSide = SideCamera.sideOf(presetHole?.centreXDp)
    val sideActive = cameraSide != CameraSide.Centre

    if (showRedetectConfirm) {
        RedetectCutoutDialog(
            repository = repository,
            scope = scope,
            context = context,
            screenWidthDp = screenWidthDp,
            onDismiss = { showRedetectConfirm = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        LayoutModeSwitch(
            settings = settings,
            repository = repository,
            scope = scope,
            context = context,
            configuration = configuration,
            screenWidthDp = screenWidthDp
        )

        CollapsibleCard(
            title = "Quick presets",
            stateKey = "pos_presets_${if (settings.enableNotchMode) "notch" else "pill"}",
            summary = "One-tap layouts for common camera cutouts",
            initiallyExpanded = false,
            framed = false
        ) {
            QuickPresetsCard(
                settings = settings,
                repository = repository,
                scope = scope,
                context = context,
                configuration = configuration,
                screenWidthDp = screenWidthDp,
                centrePlan = centrePlan,
                widePlan = widePlan,
                leftPlan = leftPlan,
                rightPlan = rightPlan,
                holeSize = holeSize,
                cameraSide = cameraSide,
                sideActive = sideActive,
                onRedetect = { showRedetectConfirm = true }
            )
        }

        CollapsibleCard(
            title = "Exempt apps",
            stateKey = "pos_exempt_apps",
            summary = "Apps that switch the island totally off in portrait",
            initiallyExpanded = false
        ) {
            ExemptAppsCard(settings = settings, repository = repository, landscape = false)
        }

        CollapsibleCard(
            title = "Size and position",
            stateKey = "pos_tuning_${if (settings.enableNotchMode) "notch" else "pill"}",
            summary = "Width, height, offsets and corner radius",
            initiallyExpanded = false,
            framed = false
        ) {
            SizeAndPositionCard(
                settings = settings,
                repository = repository,
                scope = scope,
                screenWidthDp = screenWidthDp,
                presetHole = presetHole,
                sideActive = sideActive,
                localWidthState = localWidthState,
                localHeightState = localHeightState,
                localXOffsetState = localXOffsetState,
                localYOffsetState = localYOffsetState,
                localContentPercentState = localContentPercentState,
                localCornerRadiusState = localCornerRadiusState,
                livePill = livePill,
                savePill = savePill
            )
        }

        // Camera position: style, detected hole, manual area (portrait; the style also feeds landscape)
        CollapsibleCard(
            title = stringResource(R.string.camera_cutout_title),
            stateKey = "pos_camera_cutout",
            summary = stringResource(R.string.camera_cutout_summary),
            initiallyExpanded = false,
            framed = false,
            searchKeyPrefix = NoCutoutPrompt.CARD_KEY_PREFIX
        ) {
            CameraCutoutCard(
                settings = settings,
                repository = repository,
                scope = scope,
                screenWidthDp = screenWidthDp,
                detected = detectedCutout,
                localCutoutXState = localCutoutXState,
                localCutoutYState = localCutoutYState,
                localCutoutWidthState = localCutoutWidthState,
                localCutoutHeightState = localCutoutHeightState,
                cutoutDraggingState = cutoutDraggingState,
                liveCutoutArea = liveCutoutArea
            )
        }

        CollapsibleCard(
            title = "Shadow",
            stateKey = "pos_look_${if (settings.enableNotchMode) "notch" else "pill"}",
            summary = "Drop shadow under the island (opacity and colour are in Colors)",
            initiallyExpanded = false,
            framed = false
        ) {
            ShadowCard(
                settings = settings,
                repository = repository,
                scope = scope,
                localShadowElevationState = localShadowElevationState
            )
        }
    }
}
