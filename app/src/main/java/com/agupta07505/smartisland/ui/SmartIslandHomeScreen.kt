/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Apps
import com.agupta07505.smartisland.ui.sections.UpdatesAndDownloadsSection
import com.agupta07505.smartisland.ui.sections.SupportSection
import com.agupta07505.smartisland.ui.sections.DeveloperOptionsSection
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Code

import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Gesture
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SettingsBackupRestore
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.FontDownload
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import com.agupta07505.smartisland.ui.glass.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import com.agupta07505.smartisland.ui.glass.LocalGlassDark
import com.agupta07505.smartisland.ui.glass.LocalLiquidGlass
import com.agupta07505.smartisland.ui.glass.ThemedBackdrop
import com.agupta07505.smartisland.ui.glass.glassEdge
import com.agupta07505.smartisland.ui.glass.Switch
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import com.agupta07505.smartisland.ui.components.CollapsibleCard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.data.INotificationRepository
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.SmartIslandSettingsRepository
import com.agupta07505.smartisland.di.SmartIslandRepositories
import com.agupta07505.smartisland.ui.sections.DemoSection
import com.agupta07505.smartisland.ui.sections.EventTogglesSection
import com.agupta07505.smartisland.ui.sections.IslandFontPicker
import com.agupta07505.smartisland.ui.sections.LandscapeSection
import com.agupta07505.smartisland.ui.sections.OrientationTab
import com.agupta07505.smartisland.ui.sections.OrientationTabs
import com.agupta07505.smartisland.ui.sections.AppShortcutsSection
import com.agupta07505.smartisland.ui.sections.AnimationSpeedSection
import com.agupta07505.smartisland.ui.sections.MergeStyleSection
import com.agupta07505.smartisland.ui.sections.IdleWidgetsSection
import com.agupta07505.smartisland.ui.sections.IdleWidgetsSettingsSection
import com.agupta07505.smartisland.ui.sections.WeatherSettingsCard
import com.agupta07505.smartisland.ui.sections.NetSpeedSettingsCard
import com.agupta07505.smartisland.ui.sections.CalendarSettingsCard
import com.agupta07505.smartisland.ui.search.searchAnchor
import com.agupta07505.smartisland.ui.sections.AboutSection
import com.agupta07505.smartisland.ui.sections.BackupRestoreSection
import com.agupta07505.smartisland.ui.sections.CustomizationsSection
import com.agupta07505.smartisland.ui.sections.DiagnosticsSection
import com.agupta07505.smartisland.ui.sections.FaceIdSettingsSection
import com.agupta07505.smartisland.ui.sections.TimingSection
import com.agupta07505.smartisland.ui.sections.GesturesSection
import com.agupta07505.smartisland.ui.sections.NotificationHistorySection
import com.agupta07505.smartisland.ui.sections.NotificationsAndPrivacySection
import com.agupta07505.smartisland.ui.sections.PermissionsSection
import com.agupta07505.smartisland.ui.sections.PositionsSection
import com.agupta07505.smartisland.util.SystemServiceRecovery
import com.agupta07505.smartisland.util.runCatchingLogged
import com.agupta07505.smartisland.ui.search.LocalSearchFocus
import com.agupta07505.smartisland.ui.search.SEARCH_FOCUS_ARMED_MS
import com.agupta07505.smartisland.ui.search.SearchFocusState
import com.agupta07505.smartisland.ui.search.SearchTarget
import com.agupta07505.smartisland.ui.search.SettingsSearch
import com.agupta07505.smartisland.ui.search.SettingsSearchEntry
import com.agupta07505.smartisland.ui.search.SettingsSearchField
import com.agupta07505.smartisland.ui.search.SettingsSearchResults
import com.agupta07505.smartisland.ui.search.buildSettingsSearchIndex
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

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
    IdleWidgets,
    About,
    UpdatesAndDownloads,
    SupportCommunity,
    DeveloperOptions,
    FaceIdSettings,
    WeatherSettings,
    NetSpeedSettings,
    CalendarSettings
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
    /** A section the island asked the app to open, such as the Utilities page's settings button; null when none. */
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
        notificationRepository ?: runCatching {
            SmartIslandRepositories.notificationRepository(context)
        }.getOrNull()
    }

    val settings by resolvedRepository.settings.collectAsStateWithLifecycle(initialValue = SmartIslandSettings.Default)
    val scope = rememberCoroutineScope()

    var showWelcomeDialog by rememberSaveable { mutableStateOf(false) }

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
        com.agupta07505.smartisland.ui.sections.NewUserTour(settings = settings, onFinish = { showTour = false })
    }

    var overlayGranted by remember { mutableStateOf(isAccessibilityServiceEnabled(context)) }
    var notificationGranted by remember { mutableStateOf(isNotificationListenerEnabled(context)) }
    var batteryIgnored by remember { mutableStateOf(isBatteryOptimizationIgnored(context)) }

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

    var bluetoothGranted by remember { mutableStateOf(isBluetoothConnectPermissionGranted(context)) }

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
                overlayGranted = isAccessibilityServiceEnabled(context)
                notificationGranted = isNotificationListenerEnabled(context)
                refreshBatteryOptimizationWithRetry()
                bluetoothGranted = isBluetoothConnectPermissionGranted(context)
                SystemServiceRecovery.requestRecovery(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var selectedTab by rememberSaveable { mutableStateOf(StudioTab.Studio) }
    var orientationTab by rememberSaveable { mutableStateOf(OrientationTab.Portrait) }
    var activeDetailSection by rememberSaveable { mutableStateOf<FeatureDetailSection?>(null) }
    var transitionDirection by rememberSaveable { mutableIntStateOf(1) }

    // Settings search: the query survives rotation; the focus state tells the chosen row to
    // scroll into view and flash once its screen is showing.
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
    // A jump stays armed for a few seconds, then is forgotten so the row never flashes again
    // when the user later opens the same screen by hand.
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
                    orientationTab = if (it) OrientationTab.Landscape else OrientationTab.Portrait
                }
                // Already on the Settings tab: show the normal overview, where the item is.
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

    // The island's settings button: open the App Shortcuts configuration, then forget the request.
    LaunchedEffect(openSection) {
        if (openSection == com.agupta07505.smartisland.util.UtilityActions.SECTION_APP_SHORTCUTS) {
            transitionDirection = 1
            activeDetailSection = FeatureDetailSection.AppShortcuts
            onOpenSectionHandled()
        }
    }

    BackHandler(enabled = activeDetailSection != null) {
        transitionDirection = -1
        // The weather page is opened from the Idle widgets page, so back goes there; every other page goes home.
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
                            transitionDirection = 1
                            activeDetailSection = FeatureDetailSection.PermissionsCenter
                        }
                    )

                    when (selectedTab) {
                        StudioTab.Studio -> {
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
                                    transitionDirection = 1
                                    activeDetailSection = FeatureDetailSection.PermissionsCenter
                                }
                            )

                            // Portrait / Landscape switch, then event toggles for that profile
                            OrientationTabs(
                                selected = orientationTab,
                                onSelected = { orientationTab = it }
                            )
                            EventTogglesSection(
                                settings = settings,
                                repository = resolvedRepository,
                                landscape = orientationTab == OrientationTab.Landscape,
                                onOpenFaceIdSettings = {
                                    transitionDirection = 1
                                    activeDetailSection = FeatureDetailSection.FaceIdSettings
                                }
                            )
                        }

                        StudioTab.Position -> {
                            OrientationTabs(
                                selected = orientationTab,
                                onSelected = { orientationTab = it }
                            )
                            run {
                                if (orientationTab == OrientationTab.Landscape) {
                                    LandscapeSection(
                                        settings = settings,
                                        repository = resolvedRepository
                                    )
                                } else {
                                    PositionsSection(
                                        settings = settings,
                                        repository = resolvedRepository,
                                        onNavigateToBackup = {
                                            transitionDirection = 1
                                            activeDetailSection = FeatureDetailSection.BackupRestore
                                        }
                                    )
                                }
                            }
                            AnimationSpeedSection(
                                settings = settings,
                                repository = resolvedRepository,
                                landscape = orientationTab == OrientationTab.Landscape
                            )
                            MergeStyleSection(
                                settings = settings,
                                repository = resolvedRepository,
                                landscape = orientationTab == OrientationTab.Landscape
                            )
                            IdleWidgetsSection(
                                settings = settings,
                                repository = resolvedRepository,
                                onOpenSettings = {
                                    transitionDirection = 1
                                    activeDetailSection = FeatureDetailSection.IdleWidgets
                                }
                            )
                        }

                        StudioTab.Demo -> {
                            DemoSection(
                                settings = settings,
                                notificationRepository = resolvedNotificationRepository
                            )
                        }

                        StudioTab.Settings -> {
                            SettingsOverviewSection(
                                settings = settings,
                                searchQuery = searchQuery,
                                onSearchQueryChange = { searchQuery = it },
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
                                onNavigateTo = { section ->
                                    transitionDirection = 1
                                    activeDetailSection = section
                                }
                            )
                        }
                    }

                }
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
                        activeDetailSection = if (detailSection in IDLE_SETTINGS_PAGES) FeatureDetailSection.IdleWidgets else null
                    },
                    onBluetoothClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            if (!bluetoothGranted) {
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
                    },
                    onRefreshPermissions = {
                        overlayGranted = isAccessibilityServiceEnabled(context)
                        notificationGranted = isNotificationListenerEnabled(context)
                        refreshBatteryOptimizationWithRetry()
                        bluetoothGranted = isBluetoothConnectPermissionGranted(context)
                    },
                    onNavigateTo = { section ->
                        transitionDirection = 1
                        activeDetailSection = section
                    }
                )
            }
            }
        }
    }
    }
}

private const val MAX_CONTENT_WIDTH_DP = 640

private fun contentSidePadding(screenWidthDp: Int): androidx.compose.ui.unit.Dp {
    val base = when {
        screenWidthDp < 360 -> 14
        screenWidthDp > 600 -> 28
        else -> 20
    }
    val centred = (screenWidthDp - MAX_CONTENT_WIDTH_DP) / 2
    return maxOf(base, centred).dp
}

@Composable
private fun StudioTopHeader(
    isIslandEnabled: Boolean,
    canEnable: Boolean,
    onHealthClick: () -> Unit
) {
    val context = LocalContext.current
    val appIcon = remember(context) {
        runCatchingLogged("HeaderSection", "Failed to get application icon") {
            val drawable = context.packageManager.getApplicationIcon(context.packageName)
            val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 144
            val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 144
            val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
            drawable.setBounds(0, 0, width, height)
            drawable.draw(canvas)
            bitmap.asImageBitmap()
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (appIcon != null) {
                Image(
                    bitmap = appIcon,
                    contentDescription = "Smart Island Logo",
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            brush = Brush.linearGradient(
                                listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("SI", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
            Column {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.app_subtitle),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        val statusColor = when {
            !canEnable -> Color(0xFFE88C25)
            isIslandEnabled -> Color(0xFF0F9F6E)
            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        }
        val statusText = when {
            !canEnable -> stringResource(R.string.status_setup_needed)
            isIslandEnabled -> stringResource(R.string.status_active)
            else -> stringResource(R.string.status_ready)
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(statusColor.copy(alpha = 0.12f))
                .border(1.dp, statusColor.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                .clickable(onClick = onHealthClick)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(statusColor, CircleShape)
                )
                Text(
                    text = statusText,
                    color = statusColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun MasterPowerCard(
    enabled: Boolean,
    canEnable: Boolean,
    overlayGranted: Boolean,
    notificationGranted: Boolean,
    batteryIgnored: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onSetupPermissionsClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.master_switch_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = if (canEnable) {
                        if (enabled) stringResource(R.string.master_switch_active_desc)
                        else stringResource(R.string.master_switch_ready_desc)
                    } else stringResource(R.string.master_switch_missing_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = enabled,
                enabled = canEnable || enabled,
                onCheckedChange = onCheckedChange
            )
        }

        if (!canEnable) {
            // What is still missing, at a glance: one chip per required permission.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SetupChip("Accessibility", overlayGranted, onSetupPermissionsClick)
                SetupChip("Notifications", notificationGranted, onSetupPermissionsClick)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onSetupPermissionsClick)
                    .background(Color(0xFFE88C25).copy(alpha = 0.08f))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Rounded.Warning,
                        contentDescription = null,
                        tint = Color(0xFFE88C25),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = stringResource(R.string.btn_grant_required_permissions),
                        color = Color(0xFFE88C25),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeModeSelector(selected: String, onSelected: (String) -> Unit) {
    val options = listOf("system" to "System", "light" to "Light", "dark" to "Dark")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { (id, label) ->
            val isSelected = selected == id
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent)
                    .clickable { onSelected(id) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SetupChip(label: String, granted: Boolean, onClick: () -> Unit) {
    val color = if (granted) Color(0xFF0F9F6E) else Color(0xFFE88C25)
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = if (granted) "\u2713" else "!",
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
        Text(text = label, color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SettingsOverviewSection(
    settings: SmartIslandSettings,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    searchResults: List<SettingsSearchEntry>,
    onSearchPick: (SettingsSearchEntry) -> Unit,
    overlayGranted: Boolean,
    notificationGranted: Boolean,
    batteryIgnored: Boolean,
    onThemeModeChange: (String) -> Unit,
    onLiquidGlassChange: (Boolean) -> Unit,
    onMaterialYouChange: (Boolean) -> Unit,
    onAccentChange: (Long) -> Unit,
    onIslandFontChange: (String) -> Unit,
    onFontMixChange: (Boolean, String, String, String) -> Unit,
    onNavigateTo: (FeatureDetailSection) -> Unit
) {
    val canEnable = overlayGranted && notificationGranted

    val SystemGroup: @Composable () -> Unit = {
        SettingsCategoryGroup(title = stringResource(R.string.category_system_core)) {
            FeatureStudioNavigationCard(
                title = stringResource(R.string.card_permissions_setup_title),
                subtitle = "Accessibility, notification access, battery",
                icon = Icons.Rounded.Shield,
                iconColor = BadgeGreen,
                statusText = if (!canEnable) stringResource(R.string.status_action_required) else null,
                statusColor = if (canEnable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary,
                onClick = { onNavigateTo(FeatureDetailSection.PermissionsCenter) }
            )

            FeatureStudioNavigationCard(
                title = stringResource(R.string.card_backup_restore_title),
                subtitle = "Export or import all settings",
                icon = Icons.Rounded.SettingsBackupRestore,
                iconColor = BadgeBlue,
                onClick = { onNavigateTo(FeatureDetailSection.BackupRestore) }
            )

            FeatureStudioNavigationCard(
                title = stringResource(R.string.card_updates_downloads_title),
                subtitle = "Check GitHub releases and download updates",
                icon = Icons.Rounded.Download,
                iconColor = BadgeTeal,
                onClick = { onNavigateTo(FeatureDetailSection.UpdatesAndDownloads) }
            )

            FeatureStudioNavigationCard(
                title = stringResource(R.string.card_support_requests_title),
                subtitle = "Report issues, feature requests & community",
                icon = Icons.Rounded.People,
                iconColor = BadgePink,
                onClick = { onNavigateTo(FeatureDetailSection.SupportCommunity) }
            )

            FeatureStudioNavigationCard(
                title = stringResource(R.string.card_developer_options_title),
                subtitle = "Diagnostics, log recording & developer options",
                icon = Icons.Rounded.Code,
                iconColor = BadgeSlate,
                statusText = if (settings.recordLogs) "Recording" else null,
                statusColor = MaterialTheme.colorScheme.primary,
                onClick = { onNavigateTo(FeatureDetailSection.DeveloperOptions) }
            )

            FeatureStudioNavigationCard(
                title = stringResource(R.string.card_about_title),
                subtitle = stringResource(R.string.card_about_subtitle),
                icon = Icons.Rounded.Info,
                iconColor = BadgeSlate,
                onClick = { onNavigateTo(FeatureDetailSection.About) }
            )
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SettingsSearchField(query = searchQuery, onQueryChange = onSearchQueryChange)

        if (searchQuery.isNotBlank()) {
            SettingsSearchResults(query = searchQuery, results = searchResults, onPick = onSearchPick)
        } else {
            // Appearance: the app's own theme, then the island's colours. (Position, size, shadow and the
            // animation choices are on the Layout tab.)
            SettingsCategoryGroup(title = stringResource(R.string.category_appearance_controls)) {
                ThemeModeSelector(selected = settings.themeMode, onSelected = onThemeModeChange)

                com.agupta07505.smartisland.ui.sections.AppThemeOptions(
                    settings = settings,
                    onLiquidGlassChange = onLiquidGlassChange,
                    onMaterialYouChange = onMaterialYouChange,
                    onAccentChange = onAccentChange
                )

                FeatureStudioNavigationCard(
                    title = stringResource(R.string.card_color_studio_title),
                    subtitle = "Colours, opacity, gradients and card styles",
                    icon = Icons.Rounded.Palette,
                    iconColor = BadgeViolet,
                    statusText = stringResource(R.string.card_color_studio_status, (settings.opacity * 100).toInt()),
                    onClick = { onNavigateTo(FeatureDetailSection.ColorStudio) }
                )

                CollapsibleCard(
                    title = stringResource(R.string.island_font_title),
                    stateKey = "settings_appearance_fonts",
                    summary = "Typography and font mixing",
                    icon = Icons.Rounded.FontDownload,
                    initiallyExpanded = false
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        IslandFontPicker(selected = settings.islandFont, onSelected = onIslandFontChange)
                        com.agupta07505.smartisland.ui.sections.IslandFontMixSection(settings = settings, onChange = onFontMixChange)
                    }
                }
            }

            // Features: what shows on the island and which apps may use it.
            SettingsCategoryGroup(title = stringResource(R.string.category_behaviors_launcher)) {
                FeatureStudioNavigationCard(
                    title = stringResource(R.string.card_notifications_privacy_title),
                    subtitle = "Which apps and events show, lock screen privacy",
                    icon = Icons.Rounded.Notifications,
                    statusText = if (settings.showOnLockScreen) stringResource(R.string.card_notifications_privacy_status_lock) else null,
                    onClick = { onNavigateTo(FeatureDetailSection.NotificationRules) }
                )

                FeatureStudioNavigationCard(
                    title = stringResource(R.string.card_notification_history_title),
                    subtitle = "Browse notifications the island has shown",
                    icon = Icons.Rounded.History,
                    iconColor = BadgeIndigo,
                    statusText = if (settings.enableNotificationHistory) stringResource(R.string.card_notification_history_status_active) else null,
                    statusColor = if (settings.enableNotificationHistory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = { onNavigateTo(FeatureDetailSection.NotificationHistory) }
                )

                FeatureStudioNavigationCard(
                    title = stringResource(R.string.card_app_shortcuts_title),
                    subtitle = "Launch favourite apps from the island",
                    icon = Icons.Rounded.Apps,
                    iconColor = BadgeTeal,
                    statusText = if (settings.enableAppShortcuts) {
                        stringResource(R.string.card_app_shortcuts_status, settings.shortcutPackages.size)
                    } else null,
                    statusColor = if (settings.enableAppShortcuts) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = { onNavigateTo(FeatureDetailSection.AppShortcuts) }
                )
            }

            // Interaction: how the island answers your hands, and how long things stay.
            SettingsCategoryGroup(title = stringResource(R.string.category_interaction)) {
                FeatureStudioNavigationCard(
                    title = stringResource(R.string.card_gestures_guide_title),
                    subtitle = "Swipes, taps, long press, sensitivity and vibration",
                    icon = Icons.Rounded.Gesture,
                    iconColor = BadgePink,
                    onClick = { onNavigateTo(FeatureDetailSection.GesturesGuide) }
                )

                FeatureStudioNavigationCard(
                    title = stringResource(R.string.idle_widgets_title),
                    subtitle = stringResource(R.string.card_idle_widgets_subtitle),
                    icon = Icons.Rounded.Widgets,
                    iconColor = BadgeAmber,
                    statusText = if (settings.idleWidgetsEnabled || settings.idleWidgetsEnabledLandscape) {
                        stringResource(R.string.status_active)
                    } else null,
                    statusColor = MaterialTheme.colorScheme.primary,
                    onClick = { onNavigateTo(FeatureDetailSection.IdleWidgets) }
                )

                FeatureStudioNavigationCard(
                    title = stringResource(R.string.card_timing_title),
                    subtitle = stringResource(R.string.card_timing_subtitle),
                    icon = Icons.Rounded.Timer,
                    iconColor = BadgeSky,
                    onClick = { onNavigateTo(FeatureDetailSection.Timing) }
                )
            }

            // System: permissions, backup, developer options, about.
            SystemGroup()
        }
    }
}

@Composable
private fun SettingsCategoryGroup(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 0.8.sp,
            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
        )
        content()
    }
}

private val BadgeIndigo = Color(0xFF6366F1)
private val BadgeTeal = Color(0xFF14B8A6)
private val BadgePink = Color(0xFFEC4899)
private val BadgeAmber = Color(0xFFF59E0B)
private val BadgeSky = Color(0xFF0EA5E9)
private val BadgeViolet = Color(0xFF8B5CF6)
private val BadgeBlue = Color(0xFF3B82F6)
private val BadgeSlate = Color(0xFF64748B)
private val BadgeGreen = Color(0xFF16A34A)

@Composable
private fun FeatureStudioNavigationCard(
    title: String,
    icon: ImageVector,
    iconColor: Color = MaterialTheme.colorScheme.primary,
    statusText: String? = null,
    statusColor: Color = MaterialTheme.colorScheme.primary,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .glassEdge(RoundedCornerShape(14.dp), glass = LocalLiquidGlass.current, dark = LocalGlassDark.current)
            .bounceClick(onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(19.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (statusText != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = statusText,
                color = statusColor,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun StudioBottomNavigationBar(
    selectedTab: StudioTab,
    onTabSelected: (StudioTab) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        NavigationBarItem(
            selected = selectedTab == StudioTab.Studio,
            onClick = { onTabSelected(StudioTab.Studio) },
            icon = { Icon(Icons.Rounded.FlashOn, contentDescription = stringResource(R.string.tab_studio)) },
            label = { Text(stringResource(R.string.tab_studio), fontWeight = FontWeight.SemiBold) },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary
            )
        )
        NavigationBarItem(
            selected = selectedTab == StudioTab.Position,
            onClick = { onTabSelected(StudioTab.Position) },
            icon = { Icon(Icons.Rounded.Tune, contentDescription = stringResource(R.string.tab_position)) },
            label = { Text(stringResource(R.string.tab_position), fontWeight = FontWeight.SemiBold) },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary
            )
        )
        NavigationBarItem(
            selected = selectedTab == StudioTab.Demo,
            onClick = { onTabSelected(StudioTab.Demo) },
            icon = { Icon(Icons.Rounded.Science, contentDescription = stringResource(R.string.tab_demo)) },
            label = { Text(stringResource(R.string.tab_demo), fontWeight = FontWeight.SemiBold) },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary
            )
        )
        NavigationBarItem(
            selected = selectedTab == StudioTab.Settings,
            onClick = { onTabSelected(StudioTab.Settings) },
            icon = { Icon(Icons.Rounded.Settings, contentDescription = stringResource(R.string.tab_settings)) },
            label = { Text(stringResource(R.string.tab_settings), fontWeight = FontWeight.SemiBold) },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@SuppressLint("BatteryLife")
@Composable
private fun DetailScreenHost(
    section: FeatureDetailSection,
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository,
    overlayGranted: Boolean,
    notificationGranted: Boolean,
    batteryIgnored: Boolean,
    bluetoothGranted: Boolean,
    onBack: () -> Unit,
    onBluetoothClick: () -> Unit,
    onRefreshPermissions: () -> Unit,
    onNavigateTo: (FeatureDetailSection) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val isScrollableParent = section != FeatureDetailSection.NotificationHistory
    val scrollModifier = if (isScrollableParent) Modifier.verticalScroll(rememberScrollState()) else Modifier
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val horizontalPad = contentSidePadding(screenWidthDp)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .then(scrollModifier)
            .padding(
                start = horizontalPad,
                end = horizontalPad,
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 12.dp,
                bottom = if (isScrollableParent) 28.dp else 12.dp
            ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            val title = when (section) {
                FeatureDetailSection.NotificationRules -> stringResource(R.string.detail_title_notifications_privacy)
                FeatureDetailSection.AppShortcuts -> stringResource(R.string.detail_title_app_shortcuts)
                FeatureDetailSection.NotificationHistory -> stringResource(R.string.detail_title_notification_history)
                FeatureDetailSection.ColorStudio -> stringResource(R.string.detail_title_color_studio)
                FeatureDetailSection.GesturesGuide -> stringResource(R.string.detail_title_gestures_guide)
                FeatureDetailSection.PermissionsCenter -> stringResource(R.string.detail_title_permissions_center)
                FeatureDetailSection.BackupRestore -> stringResource(R.string.detail_title_backup_restore)
                FeatureDetailSection.Diagnostics -> "Diagnostic log"
                FeatureDetailSection.Timing -> stringResource(R.string.detail_title_timing)
                FeatureDetailSection.IdleWidgets -> stringResource(R.string.idle_widgets_title)
                FeatureDetailSection.About -> stringResource(R.string.detail_title_about)
                FeatureDetailSection.UpdatesAndDownloads -> stringResource(R.string.detail_title_updates_downloads)
                FeatureDetailSection.SupportCommunity -> stringResource(R.string.detail_title_support_community)
                FeatureDetailSection.DeveloperOptions -> stringResource(R.string.detail_title_developer_options)
                FeatureDetailSection.FaceIdSettings -> "Face ID settings"
                FeatureDetailSection.WeatherSettings -> stringResource(R.string.weather_cfg_title)
                FeatureDetailSection.NetSpeedSettings -> stringResource(R.string.net_cfg_title)
                FeatureDetailSection.CalendarSettings -> stringResource(R.string.cal_cfg_title)
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        }

        when (section) {
            FeatureDetailSection.NotificationRules -> {
                NotificationsAndPrivacySection(settings = settings, repository = repository)
            }
            FeatureDetailSection.AppShortcuts -> {
                AppShortcutsSection(settings = settings, repository = repository)
            }
            FeatureDetailSection.NotificationHistory -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    NotificationHistorySection(settings = settings, repository = repository)
                }
            }
            FeatureDetailSection.ColorStudio -> {
                CustomizationsSection(settings = settings, repository = repository)
            }
            FeatureDetailSection.GesturesGuide -> {
                GesturesSection(settings = settings, repository = repository)
            }
            FeatureDetailSection.PermissionsCenter -> {
                PermissionsSection(
                    overlayGranted = overlayGranted,
                    notificationGranted = notificationGranted,
                    batteryIgnored = batteryIgnored,
                    bluetoothGranted = bluetoothGranted,
                    onOverlayClick = {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    },
                    onNotificationClick = {
                        val detailIntent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS").apply {
                            val component = ComponentName(context, com.agupta07505.smartisland.service.SmartIslandNotificationListenerService::class.java)
                            putExtra("android.provider.extra.NOTIFICATION_LISTENER_COMPONENT_NAME", component.flattenToString())
                        }
                        runCatching {
                            context.startActivity(detailIntent)
                        }.onFailure {
                            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        }
                    },
                    onBatteryClick = {
                        runCatching {
                            context.startActivity(
                                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                                    .setData(Uri.parse("package:${context.packageName}"))
                            )
                        }.onFailure {
                            context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                        }
                    },
                    onBluetoothClick = onBluetoothClick,
                    onRefreshPermissions = onRefreshPermissions
                )
            }
            FeatureDetailSection.BackupRestore -> {
                BackupRestoreSection(settings = settings, repository = repository)
            }
            FeatureDetailSection.Diagnostics -> {
                DeveloperOptionsSection(settings = settings, repository = repository)
            }
            FeatureDetailSection.Timing -> {
                TimingSection(settings = settings, repository = repository)
            }
            FeatureDetailSection.IdleWidgets -> {
                IdleWidgetsSettingsSection(
                    settings = settings,
                    repository = repository,
                    onOpenWeatherSettings = { onNavigateTo(FeatureDetailSection.WeatherSettings) },
                    onOpenNetSpeedSettings = { onNavigateTo(FeatureDetailSection.NetSpeedSettings) },
                    onOpenCalendarSettings = { onNavigateTo(FeatureDetailSection.CalendarSettings) }
                )
            }
            FeatureDetailSection.About -> {
                AboutSection(settings = settings, repository = repository)
            }
            FeatureDetailSection.UpdatesAndDownloads -> {
                UpdatesAndDownloadsSection(settings = settings, repository = repository)
            }
            FeatureDetailSection.SupportCommunity -> {
                SupportSection()
            }
            FeatureDetailSection.DeveloperOptions -> {
                DeveloperOptionsSection(settings = settings, repository = repository)
            }
            FeatureDetailSection.FaceIdSettings -> {
                FaceIdSettingsSection(settings = settings, repository = repository)
            }
            FeatureDetailSection.WeatherSettings -> {
                WeatherSettingsCard(settings = settings, repository = repository, modifier = Modifier.searchAnchor("idle.weather"))
            }
            FeatureDetailSection.NetSpeedSettings -> {
                NetSpeedSettingsCard(settings = settings, repository = repository, modifier = Modifier.searchAnchor("idle.netspeed"))
            }
            FeatureDetailSection.CalendarSettings -> {
                CalendarSettingsCard(modifier = Modifier.searchAnchor("idle.calendar"))
            }
        }
    }
}

private fun isNotificationListenerEnabled(context: Context): Boolean {
    val enabled = Settings.Secure.getString(
        context.contentResolver,
        "enabled_notification_listeners"
    )
    return enabled?.split(":")?.any {
        ComponentName.unflattenFromString(it)?.packageName == context.packageName
    } == true
}

private fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val expectedComponentName = ComponentName(context, com.agupta07505.smartisland.service.SmartIslandOverlayService::class.java)
    val enabledServicesSetting = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false
    val colonSplitter = android.text.TextUtils.SimpleStringSplitter(':')
    colonSplitter.setString(enabledServicesSetting)
    while (colonSplitter.hasNext()) {
        val componentNameString = colonSplitter.next()
        val enabledService = ComponentName.unflattenFromString(componentNameString)
        if (enabledService != null && enabledService == expectedComponentName) {
            return true
        }
    }
    return false
}

private fun isBatteryOptimizationIgnored(context: Context): Boolean {
    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M) return true
    val pm = context.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
    return pm?.isIgnoringBatteryOptimizations(context.packageName) ?: true
}

@Composable
private fun WelcomeDialog(
    onDismiss: () -> Unit,
    onTakeTour: () -> Unit
) {
    val context = LocalContext.current
    val appIcon = remember(context) {
        runCatchingLogged("WelcomeDialog", "Failed to get app icon") {
            val drawable = context.packageManager.getApplicationIcon(context.packageName)
            val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 144
            val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 144
            val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
            drawable.setBounds(0, 0, width, height)
            drawable.draw(canvas)
            bitmap.asImageBitmap()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp),
            shape = RoundedCornerShape(26.dp),
            // Fully opaque: the glass theme makes `surface` translucent, which left this dialog hard to read.
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 1f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (appIcon != null) {
                    Image(
                        bitmap = appIcon,
                        contentDescription = "Smart Island Logo",
                        modifier = Modifier
                            .size(68.dp)
                            .clip(RoundedCornerShape(18.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(18.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("SI", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }

                Text(
                    text = stringResource(R.string.welcome_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = stringResource(R.string.welcome_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = {
                        runCatchingLogged("WelcomeDialog", "Failed to open GitHub link") {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/agupta07505/SmartIsland")))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSecondary
                        )
                        Text(
                            stringResource(R.string.star_on_github),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondary
                        )
                    }
                }

                OutlinedButton(
                    onClick = {
                        runCatchingLogged("WelcomeDialog", "Failed to open Telegram link") {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://telegram.me/SmartIslandApp")))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.People,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            stringResource(R.string.join_telegram),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                ElevatedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(stringResource(R.string.btn_get_started), fontWeight = FontWeight.Bold)
                }

                TextButton(onClick = onTakeTour, modifier = Modifier.fillMaxWidth()) {
                    Text("Take a quick tour of the gestures", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

private fun isBluetoothConnectPermissionGranted(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        return true
    }

    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.BLUETOOTH_CONNECT
    ) == PackageManager.PERMISSION_GRANTED
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
