# Changelog

All notable changes to Smart Island should be documented in this file.

The format is inspired by Keep a Changelog, and this project uses the GNU General Public License v3.0.

## [7.6.0] - 2026-10-19

### Highlights (v7.6.0)
- **Multilingual Support**: Added Spanish and Japanese translations; expanded Brazilian Portuguese and Simplified Chinese.
- **Translation Automation**: Integrated string extraction and translation script with local LibreTranslate support.
- **Updated Screenshots**: Refreshed showcase screenshots across README.
- **System Events & UI Polish**: Audio jack and USB connection refinements, welcome card opacity adjustments, and overlay stability fixes.

## [7.5.0] - 2026-10-18

### Major Release Highlights (v7.5.0)

Smart Island **v7.5.0** adds idle widgets and a full landscape mode, and reworks large parts of the card, gesture and theme system:
- **Idle Widgets**: the cutout now rotates through live widgets (clock, date, weather, wind, battery, next alarm, net speed, unread count, Do Not Disturb) when no event is on the island.
- **Weather, Net Speed & Calendar**: location-aware weather with a custom provider option, a net speed widget with daily usage, and the next calendar event on the date widget.
- **Face Unlock Animation**: a face scan and fingerprint animation drawn over the cutout when the phone is unlocked.
- **Per-Event Control**: about 30 events, each with its own switch, auto-expand and lock screen setting, plus new lock screen presets.
- **Quick Launch & Utilities**: pinned apps, recents and a 2x4 utilities grid with resizable app widgets.
- **Landscape Overhaul**: independent portrait and landscape profiles, a landscape lab and a rotation veil.
- **Material You & Liquid Glass**: Material 3 colours from your wallpaper or one accent, an optional glass theme, bundled and custom fonts.

### Added

- **Idle Widgets (`IdleWidget.kt`, `IdleWidgets.kt`, `IdleWidgetsSettingsSection.kt`)**:
  - Widgets that take turns on the cutout: Live Clock, Date, Weather, Wind, Battery, Next Alarm, Net Speed, Notifications count and Do Not Disturb / Modes.
  - Rotation interval of 3 to 60 seconds, temperature units (auto, Celsius, Fahrenheit) and a semi-expanded widget island that opens on tap.
  - **Hour tick**: once an hour the pill ticks over to the new time.
  - **Day Moments**: sunrise, afternoon, sunset and midnight glyphs at fixed local times.
  - **Schedules**: choose which widgets show during work hours (weekdays only option), at night or while charging, drag them into your own order and pin one to always lead.
  - **Tick styles**: smooth, or an LED board with hop patterns, speed, stagger and red, amber, green or white colours, with ambient LED motions for sun, rain and wind.
- **Weather Widget (`WeatherConfig.kt`, `WeatherDisplay.kt`, `WeatherSettingsCard.kt`)**:
  - Location from auto detect, a chosen city or the last known place.
  - Open-Meteo by default, or your own compatible https service with an optional API key that is never part of a backup.
  - Refresh interval or manual only, and island rows for high and low, rain chance, humidity, UV, sun times, air quality and hourly forecast.
- **Net Speed Widget (`NetSpeedRules.kt`, `NetUsage.kt`, `NetSpeedSettingsCard.kt`)**: shows the busier direction above a threshold you set, with hysteresis so it does not flicker, can be limited to Wi-Fi or mobile data, and keeps a daily Wi-Fi and mobile usage counter that restarts at midnight.
- **Calendar Next Event (`CalendarRules.kt`, `CalendarReader.kt`)**: the date widget lists your next event within 24 hours.
- **Face Unlock Animation (`FaceIdLayer.kt`, `FaceIdFingerprint.kt`, `FaceUnlockSection.kt`)**: a scan animation over the cutout on face unlock, a fingerprint version for finger unlocks, app lock prompts (including MIUI app lock), and a choice between face unlock only or any unlock.
- **Per-Event Switches (`IslandEvent.kt`, `EventTogglesSection.kt`)**:
  - 32 events grouped as Power, Connectivity and Activities, each with its own on/off, auto-expand and hide-on-lock-screen switch.
  - New events include Wi-Fi and VPN disconnect, USB in and out, wired headphones in and out, audio recording, backups, file copy and move, file delete and app install.
  - **Lock screen presets**: Show all, Messages only, Calls only or Off.
- **Quick Launch Window (`LauncherPager.kt`, `UtilityGrid.kt`, `UtilitiesSection.kt`, `PinnedApps.kt`)**:
  - Apps and Recents pages, plus a Utilities page on a second level.
  - Utilities include mobile data, Wi-Fi, hotspot, flashlight, calculator, clear cache, screen recorder, screenshot, timer, stopwatch, lock screen, voice recorder, camera, Bluetooth, airplane mode, auto rotate, dark mode and battery saver.
  - Resizable app widgets on a 2x4 grid with two tabs.
  - Pinned apps picker in its own see-through window so choosing never opens the app.
- **Floating Window Engines (`FloatingWindowMode.kt`, `LmoFreeform.kt`)**: Android freeform or LMO Freeform, with a pull-down handle on the expanded island to open the current notification in a window.
- **Landscape Overhaul (`OrientationProfile.kt`, `LandscapeLayout.kt`, `LandscapeLabSection.kt`)**:
  - Fully independent portrait and landscape profiles for geometry, events and animation speed.
  - Landscape lab that plays the demo script in each situation, a corner pill and a split island with a beside picker or picture island.
  - **Rotation veil** that hides the island until the new orientation has settled.
- **Notification Cards**:
  - Half-typed reply drafts that survive collapses and interruptions.
  - **Sibling picker** and secondary cutouts for several cards from one app.
  - Stepped card depth: expandable text, a reader and a picture selector for pictures and stickers.
  - Same-app card switching and silent notifications skipped when cycling events.
- **Gestures (`GestureGuide.kt`, `GestureSensitivityRow.kt`, `HapticStrengthRow.kt`)**: new actions for next stored event, next card of the same app and long press to hide or restore the pill, plus gesture sensitivity and haptic strength settings and calmer, spring-based swipe feel.
- **Media & Call Controls**: a redesigned media card with an audio output switcher chip, artwork tint colour and the app's own extra buttons, and call cards that can mute the mic and switch earpiece, speaker or Bluetooth.
- **Look & Feel (`Theme.kt`, `LiquidGlass.kt`, `AppThemeOptions.kt`)**:
  - Material 3 colour scheme built from one accent colour, or from the wallpaper with Material You on Android 12 and up.
  - Optional **liquid glass** theme with translucent panels over a colour backdrop.
  - Bundled **Lora, Inter Display and Poppins** fonts, your own `.ttf` and `.otf` fonts, and a **mix fonts** mode using up to three fonts by weight.
  - **Gooey bubble merge** style (Android 12+) for the secondary cutout.
  - Animation speed and timing sections to tune the island.
- **Settings Search & Exempt Apps**: in-settings search for every toggle, plus per-orientation lists of apps that switch the island off (or, in portrait, make the pill static) while in the foreground.
- **Shizuku & Diagnostics (`ShizukuManager.kt`, `DiagnosticsSection.kt`, `AppLogRecorder.kt`)**: broader Shizuku detection (forks, Sui, hidden packages), one-tap grant of the sensitive notification permission on Android 15+, hotspot start and stop through shell, a diagnostic log for missed calls, an environment snapshot, a main thread watchdog and a log that survives the process being killed.
- **Baseline Profile Module**: bundled `:baselineprofile` module and CI workflow for ahead-of-time compiled startup and animation paths.
- **New Permissions**: `READ_CALENDAR`, `ACCESS_COARSE_LOCATION`, `ACCESS_FINE_LOCATION`, `ACCESS_NOTIFICATION_POLICY`, `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE`, `WRITE_SETTINGS`, `KILL_BACKGROUND_PROCESSES`, `SET_ALARM` and the optional `RECEIVE_SENSITIVE_NOTIFICATIONS`.
- **Tests & Docs**: dozens of new unit tests across the new model code, plus `NOTICE.md` and `THIRD_PARTY_FONTS.md` for bundled font licences.

### Improved & Fixed

- **Timer and Stopwatch Detection (`TimerStopwatchParser.kt`)**: alarms with snooze are never read as timers, paused stopwatches stay stopwatches, chat messages mentioning a timer or lap are ignored, and countdown deadlines use the most accurate source with HyperOS fallbacks.
- **Navigation Parsing (`NavigationParser.kt`)**: delivery and ride apps never show as turn-by-turn navigation, only known map apps are trusted for the navigation category, and the card is split into distance, maneuver, hint and trip stats.
- **Notification Filtering (`NotificationFilter.kt`)**: WhatsApp calls that carry the group-summary flag, CallStyle calls with an empty title, chat and inbox styles with empty text, wearable connection spam and sensitive system settings are all handled, and a suppressed notification now logs the rule that blocked it.
- **Anti-Spam (`NotificationCooldownManager.kt`)**: finished download notifications are never held back, progress updates no longer count as spam, and system events such as a charger plugged in again and again or Wi-Fi that keeps reconnecting can be cooled down on their own.
- **Transfers and Recorders**: per-app workarounds for apps whose transfers do not look like downloads, a voice recorder's cloud backup is classified as a transfer, and audio recorder apps get their own mode with paused recordings still counted.
- **Idle Cutout Behaviour**: cards that have nothing actively running return to the idle cutout after a timeout, and bursts of cards pulse the cutout once.
- **Shade and Foreground Detection**: accessibility window events now track the notification shade, quick settings, lock screen and the foreground app, so the island hides and reappears at the right moments.
- **Hotspot Card (`HotspotUtil.kt`)**: reads the platform flag where the ROM allows it and falls back to the island's own card state.
- **Camera Cutout Detection (`CameraCutoutDetector.kt`)**: portrait detection now respects the manual area and separates the detected hole from pill padding.

## [7.0.0] - 2026-09-20

### Major Release Highlights (v7.0.0)

Smart Island **v7.0.0** is a landmark major release delivering comprehensive improvements across gesture controls, layout customization, device safety, app updates, and design aesthetics:
- **In-Pill Swipe Gestures & Media Controls**: Compact collapsed pill now supports independent left/right/up/down gestures, enabling next/prev song skipping or notification cycling directly on the pill without expanding.
- **Dedicated App Updates & Downloads Hub**: New dedicated settings section with live Total Downloads statistics (15,648+), in-app GitHub update checker, version insights, top community contributors, and recent commits feed.
- **Selective App Alerts & Sound Manager**: Redesigned lightweight app alert manager modal that prevents UI stutter by selectively loading apps on-demand, allowing granular notification, sound, and exclusion controls.
- **Unified Deep Burnt-Orange (`#D84315`) Design System**: Completely renovated settings aesthetics with cohesive dark slate surfaces, burnt-orange accenting, crisp typography, and unified icon tinting (eliminating rainbow clutter).
- **Configurable Companion Circle Placement**: Position the multi-tasking companion circle on either the Left or Right of the main pill with anti-overlap layout geometry clamping.
- **iPhone Notch Mode**: Authentic top-docked notch mode (`y = 0`) with curved bottom corners and suppression of the companion split bubble.
- **Developer Options & System Diagnostics**: 7-tap version unlock, in-memory logcat ring buffer (3,000 entries), and one-tap `.txt` report export via Storage Access Framework.
- **Full Settings Backup & Restore System**: Atomic JSON backup and restore via Android SAF with bounds validation and factory reset.
- **13 High-Resolution Screenshots**: Completely refreshed visual documentation capturing every new UI section and feature.

### Added

- **In-Pill Swipe Gestures & Media Control Engine (`IslandOverlayView.kt`, `GesturesSection.kt`, `SmartIslandSettings.kt`, `SmartIslandSettingsRepository.kt`, `SwipeAction.kt`, `ExpandedActions.kt`)**:
  - **Independent Pill Gesture Master Switch**: Toggle `enablePillSwipeActions` enables or disables swipe gestures on the compact collapsed pill independently from expanded card gestures, preventing accidental swipes while keeping tap-to-expand intact.
  - **Horizontal Swipe Notification Cycling**: Swiping left or right across the collapsed pill cycles through active notifications in the stack.
  - **Direct In-Pill Media Track Skip**: Configurable option to map Swipe Left / Swipe Right on the collapsed pill directly to `skipToPrevious` and `skipToNext` for music players (Spotify, YouTube Music, Apple Music, podcasts, system players) without expanding the card.
  - **4-Directional In-Pill Customization**:
    - **Swipe Left on Pill**: Previous Notification, Next Notification, Previous Track (`skipToPrevious`), Next Track (`skipToNext`), Play/Pause, Dismiss Current, Open App, Floating Window, Notification Shade, or **Disabled**.
    - **Swipe Right on Pill**: Next Notification, Previous Notification, Next Track (`skipToNext`), Previous Track (`skipToPrevious`), Play/Pause, Dismiss Current, Open App, Floating Window, Notification Shade, or **Disabled**.
    - **Swipe Up on Pill**: Dismiss Current Notification, Dismiss All Notifications, Open Notification Shade, or **Disabled**.
    - **Swipe Down on Pill**: Expand Island, Open Notification Shade, Open App, Floating Window, Dismiss Current, or **Disabled**.
  - **Individual Gesture Disabling**: Any swipe direction on either the pill or expanded card can be set to **"Disabled"** (`SwipeAction.None`).
  - **Touch Event Resolution & Velocity Tracking**: Refined `pointerInput` and `detectDragGestures` in `IslandOverlayView.kt` ensuring small-amplitude swipes on compact notch dimensions register reliably without conflicting with click-to-expand.

- **Dedicated App Updates & Downloads Hub (`AppUpdatesSection.kt`, `SmartIslandHomeScreen.kt`, `GitHubApiService.kt`)**:
  - **Separation into Dedicated Hub**: Separated update checking, download metrics, and GitHub community insights from the About screen into its own clean section.
  - **Total Downloads Counter Banner**: Prominently displays live total downloads metric (15,648+ downloads) at the very top of the hub.
  - **Clean Horizontal Layout Hierarchy**:
    - Line 1: **Current Version** status badge (v7.0.0).
    - Line 2: **Check for updates** action button triggering real-time GitHub Release API query.
    - Line 3: **Changelogs** modal button opening complete in-app version release notes.
  - **Top Contributors Gallery**: Fetches and renders top GitHub repository contributors with avatar thumbnails, contribution counts, and direct profile links.
  - **Recent Commits Feed**: Dynamic feed showing latest repository commit messages, authors, and timestamps.
  - **Responsive Horizontal Banner**: Updated Settings Overview card layout to display horizontally in parity with Backup & Restore, About, and Developer Options.

- **Selective App Alerts & Sound Manager Modal (`NotificationsAndPrivacySection.kt`, `AppNotificationSoundManager.kt`)**:
  - **On-Demand Selective Modal**: Replaced the previous monolithic package list that loaded hundreds of installed apps upfront with a fast, responsive modal dialog triggered via **"Select Apps to Manage"**.
  - **Instant Search & Filter**: Real-time package/label search bar with zero UI frame drops.
  - **Granular Per-App Controls**:
    - **Notification Toggle**: Enable or mute visual island alerts for the selected application.
    - **Alert Sound Toggle**: Selectively enable or disable custom chime audio for specific applications.
    - **Exclude from Smart Island**: Completely exclude high-traffic or private apps from surfacing on the island.
  - **Configured Apps Summary**: Displays list and count of explicitly customized applications with 1-tap removal.

- **Unified Deep Burnt-Orange (`#D84315`) Design System (`Color.kt`, `Theme.kt`, `SmartIslandHomeScreen.kt`, all UI sections)**:
  - **Curated Palette**: Replaced cluttered rainbow colors with a high-contrast, visually pleasing burnt-orange palette (`#D84315`, `#BF360C`, `#FF7043`, `#FFAB91`) paired with deep dark surfaces (`#121212`, `#1E1E1E`).
  - **Harmonious Icon Tinting**: Unified tinted Material icons across all settings category cards, action badges, dialogs, and sliders, providing a cohesive, state-of-the-art visual experience.
  - **Refined Card Typography**: High-contrast white headers (`#FFFFFF`) with subtle slate secondary text (`#B0BEC5`) for effortless readability.

- **Configurable Companion Circle Position (`PositionsSection.kt`, `IslandOverlayView.kt`, `SmartIslandOverlayService.kt`, `SmartIslandSettings.kt`, `SmartIslandSettingsRepository.kt`)**:
  - **Left or Right Placement Option**: Added `circlePosition` setting (`"right"` vs `"left"`, default `"right"`) allowing users to position the multi-tasking companion circle on either the left or right side of the main pill.
  - **Optimized for Corner Displays**: Avoids edge crowding and collisions on devices with right-aligned punch-hole cameras (Galaxy S10, Huawei, Honor, etc.) by anchoring the companion circle on the left.
  - **Dedicated UI Card in Positions & Sizing**: Segmented selector with visual direction icons (`AlignHorizontalLeft` and `AlignHorizontalRight`).
  - **Symmetric Anti-Collapse Geometry Clamping**: Unified layout math ensuring the companion circle and pill maintain at least `compactGap` (8dp) separation across all screen widths and orientations.

- **iPhone Notch Mode & Companion Circle Elimination (`PositionsSection.kt`, `IslandOverlayView.kt`, `SmartIslandOverlayService.kt`, `SmartIslandSettings.kt`)**:
  - **Authentic iPhone Notch Docking**: Anchors the island flush against the top display edge (`y = 0`), matching iPhone X through iPhone 14 hardware notches.
  - **Rounded Bottom Corners**: Seamless zero-radius top corners (`topStart = 0.dp, topEnd = 0.dp`) and curved bottom corners (`bottomStart = cornerRadius.dp, bottomEnd = cornerRadius.dp`).
  - **Companion Circle Elimination**: Suppresses split companion circle when two or more activities are active, keeping a unified notch footprint.
  - **Dedicated Card & Preset**: 1-tap **"Apply Recommended iPhone Notch Size"** (175x35dp, y = 0) in Positions & Sizing.

- **Developer Mode & In-App Log Recording & Diagnostic Export (`DeveloperOptionsSection.kt`, `AppLogRecorder.kt`, `AboutSection.kt`, `SmartIslandSettings.kt`)**:
  - **7-Tap Version Unlock**: Android easter-egg style unlock via 7 taps on App Version in About section.
  - **In-Memory Ring Buffer (`AppLogRecorder.kt`)**: Thread-safe buffer capturing up to 3,000 diagnostic events, service lifecycles, and exceptions.
  - **Real-Time Process Logcat Capture**: Extracts app process logcat trace (`logcat -d -v threadtime --pid=$pid`) without elevated permissions.
  - **Scoped Storage SAF Export**: One-tap export saving diagnostic report (`smartisland_logs_<timestamp>.txt`) via Storage Access Framework.
  - **1-Tap Share, Copy & Clear**: System share sheet (`ACTION_SEND`), clipboard copying, and live buffer clearing.

- **Full Settings Backup & Restore System (`BackupRestoreSection.kt`, `SmartIslandSettings.kt`, `SmartIslandSettingsRepository.kt`)**:
  - **Scoped Storage JSON Export (`CreateDocument`)**: Portable JSON export of geometry, dimensions, offsets, corner radii, opacity, shadow elevation, Color Studio palette, App Shortcuts, and Privacy rules.
  - **Scoped Storage JSON Import (`OpenDocument`)**: Validated configuration import with metadata inspection, dimension bounds clamping, and confirmation dialogs.
  - **Atomic Single-Transaction Restore**: Applies 40+ preferences atomically in DataStore without UI flicker.
  - **Factory Reset**: 1-tap reset to default settings with confirmation dialog.

- **Bluetooth Device Battery & Earbuds Alternating Animation (`IslandCollapsedContent.kt`, `BluetoothExpanded.kt`, `SystemEventReceiver.kt`, `SmartIslandSettings.kt`)**:
  - **Dual-Path Battery Level Extraction**: Broadcast extras (`BATTERY_LEVEL`) with graceful reflection fallback on `device.getBatteryLevel()`.
  - **Fluid Spring Transition**: 3-second cadence alternating between live battery gauge and earbuds icon using spring physics (`stiffness = 520f`, `dampingRatio = 0.72f`).

- **Intelligent Notification Cooldown & Anti-Spam Engine (`NotificationCooldownManager.kt`, `SmartIslandNotificationListenerService.kt`, `NotificationsAndPrivacySection.kt`, `SmartIslandSettings.kt`)**:
  - **Sliding Window Burst Detection**: 30-second window rate limiter (e.g. 3 alerts in 30s) cooling down noisy apps.
  - **Delayed Delivery**: Holds rapid successive spam while buffering the latest alert, surfacing it cleanly after the quiet interval.
  - **Per-App Whitelist Exclusion**: Whitelist critical messaging/emergency apps from being throttled.

- **13 High-Resolution Screenshots & Visual Documentation Refresh**:
  - Added 13 new high-resolution screenshots in `assets/screenshots/` showcasing the Home Try It Out lab, Layout Controls, OEM Rules, Live Activities & App Alerts Manager, App Shortcuts Launcher, Virtualized History Log, Appearance & Colors Studio, Gesture Guide & In-Pill Playground, Permissions Setup Center, Backup & Restore, App Updates Hub, Expanded Navigation, and Developer Diagnostics.

### Fixed & Improved

- **In-Pill Swipe Gesture Event Interception (`IslandOverlayView.kt`)**:
  - Fixed gesture conflict where compact pill click listeners were preempting drag gestures, ensuring horizontal and vertical swipes trigger immediately and accurately.
- **App Updates & Downloads Layout Alignment (`SmartIslandHomeScreen.kt`)**:
  - Fixed vertical stacking bug in Settings Overview; App Updates & Downloads now renders as a clean horizontal card aligned with Backup & Restore and About.
- **Service Connect / Rebind Stale Notification Flooding (`SmartIslandNotificationListenerService.kt`, `NotificationFilter.kt`)**:
  - On `onListenerConnected()`, only active ongoing notifications (Music, Calls, Timers, Stopwatches, Navigation, Live Activities, Downloads, Hotspot, Recording) are restored; stale standard notifications from the shade are prevented from dumping into the island.
  - Enforced a 1200ms debounce on alert sound playback and 1500ms debounce on auto-expansion during bursts.
  - Implemented per-package standard notification coalescing so older messages from the same chat app are replaced rather than creating dozens of pages.
  - Unconditionally suppressed background sync / polling notifications ("Checking for messages...").
- **Alarm Classification & Timer Pause Intent Resolution (`TimerStopwatchParser.kt`, `NotificationFilter.kt`, `TimerExpanded.kt`, `StopwatchExpanded.kt`)**:
  - Added `isAlarm()` check classifying clock alarms as `IslandMode.Notification`, preventing them from hijacking timer/stopwatch UI.
  - Fixed pause intent resolution to prioritize pause/freeze over stop/cancel, preserving paused timer states.
- **Bluetooth Watch & Wearable Connection Spam Fix (`SystemEventReceiver.kt`, `NotificationFilter.kt`)**:
  - Filtered out smartwatches, fitness trackers, and BLE peripherals from triggering audio Bluetooth popups.
  - Added 30-second debounce per Bluetooth MAC address, preventing reconnect loops (e.g. Galaxy Watch on OneUI) from auto-expanding the island repeatedly.
- **Do Not Disturb (DND) Compliance (`SmartIslandNotificationListenerService.kt`)**:
  - Automatically respects system DND suppression flags (`getSuppressionFilterFlags()`) and purges suppressed alerts during Priority, Alarms-only, or Total Silence modes.
- **Nubia / RedMagic Hardware Optimization (`OemDeviceRules.kt`)**:
  - Added dedicated under-display camera and punch-hole layout rules for Nubia and RedMagic devices.
- **100% Localization Parity (`values-zh`, `values-zh-rCN`, `values-pt`, `values-pt-rBR`)**:
  - Added translations for all v7.0 strings across English, Chinese (Simplified & Regional), and Portuguese (Standard & Brazil).
- **Version Bump**: Updated application version to **`7.0.0`** (`versionCode 8`).

## [6.0.0] - 2026-08-29

### Added

- **Ultra-Fluid, High-Speed Spring Animation Engine (`IslandOverlayView.kt`, `BounceClick.kt`, `SmartIslandOverlayService.kt`)**:
  - **Unified High-Fluidity Spring Physics**: Synchronized width and height morphing to calibrated spring curves (`stiffness = 520f`, `dampingRatio = 0.72f` / `0.76f`), completely eliminating the asymmetric height snap.
  - **Snappy 190ms Cross-Fades**: Reduced alpha transition duration to a responsive 190ms with smooth cubic bezier easing for instant tactile feedback.
  - **Cohesive Content Morphing**: Refined entry scaling (`0.95f → 1f`) and slide (`-6.dp → 0.dp`) for natural, seamless card expansion and collapsing.
  - **Instant Overlay Window Cleanup**: Reduced overlay collapse debounce from 500ms to 220ms in `SmartIslandOverlayService.kt` so touches behind the collapsed island resume without delay.
  - **Organic Multi-Notification Split Bubble Pop**: Secondary and tertiary companion bubbles spring out with a natural pop (`stiffness = 480f`, `dampingRatio = 0.68f`) and synchronized offset morphing (`520f`).
  - **Tactile Notification Cycling Rebound**: Upgraded `switchScaleAnim` on cycling notifications with fast 40ms compression (`0.92f`) and immediate spring rebound (`stiffness = 650f`, `dampingRatio = 0.62f`).
  - **Tactile Spring Bounce Controls**: Enhanced `.bounceClick` with spring scale physics across all buttons, playback controls, reply buttons, quick action chips, and cards.
- **Tap Anywhere on Expanded Cards to Open Target App (`IslandExpandedContent.kt`, `IslandOverlayView.kt`, `SmartIslandOverlayService.kt`)**:
  - Tapping anywhere on neutral areas of any expanded card (e.g. background, artwork, song/artist title, notification body, message text, header, padding) outside interactive buttons opens that notification's application (or system settings for Bluetooth/Battery/Hotspot/Alarms) and smoothly collapses the island.
  - Interactive child elements (Play/Pause, Skip, Seekbar, Reply input, Action chips) retain isolated click handling.
  - Preserves ongoing music notifications when launching music apps (Spotify, YouTube Music, etc.).
- **Auto-Hide Pill Inactivity Timer with Custom Seconds & Tap-to-Reveal (`NotificationsAndPrivacySection.kt`, `SmartIslandSettingsRepository.kt`, `IslandOverlayView.kt`)**:
  - New setting **"Auto-Hide Pill After Inactivity"** in the Notifications & Privacy section.
  - User-configurable timeout (1s to 60s) with 1-tap quick presets (3s, 5s, 10s, 15s, 30s) and a custom duration slider with micro `+`/`-` buttons.
  - When collapsed, if left inactive for the configured duration, the pill and companion circle smoothly shrink to 0 size / 0 alpha even if notifications or music are active.
  - **First Tap (Awaken)**: Tapping the invisible touch target over the cutout/island location awakens and smoothly restores the pill (and companion circle).
  - **Second Tap (Expand)**: Tapping the visible pill expands into the full SmartIsland card.
  - Any new incoming notification or user interaction immediately awakens the pill and resets the timer.
- **Landscape Mode Visibility Toggle (`NotificationsAndPrivacySection.kt`, `SmartIslandSettingsRepository.kt`, `SmartIslandOverlayService.kt`)**:
  - New setting **"Show in Landscape Mode"** in the Display & Expansion section.
  - Allows the island to remain active, visible, and dynamically repositioned when rotating the device to landscape orientation.
- **Full Chinese & Portuguese Localization Parity (`values-zh`, `values-zh-rCN`, `values-pt`, `values-pt-rBR`)**:
  - Added complete 100% translation coverage for all new settings, toggles, labels, and descriptions.

### Fixed & Improved

- **Animation Flicker & Stutter Elimination**:
  - Resolved height jumping during expand/collapse by introducing `defaultEstimatedHeightForMode()` in `IslandOverlayView.kt` and `IslandExpandedContent.kt`.
  - Prevented sudden card disappearance on collapse by changing condition to `if (expanded || expandedAlpha > 0.01f)`.
  - Fixed premature window cropping during collapse with `isWindowExpanded` lifecycle state and debounced `collapseJob`.
  - Added `FLAG_HARDWARE_ACCELERATED` and `lastParams` caching to prevent redundant `windowManager.updateViewLayout` calls and layout churn.
  - Switched `AudioVisualizer` in `IslandCollapsedContent.kt` to GPU-accelerated `graphicsLayer { scaleY = ... }` and `WavyMusicSeekBar.kt` to vsync-synchronized `rememberInfiniteTransition`.
- **Camera Cutout Opacity Uniformity Fix (`IslandCollapsedContent.kt`)**:
  - Eliminated redundant center dot `Box` that was causing double alpha blending and a darker circle in the center when opacity was reduced.
- **Version Bump**: Updated application version to **`6.0.0`** (`versionCode 7`).

## [5.2.0] - 2026-08-26

### Added

- **Island Background Opacity & Translucency Controls (`CustomizationsSection.kt`, `PositionsSection.kt`, `SmartIslandSettingsRepository.kt`)**:
  - Added continuous opacity adjustment (`SmartIslandSettings.MIN_OPACITY` = 0.20f to `MAX_OPACITY` = 1.00f) with live percentage readouts.
  - Dedicated **"Island Opacity & Transparency"** cards integrated in both **Appearance & Colors Studio** and **Position & Geometry** tabs.
  - 4 quick 1-tap presets: **100% Solid**, **85% Dark**, **70% Glass**, and **50% Clear**.
  - Micro-increment buttons (`+/- 5%`) for fine precision adjustments.
  - Full overlay background rendering support with dynamic alpha blending and drop shadow calibration.
- **High-Performance Notification History Virtualization & "Delete by App" (`NotificationHistorySection.kt`, `NotificationHistoryRepository.kt`)**:
  - **Virtualized `LazyColumn` Architecture**: Replaced full-tree column composition with a top-level virtualized list using stable item keys (`it.id`), eliminating UI lag spikes on datasets with 500–1000+ notifications.
  - **Asynchronous `AppIconMemoryCache`**: Built an in-memory LRU icon cache backed by `Dispatchers.IO` loading to prevent main-thread binder stalls during rapid scrolling.
  - **"Delete by App" Support**: Added `deleteByPackage` in SQLite helper and repository, accompanied by an interactive modal displaying per-app notification counts, 1-tap filtering, and bulk deletion with confirmation.
  - **In-Memory State Management**: Optimized repository updates to directly maintain the in-memory history state without executing heavy full-table SQLite re-queries on every incoming notification or deletion.
  - **Throttled Pruning Routine**: Throttled automated history database cleanup in `SmartIslandNotificationListenerService` to run at most once every 15 minutes.
- **Gesture Guide Interactive Layout Overhaul (`GesturesSection.kt`)**:
  - Replaced cramped 5-badge single row with a responsive 2-column clickable badge grid with generous tap targets.
  - 1-tap quick navigation: Tapping any gesture summary badge immediately switches to that gesture's step-by-step interactive guide.
  - Added clean 12.dp edge padding to `ScrollableTabRow`.

### Fixed & Improved

- **Multi-Device Crash Prevention & System Service Guards**:
  - `SmartIslandOverlayService`: Converted `KeyguardManager` and `NotificationManager` casts to safe `as?` lookups with guarded null checks to prevent crashes on specialized OEM builds.
  - `SmartIslandNotificationListenerService`: Wrapped `activeNotifications` iteration and nullable `mediaSessionManager` queries with safe calls and `runCatchingLogged` blocks.
  - `LaunchableApp`: Guarded `AppOpsManager` and `UsageStatsManager` usage access resolution in `runCatching`.
  - `SystemEventReceiver`: Fixed `ACTION_BATTERY_CHANGED` sticky broadcast probe registration on Android 14+ (API 34+) using `Context.RECEIVER_EXPORTED`.
  - `LiveActivityParser`: Fixed regex state evaluation bug by caching extracted duration minutes in local variable to prevent `IllegalStateException: No match found`.
  - `NotificationHistoryRepository`: Added `CoroutineExceptionHandler` to the IO coroutine scope to prevent uncaught SQLite exceptions.
- **UI Responsiveness & Layout Stability**:
  - Fixed parent height measurement in `DetailScreenHost` for `NotificationHistorySection` by providing `.weight(1f)` constraint.
  - Added internal scrolling (`verticalScroll`) and responsive max-height constraints to `NotificationDetailDialog` and `DeleteByAppDialog` to prevent button clipping on long notification messages.
  - Fixed setting toggle subtitle text wrapping by applying `.weight(1f)` to title/subtitle columns in `NotificationsAndPrivacySection.kt`.
  - Corrected split-pill window coordinate positioning logic to keep the right/left circle bounds from clipping on edge notches.
- **Version Bump**: Updated application version to **`5.2.0`** (`versionCode 6`).

## [5.1.0] - 2026-08-22

### Added

- **Inline-Reply Input & Soft-Keyboard WindowManager Focus Handling (`NotificationExpanded.kt`, `SmartIslandOverlayService.kt`)**:
  - Direct inline text reply right inside the expanded Island notification card for messaging apps (WhatsApp, Telegram, Signal, SMS, Slack, etc.).
  - **Dynamic WindowManager Focus Switching**:
    - Switches overlay window from non-focusable (`FLAG_NOT_FOCUSABLE`) for normal touch pass-through to input-ready focusable mode (`FLAG_ALT_FOCUSABLE_IM` / input method aware) with `SOFT_INPUT_ADJUST_PAN` / `SOFT_INPUT_ADJUST_RESIZE` when typing.
    - Added `isInputActive` state to `IslandViewModel` to automatically pause auto-collapse timers and prevent the island from vanishing while typing.
    - Direct `RemoteInput` pending intent dispatching with quick send button, automatic text clearance, and keyboard dismiss handling.
- **Timer & Stopwatch Dynamic Island Modes (`IslandMode.TIMER`, `IslandMode.STOPWATCH`)**:
  - **Intelligent Clock Parsing (`TimerStopwatchParser.kt`)**:
    - Deep-parsing of timer and stopwatch status notifications across major OEM clock apps (Google Clock, Samsung Clock, Xiaomi/MIUI/HyperOS Clock, OnePlus/ColorOS Clock, Huawei Clock).
    - Extracts remaining countdown time, total duration, elapsed millisecond ticker, timer state (running, paused, finished), and lap sequences.
  - **Collapsed Pill Glyphs**:
    - Live countdown badge and animated timer glyph; active stopwatch millisecond ticker.
  - **Expanded Timer Card (`TimerExpanded.kt`)**:
    - Circular and linear progress indicators, remaining countdown display, original duration tag, and interactive controls (*Pause*, *Resume*, *Reset*, *Stop*).
  - **Expanded Stopwatch Card (`StopwatchExpanded.kt`)**:
    - High-frequency elapsed ticker, lap counter, full lap times history list, and interactive controls (*Pause*, *Lap*, *Resume*, *Reset*).
  - Custom RGB Color Studio and preset palettes support for both Timer and Stopwatch accent colors.
- **Persistent SQLite Notification History (`NotificationHistorySection.kt`)**:
  - Full local SQLite database architecture via `NotificationHistoryDbHelper` and `NotificationHistoryRepository`.
  - Automatically captures and indexes past status bar notifications locally with app metadata, timestamps, and category modes.
  - **Studio History Management Section**:
    - Real-time search bar filtering across notification titles, message bodies, and package names.
    - App filter chips and mode filter chips for quick scoping.
    - Full notification details bottom sheet.
    - Swipe-to-delete gesture per entry and 1-tap "Clear All History" action.
    - Automated SQLite pruning routines to keep local database lightweight.
- **OEM Device Rules Engine & Background Keep-Alive Guide (`OemDeviceRules.kt`, `OemAutostartUtil.kt`)**:
  - Deep vendor-specific autostart and battery optimization guidance for Xiaomi/HyperOS/MIUI, Samsung OneUI, OnePlus/Oppo/Realme (ColorOS/OxygenOS), Huawei/Honor (EMUI/MagicOS), Vivo/iQOO (FuntouchOS/OriginOS), Asus ZenUI, and Transsion (HiOS/XOS).
  - Direct deep-links to vendor-specific battery optimization, autostart, and lock-in-recents settings menus.
  - Specialized OEM notification handling rules (e.g. system screen recorder packages, hotspot tethering broadcasts, and incoming call heads-up overrides).
- **In-App GitHub Release Checker & Network Privacy Controls (`GitHubApiService.kt`, `allowNetworkChecks`)**:
  - Direct in-app GitHub Releases API integration checking for newer versions, release changelogs, and download links.
  - **Strict Network Privacy Guard**:
    - Added `allowNetworkChecks` setting (persisted via `allow_network_checks` key in DataStore).
    - UI toggle under *Notifications & Privacy* section.
    - Offline Mode fallback: When disabled, zero network calls are dispatched, and the About section displays an explicit Offline Mode badge.
- **Hotspot Tethering Monitor & Navigation Parser Enhancements**:
  - `HotspotUtil`: Enhanced connected client regex parser across various Android versions and OEM tethering notifications.
  - `NavigationParser`: Improved multi-line route instruction handling, distance/ETA parsing, and turn-by-turn maneuver arrow glyph extraction.
- **Unit Test Suite Expansion**:
  - Added 52+ unit test cases covering new modules and critical paths:
    - `TimerStopwatchParserTest` (268 lines): Complete validation of Google, Samsung, and Xiaomi clock parsing rules.
    - `GitHubApiServiceTest` (52 lines): Semver version comparison and prerelease tag parsing tests.
    - `OemDeviceRulesTest` (95 lines): OEM manufacturer rules, package overrides, and autostart intent resolution.
    - `NavigationParserTest` (32 lines): Route instruction and ETA regex extraction tests.
    - `NotificationHistoryEntryTest` (65 lines): SQLite entity serialization and category mapping tests.
    - `IslandViewModelTest` (54 lines): Inline-reply active input state and auto-collapse suppression tests.
    - `SmartIslandNotificationRepositoryTest` (26 lines): Repository flow and timer/stopwatch state tests.

### Changed

- **Version Bump**: Updated application version to **`5.1.0`** (`versionCode 5`).
- **DataStore Settings Expansion**: Added preference keys for `allow_network_checks`, `timer_color`, `stopwatch_color`, and `device_type`.
- **About Section Redesign**: Revamped `AboutSection.kt` with live update insights, system health diagnostics, and contributor credits.
- **Build Tooling**: Updated Gradle wrapper to **`9.6.1` / `9.7.1`** with refined AGP build settings.

### Fixed

- **Soft-Keyboard Overlay Focus**: Dynamically adjusted WindowManager layout flags (`FLAG_NOT_FOCUSABLE` / `FLAG_ALT_FOCUSABLE_IM`) during inline reply text input, preventing touch blocking while allowing smooth keyboard entry.
- **Notification Suppression Race Condition**: Enhanced retry handling and self-cancellation tracking in `SmartIslandNotificationListenerService` to avoid missed dismissals.
- **Turn-by-Turn Instruction Truncation**: Fixed route text truncation and maneuver icon extraction in navigation mode.
- **CI Lint Cleanups**: Resolved AndroidGradlePluginVersion and AGP deprecation lint warnings for 100% clean builds.

## [5.0.0] - 2026-08-15

### Added

- **Low Battery & Battery Saver Modes (`IslandMode.Battery`)**:
  - Added dedicated visual states for battery health:
    - *Low Battery* (20%): Pulsing red `BatteryAlert` indicator glyph with live battery percentage badge.
    - *Battery Saver*: Warm amber `BatterySaver` energy-savings glyph with live battery percentage badge.
    - *Charging*: Electric green lightning bolt with remaining charge time estimate.
  - Broadcast receiver integration via `ACTION_BATTERY_LOW`, `ACTION_BATTERY_OKAY`, and `ACTION_POWER_SAVE_MODE_CHANGED` in `SystemEventReceiver`.
- **Full-Color App Launcher Icons (v4 Parity)**:
  - Restored `loadAppIconBitmap(packageName)` utilizing `packageManager.getApplicationIcon(packageName)` with LRU bitmap caching in `SmartIslandNotificationListenerService`.
  - Displays original, full-color application launcher icons (WhatsApp, Snapchat, Gmail, Instagram, etc.) in the collapsed pill and expanded cards, eliminating 1-bit status bar silhouettes.
- **Message Sync Notification Filtering**:
  - Implemented `NotificationFilter.isMessageSyncNotification` to detect background chat polling and synchronization notifications (*"Syncing messages..."*, *"Checking for new messages"*, *"Connecting..."*, *"Syncing snaps"*).
  - Explicitly suppressed sync notifications from triggering or hijacking the active download/upload progress mode.
- **Revamped 5-Gesture Guide & Controls (`GesturesSection.kt`)**:
  - Completely updated gesture guide with structured, step-by-step text instructions, badges, and quick reference chips for all 5 gestures:
    1. *Single Tap / Click*: Expand pill & collapse back *(Cutout region triggers shortcuts even when idle-hidden)*.
    2. *Quick Swipe Up*: Dismiss the currently active notification card.
    3. *Hold + Swipe Up*: Press & hold for 300ms until haptic vibration pulse, then swipe up to clear **all** notifications simultaneously.
    4. *Swipe Down*: Drag downward by \ge 48dp to launch the application in a freeform floating window overlay.
    5. *Swipe Left / Right*: Smooth horizontal swipe navigation between multiple notifications and media in the stack.
- **Horizontal Pager Snapping Stability**:
  - Synchronized horizontal notification card pager in `IslandExpandedContent` with `pagerState.settledPage` and guarded programmatic page scrolling with `!pagerState.isScrollInProgress`, preventing mid-swipe freezing or locking between notification cards.
- **Flashlight Dynamic Island Mode**:
  - Added `IslandMode.Flashlight` with active torch indicator badge and 1-tap quick turn-off action.
- **Screen Recording Mode**:
  - Added `IslandMode.ScreenRecording` with live elapsed recording timer.
- **Custom RGB Slider Color Picker**:
  - Added fine-grained Red, Green, Blue slider dialog with real-time Hex color preview for all 11 dynamic island modes.
- **Split Island Multi-State Pill**:
  - Added secondary auxiliary bubble pill support for concurrent background events (e.g. Music + Hotspot, Call + Bluetooth).
- **Refreshed Showcase Screenshots**:
  - Added 16 high-resolution screenshots in `assets/screenshots/` detailing the Simulation Lab, Notch Layouts, Sizing Controls, Features Hub, Privacy Rules, Live Activities, App Shortcuts, Color Studio, Custom RGB Picker, Expanded Music Player, About Screen, Community Feedback, 5 Gesture Guides, Permissions Center, and Floating Home Screen Island.

### Changed

- **Version Bump**: Updated application version to **`5.0.0`** (`versionCode 5`).
- **Copilot & Contributing Guidelines**: Updated `.github/copilot-instructions.md` with all v5.0.0 island modes, icon loading rules, pager snapping requirements, and CI/CD validation instructions.

### Fixed

- **GitHub Actions CI/CD Pipeline**:
  - Fixed workflow version incompatibilities by updating all GitHub action dependencies to stable official `@v4` (`actions/checkout@v4`, `actions/setup-java@v4`, `actions/upload-artifact@v4`, `actions/download-artifact@v4`).
- **Camera Cutout Detector API Guard**:
  - Updated `display?.cutout` check from API 28 (P) to API 29 (Q) in `CameraCutoutDetector.kt` to fix `NewApi` compilation errors.
- **Compose Lint Suppressions**:
  - Added lint rule suppressions in `app/build.gradle.kts` for non-fatal compose warnings (`IconXmlAndPng`, `BatteryLife`, `ConfigurationScreenWidthHeight`, `ModifierParameter`), ensuring 100% clean CI builds.
- **On-Device Version Verification**:
  - Verified `versionName = 5.0.0` and `versionCode = 5` across Settings, About screen, and ADB package dumpsys.

## [4.0.0] - 2026-07-27

### Added

- **Material 3 Expressive Design Overhaul**: Complete UI overhaul featuring modern Material 3 design tokens (`surfaceContainer`, `outlineVariant`, `primaryContainer`), rounded shapes, and clean minimal visual aesthetics.
- **Wallpaper Dynamic Color Support**: Enabled Android 12+ (API 31+) Dynamic System Color extraction (`dynamicLightColorScheme` / `dynamicDarkColorScheme`) for seamless wallpaper color harmonization.
- **Wi-Fi Hotspot Monitor (`HotspotExpanded` & `HotspotUtil`)**: Added real-time Wi-Fi tethering monitoring via `HotspotUtil`, showing active connected client count parsed from system hotspot notifications, SSID details, data usage indicators, static hotspot icon and labels, and a quick turn-off action button.
- **Live Activity Engine (`LiveActivityParser`, `LiveActivityExpanded`)**: Full live activity support for delivery and ride tracking with real-time status cards, dynamic brand colors per app, smooth Compose animations, and structured notification extras parsing.
- **Download/Upload Progress Mode (`DownloadExpanded`)**: Real-time download and upload progress bars with transfer speeds (MB/s), file names, custom transfer icons, and refined download detection logic. Notifications are hidden from the system shade by default when displayed in the island.
- **Turn-by-Turn Navigation Mode (`NavigationExpanded`, `NavigationParser`)**: Step-by-step navigation guidance showing next instruction, distance, ETA, maneuver arrow icons, and a dedicated navigation mode card.
- **Per-App Notification & Sound Controls**: Added granular per-app notification enable/disable toggles and independent sound controls under `NotificationsAndPrivacySection`.
- **Grouped Settings Architecture**: Consolidated 8 standalone settings sections into **4 minimal, clean expandable Category Hub Cards**:
  - *Appearance & Layout* (Island layout, position, theme & custom visualizer styling).
  - *Interactions & Shortcuts* (Gesture controls/playground & quick launch app shortcuts grid).
  - *Permissions & Privacy* (System service setup & lockscreen notification rules).
  - *Help & About* (Support, Telegram community & version information).
- **Expanded Quick-Test Dashboard**: Added interactive preview triggers for Hotspot, Transfer, Live Activity, and Navigation preview modes.
- **Enhanced Item Row Controls (`ClickableRowItem`)**: Upgraded item rows with soft MD3 rounded icon container badges, crisp icon tinting, and subtitle text support.

### Changed

- **Settings Hub Layout**: Refactored Settings tab into a minimal, uncluttered, expandable accordion design.
- **Notification Actions & Ring Styling**: Updated notification action button styles and ring/badge visual treatment for better MD3 consistency.
- **Download UI & Progress Visuals**: Refactored download card UI, progress bar stroke widths, heights, and padding to align with the new MD3 design system.
- **Live Activity Brand Colors**: Added dynamic per-app brand color tinting to live activity cards for richer visual identity.
- **Theme Updates**: Updated `Theme.kt` with finalized MD3 color roles and grouped settings card surface treatments.
- **Release Version**: Updated application version to `4.0.0` (versionCode `4`).

### Fixed

- **Android Lint Debug Build Issue**: Resolved lint errors that were blocking the debug build pipeline.

## [3.2.1] - 2026-07-24

### Added

- **System Service Recovery (`SystemServiceRecovery`)**: Implemented system service recovery utility for reconnecting system-managed services (`AccessibilityService` and `NotificationListenerService`) to improve service stability and state restoration.
- **Notification Shade Suppression Controls**: Added a dedicated "Hide from notification shade" setting under Notifications & Privacy (defaulting to off) allowing optional suppression of third-party notifications from Android's system shade.
- **OEM Vendor ROM Stability Fixes**: Added background autostart and keep-alive guards to prevent aggressive OEM Accessibility Service kills on heavy vendor ROMs (MIUI/HyperOS, ColorOS/RealmeUI, FuntouchOS, OneUI).
- **Shizuku 1-Tap Restricted Settings & Permission Grant**: Added 1-tap Shizuku execution to auto-grant Restricted Settings (`ACCESS_RESTRICTED_SETTINGS`), Usage Access (`GET_USAGE_STATS`), System Overlay, Accessibility, Notification Access, and Battery Whitelist without manual setup.
- **Direct Notification Listener Detail Intent**: Enhanced the Notification Listener permission launcher to open directly to Smart Island's toggle page via `ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS`.
- **Customizable Island Action Buttons**: Added toggle settings allowing users to customize and remove action buttons from the Smart Island overlay.
- **New Application Icon**: Introduced a refreshed, modern application launcher icon and updated visual app assets.
- **Clear Test Notifications Option**: Added a dedicated "Clear Test Notifications" action in the Quick Testing dashboard section to immediately remove test and active island overlays.

### Changed

- **Accessibility Service Lifecycle**: Refactored service toggle logic so disabling Smart Island removes the pill overlay without calling `disableSelf()`, preventing permission revocation and eliminating the need for users to re-grant permissions.
- **Settings UI & Navigation Redesign**: Redesigned the settings screen layout, introduced a dedicated bottom navigation bar, and restructured settings DataStore management.
- **DataStore & IO Exception Recovery**: Implemented atomic DataStore update operations and automatic IOException recovery in `SmartIslandSettingsRepository`.
- **Notification Repository Bounding**: Enforced atomic `MutableStateFlow` updates, a 50-notification queue limit, and automatic stale key cleanup in `SmartIslandNotificationRepository`.
- **Updated Application Screenshots**: Refreshed all 12 high-resolution application screenshots in repository documentation showcasing the v3.2.1 dashboard, Shizuku setup, Notifications & Privacy, color picker, positions, app launcher, gesture guide, music player, and battery charging overlays.
- **Release Version**: Updated application version to `3.2.1` (versionCode `3`).

### Fixed

- **Permission State Decoupling**: Decoupled runtime permission validation state from the main Smart Island service toggle in `SmartIslandHomeScreen`.
- **Main Thread Execution**: Removed main-thread `Thread.sleep()` calls during overlay removal for smooth asynchronous cleanup.

## [3.2] - 2026-07-18

### Added

- **System Notification Suppression & Listener Service (`SmartIslandNotificationListenerService`)**: Implemented system notification filtering (`NotificationFilter`) and notification listener integration to present notifications in the island while suppressing duplicate heads-up alerts and system shade entries when active.
- **Landscape Orientation Listener (`IslandOrientationListener`)**: Added automatic orientation change detection to dynamically remove and hide the Smart Island overlay in landscape mode, ensuring uninterrupted full-screen gaming and video playback.
- **Battery Optimization Setup Section**: Expanded the Permissions configuration screen with a dedicated guidance card for setting Battery Optimization to 'No restrictions' to ensure OS background execution protection.
- **Lock Screen Privacy & Controls**: Introduced lock screen visibility settings (`showOnLockScreen`, `hideSensitiveOnLockScreen`) to allow opt-in island display while respecting user privacy.
- **Strict Permission Guard**: Enforced runtime permission validation before enabling the main Smart Island service toggle switch.
- **Overlay System Warning Toggle**: Added a direct settings action to guide users on hiding the persistent "displaying over other apps" system notification.
- **Community Contributions & PR Merges**: Integrated Pull Request [#11](https://github.com/agupta07505/SmartIsland/pull/11) contributed by [@likhithkrishna1103-tech](https://github.com/likhithkrishna1103-tech) (Likhith Krishna) bringing foundational improvements to lock screen privacy settings, hidden API restriction bypass for touchable regions, notification icon handling, and music player responsiveness.

### Changed

- **Pass-Through Touch Region Registration**: Bypassed hidden API restrictions (`WindowTouchBounds`) to refine non-touchable window inset boundaries, restoring reliable tap and gesture pass-through to underlying applications.
- **App Shortcuts & Visual Spacing**: Updated expanded island app shortcut grid rendering and improved overall dashboard card padding and layout spacing.
- **Documentation & UI Assets**: Refreshed high-resolution application screenshots showcasing updated permissions, positions, customizations, app shortcuts grid, and wavy music player.
- **Release Version**: Updated application target version to `3.2` (versionCode `3`).

### Fixed

- **Collapsed Pill Touch Reliability**: Resolved non-responsive tap and gesture detection on the collapsed island pill.
- **Large Window Expansion Shifting**: Fixed content jump and height layout shifting when expanding notifications or switching pages.
- **App Clear Crash**: Fixed background service crash when clearing the application from the recent apps launcher.
- **Overlay Memory & Lifecycle**: Resolved service lifecycle leakage and improved state persistence across theme changes and service restarts.

### Planned next

- Onboarding wizard & setup checklist enhancements for first-time users.
- Custom notification filtering rules by package and priority level.
- Dynamic island shape templates and expansion animation presets.

## [3.1] - 2026-07-15

### Added

- **First-Run Welcome Experience**: Added a polished welcome dialog that introduces Smart Island's privacy-first approach and gives new users a clear starting point.
- **Community Shortcuts**: Added direct actions in the welcome dialog for starring the project on GitHub and joining the Smart Island Telegram community.
- **Persistent Welcome State**: Added a local DataStore preference so the welcome dialog is shown only once after installation.

### Changed

- **Support & Feedback**: Improved community links and support actions, including clearer access to the Telegram community.
- **Release Tooling**: Updated GitHub Actions dependencies used by the Android build and release workflow.
- **Release Version**: Updated the application version to `3.1`.

### Planned next

- Fix collapsed-pill touch reliability as the first v3.2 release blocker.
- Add opt-in, privacy-safe Smart Island support on the lock screen after the touch fix is verified.

## [3.0.0] - 2026-07-10

### Added

- **Interactive Gesture Guide**: Added a dedicated "Gesture guide" option to the home screen settings dashboard routing to an interactive tabbed guide (`GesturesSection.kt`).
- **Looping Gesture Animations**: Built looping animations with pulsing finger paths demonstrating Swipe Up (dismiss), Swipe Down (launch in popup/floating window), and Swipe Left/Right (horizontal paging) on a mock status bar island.
- **Try-It-Yourself Playground**: Added interactive gesture detectors utilizing drag thresholds to let users horizontally/vertically swipe the mock island and trigger responses live in a sandbox.
- **App Shortcuts**: Added a shortcut picker for selecting up to eight installed apps and opening them directly from the expanded island, with recent apps used as a fallback.
- **Expanded Color Customization**: Added preset palettes and a custom RGB picker for the notification dot, battery indicator, and music visualizer.
- **Dark Theme Support**: Added theme-aware settings and overlay styling for improved readability in dark mode.

### Changed

- **Key-Based Height Synchronization**: Refactored the dynamic height tracking (`pageHeights`) in the expanded pager to key by unique notification ID instead of page index, permanently preventing height-jumping artifacts when notifications update or switch categories.
- **Density-Independent Gesture Limits**: Adjusted the touch offsets and swipe trigger limits to be calculated in DP and translated dynamically to pixels using `LocalDensity`, correcting the horizontal pagination guide metrics and vertical drag thresholds on high and low-DPI devices.
- **Overlay Lifecycle**: Improved service, Compose view-tree, and background lifecycle handling to keep the island stable during recomposition, theme changes, and service restarts.
- **Release Automation**: Refined Android CI artifact naming and added structured GitHub release notes.

### Fixed

- **Expanded Content Sizing**: Fixed shifting, overshooting, and incorrect minimum-height behavior while paging between notifications with different content heights.
- **Swipe-Down Gesture**: Fixed unreliable drag-down recognition when launching an app in a floating window.
- **RGB Picker**: Fixed the custom color picker's RGB slider rendering and selection behavior.
- **Unexpected App Closure**: Fixed multiple lifecycle and animation paths that could force-close the app or overlay.
- **Notification Filtering**: Improved filtering of system and ongoing notifications after removing the unused AndroidX Window dependency.

## [2.2.0] - 2026-07-07

### Added
- **Battery Charging Island Mode**: Complete charging status indicator that slides down automatically when a charger is plugged in (`ACTION_POWER_CONNECTED`).
- **Pulsing Battery & Gradient Animations**: Implemented pulsing charging icons (infinite scale transition) in the collapsed state and flowing multicolor gradient indicators in the expanded state.
- **Time-until-full & Progress**: Dynamic battery percentage and charging-time remaining estimates computed directly via `BatteryManager`.
- **Battery Demo Button**: Added a dedicated "Battery" button to the Quick Test controls on the home screen to preview and test the charging island mode.
- **Unit Tests**: Added unit test suites verifying priority/suppression rules for non-system apps (`NotificationPriorityTest`), media controller action mappings, and `SystemEventReceiver` battery events.

### Changed
- **Reorganized Home Dashboard UI**: Restructured Quick Test buttons into a 2x2 grid layout and repositioned status texts under subtitles for improved spacing and visual appeal.
- **Center Header Alignment**: Center-aligned the main header description text on the home screen.
- **Battery Charging Updates**: Refactored `SystemEventReceiver` to update battery percentages silently without re-triggering expand and auto-collapse cycles.
- **Media Controller Resolution**: Improved media playback robustness by prioritizing active playing sessions (`PlaybackState.STATE_PLAYING`) when resolving media controllers by package name.
- **Better reflection diagnostics**: Switched key reflection-based API hooks (pass-through touch insets and freeform window launching) to use `runCatchingLogged` utility for easier debugging.

### Fixed
- **License Header Typo**: Corrected `GNU GPL v3License` to `GNU GPL v3 License` in workflow, configuration, ignore, and helper files.
- **Architecture Doc Tracked Status**: Removed `analysis.md` from `.gitignore` list to ensure the codebase analysis documentation remains fully tracked in Git.

## [2.1.0] - 2026-07-07

### Added

- **Centralized Notification Repository (`SmartIslandNotificationRepository`)**: Created to manage notification streams and commands flow reactively.
- **Dependency Injection**: Registered `SmartIslandApp` Application class to hold repositories singletons.
- **Material 3 Unified Theme**: Introduced day/night theme structure with DayNight Material3 scheme support.
- **Automated Tests**: Created JUnit/MockK test suites: `IslandModeMappingTest`, `NotificationPriorityTest`, `SmartIslandSettingsTest`.
- **Utility Modules**: Created isolated helper scripts `TimeUtils` and `LogUtils`.

### Changed

- **Modularized UI Structure**: Split monolithic screen `SmartIslandHomeScreen.kt` into clean components: `HeaderSection`, `PermissionsSection`, `PositionsSection`, `SupportSection`, and `AboutSection`.
- **Decoupled Architecture**: Migrated views `IslandOverlayView.kt` and `IslandExpandedContent.kt` from calling static service instances to reactively communicating via repositories.
- **Resource Maintainability**: Moved hardcoded screen UI strings into standard `strings.xml` resource tags.
- **Build Configurations**: Enabled ProGuard/R8 minification, resource shrinking, and strict lint checks in `build.gradle.kts`.

### Fixed

- **License Header Typo**: Corrected `GNU GPL v3License` to `GNU GPL v3 License` globally.
- **Theme Parent Reference**: Migrated parent configuration in `styles.xml` to `Theme.DeviceDefault.NoActionBar` to prevent XML resource linking compilation errors.

## [2.0.0] - 2026-07-05

### Added

- **Custom Wavy Music Seek Bar (`WavyMusicSeekBar`)**: Renders playing progress as a smooth, filled organic wave with custom wave animations that automatically freeze when paused, and flat damping at layout boundaries. Includes a circular thumb for direct drag-to-seek support.
- **Enhanced Player Controls**: Integrated a Song Like button (with outline/fill favorite heart status) on the left side of the skip-back button, and a Repeat/Loop button on the right side of the skip-forward button.
- **Home Screen Dashboard Reorganization**: Redesigned the main menu into neat topic cards: Permissions, Positions, Support & Feedback, and About.
- **Dashboard Slide Transitions**: Implemented slide-in/slide-out horizontal enter/exit animations using Compose `AnimatedContent` for menu subtopics, complete with a system back handler (`BackHandler`) to reverse the slides.

### Changed

- **App Logo Header**: Replaced the visual pill visual placeholder in the home screen header with the actual application icon embedded inside a sleek black background card, adjusted to a filled 60dp icon size.
- **Visual Spacing**: Shifted dashboard and category headers down by applying spacious 36dp top paddings to achieve a modern, relaxed design.
- **Active Session Seeker**: Refactored the notification listener to correctly pass its listener component name, enabling accurate fallback media controllers querying when token actions are restricted.

### Fixed

- **App Startup Crash**: Resolved startup crash on launch due to Compose `painterResource` trying to parse the adaptive vector launcher icon xml. Fixed by dynamically extracting the launcher icon drawable and rendering it to a Canvas-backed bitmap.
- **Overlay "Non-Touchable" Dead Zone**: Corrected height calculation of the collapsed overlay window by removing the status bar height offset and extra padding, keeping the window size tight to the visual bounds.
- **Pill Gestures and Swipe Interceptions**: Fixed direct tap gesture failures in the collapsed pill and swipe-up/down failures by capturing reactive state delegates to prevent stale state retention.
- **Controls State Synchronization**: Fixed state mismatch issues where loop state changes made inside external media apps (e.g., Spotify) did not reflect in the Smart Island overlay. Added custom extras query prioritization and strict toggling logic to avoid double-activation cycles.
- **Live Progress Catchup**: Fixed seek bar staying at its previous position on re-expand by querying live playback state offsets immediately upon window expansion.

## [1.0.0] - 2026-07-04

### Added

- Initial open-source baseline for the Smart Island Android app.
- Floating overlay service with animated collapsed and expanded island states.
- Notification listener integration for notification, incoming-call, and media modes.
- Local customization for island size, position, and corner radius.
- Demo states for notification, call, and music previews.
- App screenshots added to the README to showcase UI features.

### Changed

- Replaced default launcher icon with custom adaptive and legacy icons generated from the transparent logo (`logo.png`) on a pure black background.

### Fixed

- Fixed support/feedback links in the app to correctly load GitHub issue templates (`bug_report.md` and `feature_request.md`).
