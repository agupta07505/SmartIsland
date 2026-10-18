<p align="center">
  <img src="assets/logo.png" width="108" height="108" alt="Smart Island Logo" />
</p>

<h1 align="center">Smart Island</h1>

<p align="center">
  <strong>A lightweight, privacy-first Android dynamic overlay that brings glanceable notifications, media controls, active calls, battery states, timers, and live system activities to your notch.</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android%208.0%2B%20(API%2026--36)-3DDC84?logo=android&logoColor=white" alt="Android 8.0+" />
  <img src="https://img.shields.io/badge/Kotlin-2.0%20%7C%20JVM%2017-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin 2.0" />
  <img src="https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white" alt="Jetpack Compose Material 3" />
  <img src="https://img.shields.io/badge/License-GNU%20GPLv3-blue.svg" alt="GNU GPLv3" />
  <a href="https://github.com/agupta07505/SmartIsland/actions/workflows/android.yml">
    <img src="https://github.com/agupta07505/SmartIsland/actions/workflows/android.yml/badge.svg" alt="CI Build Status" />
  </a>
  <a href="https://telegram.me/SmartIslandApp" target="_blank">
    <img src="https://img.shields.io/badge/Telegram-Community-2CA5E0?logo=telegram&logoColor=white" alt="Telegram Community" />
  </a>
  <a href="https://www.virustotal.com/gui/file/c07da408e7fa3e3fdeb25ec6415af07f1d56eff099840221138e9364c6065102/details" target="_blank">
    <img src="https://img.shields.io/badge/VirusTotal-Verified%20Clean-007ACC?logo=virustotal&logoColor=white" alt="VirusTotal Verified Clean" />
  </a>
</p>

<p align="center">
  <a href="#overview">Overview</a> &bull;
  <a href="#key-highlights">Highlights</a> &bull;
  <a href="#screenshots">Screenshots</a> &bull;
  <a href="#features">Features</a> &bull;
  <a href="#13-dynamic-island-modes">Island Modes</a> &bull;
  <a href="#gesture-guide">Gestures</a> &bull;
  <a href="#architecture">Architecture</a> &bull;
  <a href="#building-from-source">Building from Source</a> &bull;
  <a href="#privacy--permissions">Privacy</a> &bull;
  <a href="#community--support">Community</a>
</p>

---

## Overview

**Smart Island** is an open-source Android application that renders a fluid, dynamic floating pill near your device's camera notch or status bar. It intercepts system notifications, live activities, media playback, incoming calls, charging events, hotspot connections, navigation directions, timers, stopwatches, and battery alerts locally—expanding into rich interactive cards with zero cloud tracking.

Designed to be 100% transparent, hackable, and privacy-conscious: all notification processing and history logs are strictly stored on-device, preferences reside in local AndroidX DataStore, and network access is solely opt-in for checking GitHub release versions.

---

## Key Highlights

<table>
  <tr>
    <td width="50%">
      <h3>🛡️ 100% Privacy & Local-First</h3>
      <p>All notification interception, alert history, and settings are strictly processed and stored on-device. Zero telemetry, zero analytics SDKs, and zero background tracking.</p>
    </td>
    <td width="50%">
      <h3>⚡ Ultra-Fluid Spring Physics</h3>
      <p>Native synchronized spring curves (<code>stiffness = 520f</code>, <code>dampingRatio = 0.72f</code>) with haptic tactile response and Baseline Profile AOT compilation for silky 60/120 FPS.</p>
    </td>
  </tr>
  <tr>
    <td width="50%">
      <h3>🎨 Deep Customization Studio</h3>
      <p>Millimeter-accurate notch calibration, independent portrait and landscape geometry, custom font loader (TTF/OTF, Lora, Inter, Poppins), and full RGB color styling.</p>
    </td>
    <td width="50%">
      <h3>👆 Dual-Tier Gesture Engine</h3>
      <p>Swipe horizontally across the compact pill to cycle tracks or notifications without expanding; enjoy intuitive swipe-to-dismiss and long-press haptic gestures on expanded cards.</p>
    </td>
  </tr>
</table>

---

## Screenshots

<p align="center">
  <img src="assets/screenshots/01_home_try_it_out.jpg" width="24%" alt="Home Screen & Try It Out" />
  <img src="assets/screenshots/02_layout_appearance_controls.jpg" width="24%" alt="Layout & Appearance Controls" />
  <img src="assets/screenshots/03_notifications_privacy_rules.jpg" width="24%" alt="Notifications & OEM Privacy Rules" />
  <img src="assets/screenshots/04_live_activities_app_alerts.jpg" width="24%" alt="Live Activities & App Alerts Manager" />
</p>

<p align="center">
  <img src="assets/screenshots/05_app_shortcuts_launcher.jpg" width="24%" alt="App Shortcuts Launcher" />
  <img src="assets/screenshots/06_notification_history_log.jpg" width="24%" alt="Virtualized Notification History Log" />
  <img src="assets/screenshots/07_appearance_colors_studio.jpg" width="24%" alt="Appearance & Colors Studio" />
  <img src="assets/screenshots/08_gesture_guide_playground.jpg" width="24%" alt="Gesture Guide & In-Pill Playground" />
</p>

<p align="center">
  <img src="assets/screenshots/09_permissions_setup_center.jpg" width="24%" alt="Permissions Setup Center" />
  <img src="assets/screenshots/10_backup_and_restore.jpg" width="24%" alt="Settings Backup & Restore" />
  <img src="assets/screenshots/11_app_updates_and_downloads.jpg" width="24%" alt="App Updates & Community Hub" />
  <img src="assets/screenshots/12_expanded_navigation_and_about.jpg" width="24%" alt="Expanded Navigation & Community" />
</p>

<p align="center">
  <img src="assets/screenshots/13_developer_options_diagnostics.jpg" width="24%" alt="Developer Options & System Diagnostics" />
</p>

---

## Features

| Feature Area | Capabilities & Details |
| --- | --- |
| **Per-Event Switches (30 Events)** | Individual toggles for 30 distinct system events across Power, Connectivity, Activities, and Privacy with folding categories, auto-expand badges, and separate Portrait & Landscape configurations. |
| **Portrait & Landscape Independence** | Fully independent geometry, edge distance, idle camera circle (offset, size, opacity), corner bar, and seamless rotation handling. |
| **Custom Font Studio** | Bundled open-source typography (Lora, Inter Display, Poppins) plus support for adding any custom `.ttf` or `.otf` font files. |
| **Privacy Indicator Dots** | Real-time indicators for Camera, Microphone, Location, and Screen Sharing merged into the pill, with 1-tap app usage inspector (enhanced with Shizuku). |
| **Same-App Grouping & Sibling Picker** | Groups multiple alerts from the same application into a unified card with a secondary cutout sibling switcher. |
| **VoIP Call Detection** | Audio-mode-based detection for VoIP calls (WhatsApp, Telegram, etc.) displaying native action buttons and active call duration. |
| **Multi-Stage Transfers Engine** | Scored classification for File Transfer, Upload, Backup, File Copy/Move, Delete, and App Install with real-time transfer speed (MB/s) and ETA meters. |
| **Motion Physics & Animation Speeds** | Relaxed, Normal, and Snappy animation presets per orientation that respect Android system animator duration scale. |
| **Settings Search Engine** | Comprehensive in-settings search indexing all toggles, events, and subpages with instant keyword matching. |
| **Baseline Profile Optimization** | Bundled `:baselineprofile` module ensures zero JIT compilation stutter and optimal runtime performance. |
| **Deep Burnt-Orange (#D84315) Design System** | Harmonious curated color palette with rich burnt-orange accents, deep slate/black card surfaces, crisp typography, and unified tinted Material icons throughout all settings cards and switches. |
| **App Updates & Project Insights Hub** | Dedicated in-app section featuring current version details, 1-tap GitHub update checker, in-app changelogs viewer, Top Contributors gallery, and Recent Commits feed. |
| **In-Pill Swipe Gestures & Media Controls** | Swiping horizontally across the compact pill skips tracks (`skipToNext`/`skipToPrevious`) or cycles notifications without expanding the island. Features master toggle, swipe direction pickers, and individual disable controls. |
| **Selective App Alerts & Sound Manager** | Fast selective app configuration modal with search bar, per-app alert toggles, custom ringtone sound toggling, and island exclusion without heavy upfront package loading. |
| **Configurable Companion Circle Position** | Place the multi-tasking companion circle on either the **Left** or **Right** of the pill with anti-overlap geometry clamping for corner and punch-hole displays. |
| **Material 3 Expressive UI** | Modern MD3 color tokens, dynamic wallpaper color adaptation (Android 12+), rounded shapes, and fluid physics. |
| **Ultra-Fluid Spring Physics** | iOS-grade synchronized spring curves (`stiffness = 520f`, `dampingRatio = 0.72f`), snappy 190ms cross-fades, organic companion bubble pops, and tactile button bounce. |
| **Tap Anywhere to Open App** | Tapping anywhere on expanded notification and music cards outside interactive buttons launches the source app and collapses the island. |
| **Auto-Hide Pill Inactivity Timer** | Automatically hides the pill and companion bubble after a customizable inactivity timeout (1s to 60s with 1-tap presets), with tap-to-awaken gesture. |
| **Landscape Mode Control** | User toggle to keep Smart Island visible in landscape orientation with automated screen width repositioning, or auto-hide for distraction-free gaming and video. |
| **Bluetooth Battery & Earbuds Animation** | Dual-path battery level extraction via broadcast extra and reflection, with a smooth 3-second spring-animated transition between live battery gauge and earbuds icon. |
| **Notification Cooldown & Anti-Spam** | Intelligent burst detection (e.g. 3 alerts in 30s) that temporarily cools down noisy apps, holding rapid spam and surfacing the latest message after a customizable quiet period (1–30 min), with per-app whitelist exclusion. |
| **Island Opacity & Transparency** | Continuous background opacity slider (20% to 100%) with 4 quick 1-tap presets (Solid, Dark, Glass, Clear) and ambient drop shadow rendering. |
| **Inline Reply & IME Focus** | Direct text reply input right inside the expanded Island notification card with dynamic WindowManager focus switching, soft keyboard integration, and `RemoteInput` dispatch. |
| **Timer & Stopwatch Modes** | Intelligent clock notification parsing for Google Clock, Samsung Clock, MIUI/HyperOS, ColorOS, and Huawei Clock. Collapsed countdown/elapsed badges, expanded circular/linear timer progress, live millisecond stopwatch ticker, and interactive pause/resume/lap/reset controls. |
| **Virtualized Notification History** | Ultra-smooth `LazyColumn` virtualized SQLite history log with background `AppIconMemoryCache`, search bar, app filter chips, full message inspection dialog, and bulk **Delete by App** support. |
| **OEM Device Rules Engine** | Tailored background protection and autostart management for Xiaomi/HyperOS, Samsung OneUI, OnePlus/Oppo/Realme (ColorOS/OxygenOS), Huawei/Honor, Vivo/iQOO, and Asus to prevent aggressive background kills. |
| **Full-Color App Icons** | Extracted launcher icons with LRU caching for crisp, authentic app icons in both collapsed pill and expanded cards. |
| **Battery Modes (Low & Saver)** | Dynamic Battery island displaying Green for Charging, Pulsing Red for Low Battery (&le; 20%), and Warm Amber for Battery Saver ON with live battery percentage badge. |
| **Comprehensive Gesture Engine** | Dual-tier gesture system: In-Pill Swipes (Left/Right/Up/Down for media/notifications) and Expanded Card Gestures (Tap, Dismiss, 300ms Hold + Swipe Clear All, Floating Window, Notification Shade). |
| **Split Island Multi-State** | Secondary auxiliary bubble for concurrent background activities (e.g. Music + Hotspot, Call + Bluetooth, Timer + Music). |
| **Wavy Music Player** | Real-time audio waveform scrubber, ambient album artwork background glow, track metadata, and dynamic color customization. |
| **Message Sync Suppression** | Automatically filters background message polling notifications (e.g. Snapchat, WhatsApp sync) to prevent false activity triggers. |
| **Live Activity Tracking** | Real-time delivery and rideshare tracking (Uber, Swiggy, Zomato, Blinkit, Rapido, Ola) with brand colors. |
| **Turn-by-Turn Navigation** | Live turn navigation indicators, maneuver direction arrows, remaining distance, and ETA. |
| **Transfer & Network Speed Meters** | Real-time transfer speed meters (MB/s), progress bars, and animated transfer indicators. |
| **Wi-Fi Hotspot Monitor** | Live tethering client counter, SSID badge, data usage, and quick turn-off action. |
| **Flashlight & Screen Recording** | Active torch toggle card and live screen recording timer overlay. |
| **App Shortcuts Launcher** | Quick-launch grid with up to 8 pinned apps or auto-filled recent applications, with complete user enable/disable control. |
| **Custom RGB Color Studio** | Fine-grained Red, Green, Blue slider color picker with live Hex preview for all 13 dynamic modes. |
| **Notch & Layout Presets** | Instant 1-tap calibration for Center Hole, Wide Island, Left Corner, Right Corner, and Compact Pill. |
| **Precision Sizing Controls** | Millimeter-accurate sliders for Width, Height, X Offset, Y Offset, Corner Radius, and Drop Shadows. |
| **Shizuku 1-Tap Auto Setup** | Automated permission grants for Restricted Settings, Usage Access, Overlay, and Battery Optimization. |
| **Backup & Restore System** | Export and restore complete island notch coordinates, dimensions, custom color themes, app shortcuts, and rules to/from JSON via Android Storage Access Framework with bounds validation. |
| **iPhone Notch Mode** | Top-edge docked notch styling, rounded bottom corners, and complete elimination of the secondary right companion circle for an authentic iPhone notch experience. |
| **Developer Mode & Log Recording** | 7-tap version unlock, in-memory diagnostic buffer, process logcat extraction, live status badges, and 1-tap `.txt` report export via Storage Access Framework. |
| **Buddy Mascot** | Optional resting companion beside the camera: blinks, naps, strolls, tucks away, and reacts to unlocks, charging, timers, transfers and music, with cameos over active notifications. Ported from Vorssaint (GPL-3.0-or-later). |
| **Lock Screen Privacy Guard** | Opt-in lock screen display with customizable sensitive content hiding (App Icon Only vs Full Preview). |

---

## 13 Dynamic Island Modes

Smart Island intelligently categorizes and presents live activities into 13 dedicated modes:

1. **🔔 Notification**: Standard incoming app alerts with full-color launcher icons, action buttons, and **Inline Reply**.
2. **⏱️ Timer**: Live countdown timer with circular/linear progress indicator and pause/resume/stop controls.
3. **⏱️ Stopwatch**: Live elapsed millisecond ticker with lap tracking and pause/reset controls.
4. **🎵 Music**: Media playback with interactive wavy seek bar, ambient album art glow, and repeat/like toggles.
5. **📞 IncomingCall**: Active call timer, caller badge, and waveform animations.
6. **🔋 Battery**: Charging speed & time-until-full, low battery pulsing alerts (&le; 20%), and battery saver mode.
7. **🛵 LiveActivity**: Delivery and ride-sharing updates (Uber, Zomato, Swiggy, Blinkit, Rapido, Ola) with brand accents.
8. **🧭 Navigation**: Turn-by-turn routing indicators, maneuver direction arrows, remaining distance, and ETA.
9. **📊 DownloadUpload**: Real-time transfer progress bars with MB/s transfer speed meters and sync suppression.
10. **📶 Hotspot**: Tethering status, connected device client counter, and 1-tap toggle.
11. **🎧 Bluetooth**: Connected device battery percentage, dual-path battery level extraction, and smooth alternating spring animation between battery gauge and earbuds icon.
12. **🔦 Flashlight**: Active torch status indicator with 1-tap shutoff.
13. **📹 ScreenRecording**: Live recording elapsed duration timer.

---

## Gesture Guide

Smart Island provides dual-tier gesture control covering both the compact collapsed pill and the expanded card:

### Collapsed Pill Gestures

| Gesture on Pill | Default Action | Available Custom Actions |
|---|---|---|
| **Swipe Left** | **Previous Notification** | Next Notification, Previous Track (`skipToPrevious`), Next Track (`skipToNext`), Play/Pause, Dismiss Current, Open App, Floating Window, Notification Shade, **Disabled** |
| **Swipe Right** | **Next Notification** | Previous Notification, Next Track (`skipToNext`), Previous Track (`skipToPrevious`), Play/Pause, Dismiss Current, Open App, Floating Window, Notification Shade, **Disabled** |
| **Swipe Up** | **Dismiss Current** | Dismiss All Notifications, Open Notification Shade, **Disabled** |
| **Swipe Down** | **Expand Island** | Open Notification Shade, Open App, Floating Window, Dismiss Current, **Disabled** |
| **Single Tap** | **Expand Island** | Opens full interactive card (or awakens pill when idle-hidden) |

### Expanded Card Gestures

| Gesture on Card | Action | Description & Feedback |
|---|---|---|
| **Single Tap** | **Open Source App** | Tapping neutral card areas opens the underlying application; tapping outside collapses the card |
| **Quick Swipe Up** | **Dismiss Current** | Flick upward (&ge; 48dp) to dismiss active notification from stack |
| **Hold + Swipe Up** | **Clear ALL Notifications** | Press & hold for **300ms** until **haptic vibration pulse**, then swipe up to dismiss all pending notifications |
| **Swipe Down** | **Floating Window / Shade** | Drag downward (&ge; 48dp) to launch app in freeform floating window or open notification shade |
| **Swipe Left / Right** | **Switch Stack / Tracks** | Navigate smoothly between notifications, timers, and active media sessions |

---

## Architecture

```mermaid
flowchart TD
    subgraph System["Android OS & System Services"]
        A[Status Bar Notifications] --> B[SmartIslandNotificationListenerService]
        Sys[System Broadcasts: Power, Battery, BT, Torch] --> SER[SystemEventReceiver]
    end

    subgraph Parsers["Parsers & Rules Engine"]
        B -->|Filter & Suppress Sync| NF[NotificationFilter]
        B -->|Parse Timers & Stopwatches| TSP[TimerStopwatchParser]
        B -->|Parse Turn-by-Turn Navigation| NP[NavigationParser]
        B -->|Parse Tethering Clients| HU[HotspotUtil]
        OEM[OEM Vendor Detection] --> ODR[OemDeviceRules / Autostart]
    end

    subgraph Repositories["Data Repositories & State"]
        NF --> NR[SmartIslandNotificationRepository]
        TSP --> NR
        NP --> NR
        HU --> NR
        SER --> NR
        ODR --> NR
        B -->|Persist Alerts| NHD[NotificationHistoryRepository / SQLite DB]
        DS[(AndroidX DataStore Settings)] --> SR[SmartIslandSettingsRepository]
        GH[GitHub Releases API] -->|User Opt-In| GHS[GitHubApiService]
    end

    subgraph Presentation["UI & Overlay Presentation"]
        NR --> VM[IslandViewModel]
        SR --> VM
        VM --> OS[SmartIslandOverlayService]
        OS --> WM[WindowManager Overlay]
        WM -->|Compose State| OV[IslandOverlayView / IslandExpandedContent]
        OV -->|Inline Reply Focus Switch| OS
        OV -->|Dual-Tier Gesture Engine| VM
    end
```

---

## Tech Stack

| Layer | Technology |
| --- | --- |
| **Language** | Kotlin 2.0+ (JVM Target 17) |
| **UI Framework** | Jetpack Compose & Material 3 Expressive Design System |
| **Architecture** | Clean Architecture, MVI / Repository Pattern |
| **Dependency Injection** | Dagger Hilt |
| **State & Async** | Kotlin Coroutines, StateFlow, Atomic Mutex |
| **Local Persistence** | AndroidX DataStore Preferences & SQLite (`NotificationHistoryDbHelper`) |
| **System Services** | NotificationListenerService, Accessibility Floating Service, WindowManager Overlays, Shizuku |
| **Network & Updates** | In-app GitHub Release API via `GitHubApiService` (strictly opt-in via `allowNetworkChecks`) |
| **Build & CI** | Gradle Wrapper, AGP 9.0+, GitHub Actions CI (@v4) |

---

## Building from Source

### Prerequisites

- **Android SDK**: API 26 through API 36
- **JDK**: Java Development Kit 17
- **Build Tools**: Gradle 8.13+ (included via Gradle Wrapper)

### 1. Clone the repository

```bash
git clone https://github.com/agupta07505/SmartIsland.git
cd SmartIsland
```

### 2. Build the debug APK

**On Windows:**
```powershell
.\gradlew.bat assembleDebug
```

**On macOS / Linux:**
```bash
./gradlew assembleDebug
```

### 3. Run Unit Tests & Lint Checks

```bash
./gradlew testDebugUnitTest lintDebug
```

### 4. Install on a connected Android device

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.agupta07505.smartisland/.MainActivity
```

---

## Privacy & Permissions

Smart Island is strictly privacy-first:

| Permission | Purpose |
| --- | --- |
| `SYSTEM_ALERT_WINDOW` | Draws the floating dynamic island overlay above other applications. |
| `BIND_NOTIFICATION_LISTENER_SERVICE` | Intercepts notification metadata to display alerts, media playback, timers, and system states. |
| `FOREGROUND_SERVICE` & `SPECIAL_USE` | Keeps the overlay service active in the background without being killed by the OS. |
| `ACCESS_RESTRICTED_SETTINGS` (Optional Shizuku) | Grants 1-tap automated permissions without manual menu navigation. |
| `INTERNET` (Opt-in) | Used **strictly** for user-opted GitHub release version checks (`allowNetworkChecks`). **Zero** notification data, metadata, or telemetry is ever transmitted over the network. |

> [!NOTE]
> All notification processing and notification history are stored 100% locally on-device. See [PRIVACY.md](PRIVACY.md).

---

## Security & Transparency

- **Security Verification**: Inspect packages via [VirusTotal](https://www.virustotal.com/gui/file/c07da408e7fa3e3fdeb25ec6415af07f1d56eff099840221138e9364c6065102/details) or audit automatic GitHub Actions CI builds.
- **Reproducible CI**: All continuous integration workflows build from public source using clean GitHub-hosted runners.
- **Security Policy**: See [SECURITY.md](SECURITY.md) for vulnerability disclosure guidelines.

---

## Community & Support

- **Telegram Community**: Join our group at [telegram.me/SmartIslandApp](https://telegram.me/SmartIslandApp) to discuss features, report issues, and interact with the team.
- **Changelog**: View complete release history in [CHANGELOG.md](CHANGELOG.md).
- **Contributing**: Contributions are warmly welcomed! Please read [CONTRIBUTING.md](CONTRIBUTING.md) and [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md).

---

## License

Smart Island is licensed under the [GNU General Public License v3.0](LICENSE).  
Copyright (C) 2026 **Animesh Gupta**.

### Acknowledgements

The **Buddy** feature is a Kotlin port of the island mascot from [Vorssaint](https://github.com/vorssaint/vorssaint-utils) (Copyright (C) 2026 Vorssaint, GPL-3.0-or-later). Ported files keep that notice. The Vorssaint name, logo and icon are not used; see its `TRADEMARKS.md`.
