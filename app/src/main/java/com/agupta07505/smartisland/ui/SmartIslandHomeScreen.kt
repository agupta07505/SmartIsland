/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agupta07505.smartisland.data.INotificationRepository
import com.agupta07505.smartisland.data.NoCutoutPrompt
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.SmartIslandSettingsRepository
import com.agupta07505.smartisland.di.SmartIslandRepositories
import com.agupta07505.smartisland.model.CutoutFit
import com.agupta07505.smartisland.service.SystemServiceRecovery
import com.agupta07505.smartisland.ui.glass.ThemedBackdrop
import com.agupta07505.smartisland.ui.search.LocalSearchFocus
import com.agupta07505.smartisland.ui.search.SEARCH_FOCUS_ARMED_MS
import com.agupta07505.smartisland.ui.search.SearchFocusState
import com.agupta07505.smartisland.ui.search.SearchTarget
import com.agupta07505.smartisland.ui.search.SettingsSearch
import com.agupta07505.smartisland.ui.search.SettingsSearchEntry
import com.agupta07505.smartisland.ui.search.buildSettingsSearchIndex
import com.agupta07505.smartisland.ui.sections.AnimationSpeedSection
import com.agupta07505.smartisland.ui.sections.DemoSection
import com.agupta07505.smartisland.ui.sections.EditingProfile
import com.agupta07505.smartisland.ui.sections.EventTogglesSection
import com.agupta07505.smartisland.ui.sections.IdleWidgetsSection
import com.agupta07505.smartisland.ui.sections.LandscapeSection
import com.agupta07505.smartisland.ui.sections.LayoutMode
import com.agupta07505.smartisland.ui.sections.LayoutModeTabs
import com.agupta07505.smartisland.ui.sections.MergeStyleSection
import com.agupta07505.smartisland.ui.sections.NewUserTour
import com.agupta07505.smartisland.ui.sections.PositionsSection
import com.agupta07505.smartisland.ui.sections.rememberEditingProfile
import com.agupta07505.smartisland.util.CameraCutoutDetector
import com.agupta07505.smartisland.util.isOverlayAccessibilityServiceEnabled
import com.agupta07505.smartisland.util.quietly
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

internal enum class StudioTab {
    Studio,
    Position,
    Demo,
    Settings
}

internal enum class FeatureDetailSection {
    NotificationRules,
    AppShortcuts,
    NotificationHistory,
    ColorStudio,
    GesturesGuide,
    PermissionsCenter,
    BackupRestore,
    Diagnostics,
    Timing,
    Apps,
    IdleWidgets,
    About,
    UpdatesAndDownloads,
    SupportCommunity,
    DeveloperOptions,
    FaceIdSettings,
    WeatherSettings,
    NetSpeedSettings,
    CalendarSettings,
    StatusBarHide
}

private val IDLE_SETTINGS_PAGES = setOf(
    FeatureDetailSection.WeatherSettings,
    FeatureDetailSection.NetSpeedSettings,
    FeatureDetailSection.CalendarSettings
)

@SuppressLint("BatteryLife")
@Composable
fun SmartIslandHomeScreen(
    repository: SmartIslandSettingsRepository? = null,
    notificationRepository: INotificationRepository? = null,
    openSection: String? = null,
    onOpenSectionHandled: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val resolvedRepository = remember(repository, context) {
        repository ?: runCatching {
            SmartIslandRepositories.settingsRepository(context)
        }.getOrElse {
            SmartIslandSettingsRepository(context.applicationContext)
        }
    }
    val resolvedNotificationRepository = remember(notificationRepository, context) {
        notificationRepository ?: quietly {
            SmartIslandRepositories.notificationRepository(context)
        }
    }

    val settings by resolvedRepository.settings.collectAsStateWithLifecycle(initialValue = SmartIslandSettings.Default)
    val scope = rememberCoroutineScope()

    var showWelcomeDialog by rememberSaveable { mutableStateOf(false) }

    CutoutFitOnFirstEntry(resolvedRepository, context)

    LaunchedEffect(Unit) {
        if (!resolvedRepository.isWelcomeDialogShown()) {
            showWelcomeDialog = true
        }
    }

    var showTour by rememberSaveable { mutableStateOf(false) }

    if (showWelcomeDialog) {
        WelcomeDialog(
            onDismiss = {
                showWelcomeDialog = false
                scope.launch { resolvedRepository.setWelcomeDialogShown(true) }
            },
            onTakeTour = {
                showWelcomeDialog = false
                showTour = true
                scope.launch { resolvedRepository.setWelcomeDialogShown(true) }
            }
        )
    }

    if (showTour) {
        NewUserTour(settings = settings, onFinish = { showTour = false })
    }

    val permissions = rememberHomePermissions(context, lifecycleOwner)
    val overlayGranted by permissions.overlay
    val notificationGranted by permissions.notification
    val batteryIgnored by permissions.battery
    val bluetoothGranted by permissions.bluetooth

    var selectedTab by rememberSaveable { mutableStateOf(StudioTab.Studio) }
    var layoutMode by rememberSaveable { mutableStateOf(LayoutMode.Portrait) }
    val editing = rememberEditingProfile(resolvedRepository, settings, layoutMode)
    var activeDetailSection by rememberSaveable { mutableStateOf<FeatureDetailSection?>(null) }
    var transitionDirection by rememberSaveable { mutableIntStateOf(1) }

    // settings search: the query survives rotation
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val searchFocus = remember { SearchFocusState() }
    val configuration = LocalConfiguration.current
    val searchIndex = remember(context, configuration) {
        buildSettingsSearchIndex { id -> context.getString(id) }
    }
    val searchResults = remember(searchIndex, searchQuery) {
        val byId = searchIndex.associateBy { it.item.id }
        SettingsSearch.search(searchIndex.map { it.item }, searchQuery).mapNotNull { byId[it.id] }
    }
    // a jump stays armed for a few seconds
    LaunchedEffect(searchFocus.token) {
        if (searchFocus.key != null) {
            delay(SEARCH_FOCUS_ARMED_MS)
            searchFocus.clear()
        }
    }
    val onSearchPick: (SettingsSearchEntry) -> Unit = { entry ->
        when (val target = entry.target) {
            is SearchTarget.Tab -> {
                transitionDirection = if (target.tab.ordinal > selectedTab.ordinal) 1 else -1
                selectedTab = target.tab
                target.landscape?.let {
                    layoutMode = if (it) LayoutMode.Landscape else LayoutMode.Portrait
                }
                // Already on the Settings tab: show the normal overview, where the item is
                if (target.tab == StudioTab.Settings) searchQuery = ""
            }
            is SearchTarget.Detail -> {
                transitionDirection = 1
                activeDetailSection = target.section
            }
        }
        searchFocus.jumpTo(entry.rowKey)
    }

    val canEnable = overlayGranted && notificationGranted
    val navigateTo: (FeatureDetailSection) -> Unit = { section ->
        transitionDirection = 1
        activeDetailSection = section
    }

    var showNoCutoutPrompt by rememberSaveable { mutableStateOf(false) }
    NoCutoutPromptOnEntry(
        resolvedRepository = resolvedRepository,
        context = context,
        welcomeShown = settings.welcomeDialogShown,
        blocked = showTour,
        onAsk = { showNoCutoutPrompt = true }
    )
    if (showNoCutoutPrompt) {
        val answered = {
            showNoCutoutPrompt = false
            scope.launch { resolvedRepository.markNoCutoutPromptDone() }
            Unit
        }
        NoCutoutPromptDialog(
            onOpenCard = {
                answered()
                transitionDirection =
                    if (activeDetailSection == null && StudioTab.Position.ordinal > selectedTab.ordinal) 1 else -1
                searchQuery = ""
                layoutMode = LayoutMode.Portrait
                selectedTab = StudioTab.Position
                activeDetailSection = null
                searchFocus.jumpTo(NoCutoutPrompt.ROW_KEY)
            },
            onDismiss = answered
        )
    }

    // The island's settings button
    LaunchedEffect(openSection) {
        if (openSection == UtilityNavigation.SECTION_APP_SHORTCUTS) {
            transitionDirection = 1
            activeDetailSection = FeatureDetailSection.AppShortcuts
            onOpenSectionHandled()
        }
    }

    BackHandler(enabled = activeDetailSection != null) {
        transitionDirection = -1
        // the weather page is opened from the Idle widgets page, so back goes there
        activeDetailSection = if (activeDetailSection in IDLE_SETTINGS_PAGES) FeatureDetailSection.IdleWidgets else null
    }

    CompositionLocalProvider(LocalSearchFocus provides searchFocus) {
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (activeDetailSection == null) {
                StudioBottomNavigationBar(
                    selectedTab = selectedTab,
                    onTabSelected = { tab ->
                        transitionDirection = if (tab.ordinal > selectedTab.ordinal) 1 else -1
                        selectedTab = tab
                    }
                )
            }
        }
    ) { scaffoldPadding ->
        AnimatedContent(
            targetState = activeDetailSection,
            modifier = Modifier.padding(scaffoldPadding),
            transitionSpec = {
                if (transitionDirection == 1) {
                    (slideInHorizontally(initialOffsetX = { it }) + fadeIn())
                        .togetherWith(slideOutHorizontally(targetOffsetX = { -it }) + fadeOut())
                } else {
                    (slideInHorizontally(initialOffsetX = { -it }) + fadeIn())
                        .togetherWith(slideOutHorizontally(targetOffsetX = { it }) + fadeOut())
                }
            },
            label = "ScreenTransition"
        ) { detailSection ->
            Box(Modifier.fillMaxSize()) {
            ThemedBackdrop()
            if (detailSection == null) {
                StudioPage(
                    selectedTab = selectedTab,
                    settings = settings,
                    editing = editing,
                    canEnable = canEnable,
                    overlayGranted = overlayGranted,
                    notificationGranted = notificationGranted,
                    batteryIgnored = batteryIgnored,
                    resolvedRepository = resolvedRepository,
                    resolvedNotificationRepository = resolvedNotificationRepository,
                    searchQuery = searchQuery,
                    searchResults = searchResults,
                    onSearchQueryChange = { searchQuery = it },
                    onSearchPick = onSearchPick,
                    onLayoutModeChange = { layoutMode = it },
                    onNavigateTo = navigateTo,
                    scope = scope,
                    context = context
                )
            } else {
                DetailScreenHost(
                    section = detailSection,
                    settings = settings,
                    repository = resolvedRepository,
                    overlayGranted = overlayGranted,
                    notificationGranted = notificationGranted,
                    batteryIgnored = batteryIgnored,
                    bluetoothGranted = bluetoothGranted,
                    onBack = {
                        transitionDirection = -1
                        activeDetailSection =
                            if (detailSection in IDLE_SETTINGS_PAGES) FeatureDetailSection.IdleWidgets else null
                    },
                    onBluetoothClick = { permissions.onBluetoothClick(context) },
                    onRefreshPermissions = { permissions.refreshAll(context) },
                    onNavigateTo = navigateTo
                )
            }
            }
        }
    }
    }
}

@NonRestartableComposable
@Composable
private fun StudioPage(
    selectedTab: StudioTab,
    settings: SmartIslandSettings,
    editing: EditingProfile,
    canEnable: Boolean,
    overlayGranted: Boolean,
    notificationGranted: Boolean,
    batteryIgnored: Boolean,
    resolvedRepository: SmartIslandSettingsRepository,
    resolvedNotificationRepository: INotificationRepository?,
    searchQuery: String,
    searchResults: List<SettingsSearchEntry>,
    onSearchQueryChange: (String) -> Unit,
    onSearchPick: (SettingsSearchEntry) -> Unit,
    onLayoutModeChange: (LayoutMode) -> Unit,
    onNavigateTo: (FeatureDetailSection) -> Unit,
    scope: CoroutineScope,
    context: Context
) {
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val horizontalPad = contentSidePadding(screenWidthDp)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(
                start = horizontalPad,
                end = horizontalPad,
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 12.dp,
                bottom = 32.dp
            ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        StudioTopHeader(
            isIslandEnabled = settings.enabled,
            canEnable = canEnable,
            onHealthClick = {
                onNavigateTo(FeatureDetailSection.PermissionsCenter)
            }
        )

        when (selectedTab) {
            StudioTab.Studio -> StudioMainTab(
                settings, editing, canEnable, overlayGranted, notificationGranted, batteryIgnored,
                resolvedRepository, onLayoutModeChange, onNavigateTo, scope, context
            )

            StudioTab.Position -> PositionTab(
                settings, editing, resolvedRepository, onLayoutModeChange, onNavigateTo
            )

            StudioTab.Demo -> {
                DemoSection(
                    settings = settings,
                    notificationRepository = resolvedNotificationRepository
                )
            }

            StudioTab.Settings -> SettingsTab(
                settings, searchQuery, onSearchQueryChange, searchResults, onSearchPick,
                overlayGranted, notificationGranted, batteryIgnored, resolvedRepository, onNavigateTo, scope
            )
        }
    }
}

@NonRestartableComposable
@Composable
private fun StudioMainTab(
    settings: SmartIslandSettings,
    editing: EditingProfile,
    canEnable: Boolean,
    overlayGranted: Boolean,
    notificationGranted: Boolean,
    batteryIgnored: Boolean,
    resolvedRepository: SmartIslandSettingsRepository,
    onLayoutModeChange: (LayoutMode) -> Unit,
    onNavigateTo: (FeatureDetailSection) -> Unit,
    scope: CoroutineScope,
    context: Context
) {
    MasterPowerCard(
        enabled = settings.enabled,
        canEnable = canEnable,
        overlayGranted = overlayGranted,
        notificationGranted = notificationGranted,
        batteryIgnored = batteryIgnored,
        onCheckedChange = { turnOn ->
            if (turnOn) {
                SystemServiceRecovery.requestRecovery(context)
            }
            scope.launch { resolvedRepository.setEnabled(turnOn) }
        },
        onSetupPermissionsClick = {
            onNavigateTo(FeatureDetailSection.PermissionsCenter)
        }
    )

    LayoutModeTabs(
        selected = editing.mode,
        foldableMode = editing.foldableMode,
        onSelected = onLayoutModeChange
    )
    EventTogglesSection(
        settings = editing.settings,
        repository = editing.repository,
        landscape = editing.mode.landscape,
        onOpenFaceIdSettings = {
            onNavigateTo(FeatureDetailSection.FaceIdSettings)
        }
    )
}

@NonRestartableComposable
@Composable
private fun PositionTab(
    settings: SmartIslandSettings,
    editing: EditingProfile,
    resolvedRepository: SmartIslandSettingsRepository,
    onLayoutModeChange: (LayoutMode) -> Unit,
    onNavigateTo: (FeatureDetailSection) -> Unit
) {
    LayoutModeTabs(
        selected = editing.mode,
        foldableMode = editing.foldableMode,
        onSelected = onLayoutModeChange
    )
    run {
        if (editing.mode.landscape) {
            LandscapeSection(
                settings = editing.settings,
                repository = editing.repository
            )
        } else {
            PositionsSection(
                settings = editing.settings,
                repository = editing.repository,
                onNavigateToBackup = {
                    onNavigateTo(FeatureDetailSection.BackupRestore)
                }
            )
        }
    }
    AnimationSpeedSection(
        settings = editing.settings,
        repository = editing.repository,
        landscape = editing.mode.landscape
    )
    MergeStyleSection(
        settings = editing.settings,
        repository = editing.repository,
        landscape = editing.mode.landscape
    )
    IdleWidgetsSection(
        settings = settings,
        repository = resolvedRepository,
        onOpenSettings = {
            onNavigateTo(FeatureDetailSection.IdleWidgets)
        }
    )
}

@NonRestartableComposable
@Composable
private fun SettingsTab(
    settings: SmartIslandSettings,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    searchResults: List<SettingsSearchEntry>,
    onSearchPick: (SettingsSearchEntry) -> Unit,
    overlayGranted: Boolean,
    notificationGranted: Boolean,
    batteryIgnored: Boolean,
    resolvedRepository: SmartIslandSettingsRepository,
    onNavigateTo: (FeatureDetailSection) -> Unit,
    scope: CoroutineScope
) {
    SettingsOverviewSection(
        settings = settings,
        searchQuery = searchQuery,
        onSearchQueryChange = onSearchQueryChange,
        searchResults = searchResults,
        onSearchPick = onSearchPick,
        overlayGranted = overlayGranted,
        notificationGranted = notificationGranted,
        batteryIgnored = batteryIgnored,
        onThemeModeChange = { mode ->
            scope.launch { resolvedRepository.setThemeMode(mode) }
        },
        onLiquidGlassChange = { on -> scope.launch { resolvedRepository.setLiquidGlassTheme(on) } },
        onMaterialYouChange = { on -> scope.launch { resolvedRepository.setMaterialYou(on) } },
        onAccentChange = { argb -> scope.launch { resolvedRepository.setAccentColor(argb) } },
        onIslandFontChange = { font ->
            scope.launch { resolvedRepository.setIslandFont(font) }
        },
        onFontMixChange = { enabled, light, regular, medium ->
            scope.launch { resolvedRepository.setFontMix(enabled, light, regular, medium) }
        },
        onNavigateTo = onNavigateTo
    )
}

@NonRestartableComposable
@Composable
private fun CutoutFitOnFirstEntry(resolvedRepository: SmartIslandSettingsRepository, context: Context) {
    val isPortraitNow = LocalConfiguration.current.orientation ==
        Configuration.ORIENTATION_PORTRAIT
    val screenWidthDpNow = LocalConfiguration.current.screenWidthDp.toFloat()
    // first entry after an install
    LaunchedEffect(isPortraitNow) {
        val first = resolvedRepository.settings.first()
        val shouldFit = CutoutFit.shouldRunOnFirstEntry(
            welcomeShown = first.welcomeDialogShown,
            fitDone = resolvedRepository.isCutoutFitDone(),
            notchMode = first.enableNotchMode,
            isPortrait = isPortraitNow
        )
        if (!shouldFit) {
            // notch mode or an existing install never fits
            if (isPortraitNow || first.welcomeDialogShown
                || first.enableNotchMode) resolvedRepository.markCutoutFitDone()
            return@LaunchedEffect
        }
        val area = CameraCutoutDetector.detectedArea(
            CameraCutoutDetector.detect(context)
        )
        val plan = CutoutFit.plan(screenWidthDpNow, area)
        if (plan != null) resolvedRepository.applyCutoutFit(plan,
            edgeHole = area) else resolvedRepository.markCutoutFitDone()
    }
}

/** Asks once, after the welcome dialog and the tour, where the camera is when nothing was detected. */
@NonRestartableComposable
@Composable
private fun NoCutoutPromptOnEntry(
    resolvedRepository: SmartIslandSettingsRepository,
    context: Context,
    welcomeShown: Boolean,
    blocked: Boolean,
    onAsk: () -> Unit
) {
    val isPortraitNow = LocalConfiguration.current.orientation ==
        Configuration.ORIENTATION_PORTRAIT
    LaunchedEffect(isPortraitNow, welcomeShown, blocked) {
        if (!welcomeShown || blocked) return@LaunchedEffect
        val settings = resolvedRepository.settings.first()
        val ask = NoCutoutPrompt.shouldShow(
            hasHardwareCutout = CameraCutoutDetector.detect(context).hasHardwareCutout,
            style = settings.cameraCutoutStyle,
            manualArea = settings.cameraCutoutManual,
            dismissed = resolvedRepository.isNoCutoutPromptDone(),
            isPortrait = isPortraitNow
        )
        if (ask) onAsk()
    }
}

private class HomePermissions(
    val overlay: MutableState<Boolean>,
    val notification: MutableState<Boolean>,
    val battery: MutableState<Boolean>,
    val bluetooth: MutableState<Boolean>,
    private val refreshBatteryWithRetry: () -> Unit,
    private val bluetoothPermissionLauncher: ManagedActivityResultLauncher<String, Boolean>
) {
    fun onBluetoothClick(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!bluetooth.value) {
                bluetoothPermissionLauncher.launch(
                    Manifest.permission.BLUETOOTH_CONNECT
                )
            } else {
                val intent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:${context.packageName}")
                )
                context.startActivity(intent)
            }
        }
    }

    fun refreshAll(context: Context) {
        overlay.value = context.isOverlayAccessibilityServiceEnabled()
        notification.value = isNotificationListenerEnabled(context)
        refreshBatteryWithRetry()
        bluetooth.value = isBluetoothConnectPermissionGranted(context)
    }
}

@Composable
private fun rememberHomePermissions(context: Context, lifecycleOwner: LifecycleOwner): HomePermissions {
    val overlay = remember { mutableStateOf(context.isOverlayAccessibilityServiceEnabled()) }
    val notification = remember { mutableStateOf(isNotificationListenerEnabled(context)) }
    val battery = remember { mutableStateOf(isBatteryOptimizationIgnored(context)) }
    var overlayGranted by overlay
    var notificationGranted by notification
    var batteryIgnored by battery

    val batteryRefreshHandler = remember { Handler(Looper.getMainLooper()) }

    DisposableEffect(Unit) {
        onDispose {
            batteryRefreshHandler.removeCallbacksAndMessages(null)
        }
    }

    fun refreshBatteryOptimizationWithRetry() {
        batteryRefreshHandler.removeCallbacksAndMessages(null)

        val initialState = isBatteryOptimizationIgnored(context)
        batteryIgnored = initialState

        var attempts = 0

        val refreshRunnable = object : Runnable {
            override fun run() {
                val currentState = isBatteryOptimizationIgnored(context)
                batteryIgnored = currentState
                attempts++

                if (currentState != initialState || attempts >= 8) {
                    return
                }

                batteryRefreshHandler.postDelayed(this, 150L)
            }
        }

        batteryRefreshHandler.postDelayed(refreshRunnable, 150L)
    }

    val bluetooth = remember { mutableStateOf(isBluetoothConnectPermissionGranted(context)) }
    var bluetoothGranted by bluetooth

    val bluetoothPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { granted ->
            bluetoothGranted = granted

            if (granted) {
                SystemServiceRecovery.resetAccessibilityRefreshAttempt()
                SystemServiceRecovery.requestRecovery(context)
            }
        }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                overlayGranted = context.isOverlayAccessibilityServiceEnabled()
                notificationGranted = isNotificationListenerEnabled(context)
                refreshBatteryOptimizationWithRetry()
                bluetoothGranted = isBluetoothConnectPermissionGranted(context)
                SystemServiceRecovery.requestRecovery(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return HomePermissions(
        overlay, notification, battery, bluetooth,
        { refreshBatteryOptimizationWithRetry() },
        bluetoothPermissionLauncher
    )
}

@Preview(showBackground = true, name = "Light Mode")
@Composable
fun SmartIslandHomeScreenLightPreview() {
    SmartIslandTheme(darkTheme = false) {
        SmartIslandHomeScreen()
    }
}

@Preview(showBackground = true, name = "Dark Mode")
@Composable
fun SmartIslandHomeScreenDarkPreview() {
    SmartIslandTheme(darkTheme = true) {
        SmartIslandHomeScreen()
    }
}
