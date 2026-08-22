<h1 align="center">Smart Island</h1>

<p align="center">
  A lightweight, privacy-first Android overlay that turns notifications, calls, media playback, battery states, timers, stopwatches, and system activities into a floating glanceable Dynamic Island.
</p>

<p align="center">
  <strong>Current release: v5.1.0</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Android-8%2B-3DDC84?logo=android&logoColor=white" alt="Android 8+" />
  <img src="https://img.shields.io/badge/Kotlin-JVM%2017-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin JVM 17" />
  <img src="https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white" alt="Jetpack Compose Material 3" />
  <img src="https://img.shields.io/badge/License-GPLv3-blue.svg" alt="GNU GPLv3" />
  <img src="https://img.shields.io/github/downloads/agupta07505/SmartIsland/total?color=2ea44f&logo=github" alt="Total Downloads" />
  <a href="https://www.virustotal.com/gui/file/c07da408e7fa3e3fdeb25ec6415af07f1d56eff099840221138e9364c6065102/details" target="_blank">
    <img src="https://img.shields.io/badge/VirusTotal-Scan-007ACC" alt="VirusTotal Scan" />
  </a>
</p>

<p align="center">
  <a href="#downloads--safety">Downloads</a> |
  <a href="#screenshots">Screenshots</a> |
  <a href="#features">Features</a> |
  <a href="#gesture-guide">5-Gesture Guide</a> |
  <a href="#getting-started">Getting Started</a> |
  <a href="#privacy-and-permissions">Privacy</a> |
  <a href="ROADMAP.md">Roadmap</a> |
  <a href="CHANGELOG.md">Changelog</a> |
  <a href="https://telegram.me/SmartIslandApp">Telegram Community</a>
</p>

---

## Overview

Smart Island is an open-source Android application that renders a fluid, dynamic floating pill near your device's camera notch or status bar. It intercepts system notifications, live activities, media playback, calls, charging events, hotspot connections, navigation instructions, timers, stopwatches, and battery alerts locally—expanding into rich interactive cards with zero cloud tracking.

The project is designed to be 100% transparent, hackable, and privacy-conscious: all notification processing and history storage are strictly on-device, preferences are stored in local DataStore, and network access is solely opt-in for in-app release checks.

---

## Downloads & Safety

* **Download APK**: Obtain the pre-compiled APK directly from the [GitHub Releases (v5.1.0)](https://github.com/agupta07505/SmartIsland/releases/latest) page.
* **Total Downloads**: ![Total Downloads](https://img.shields.io/github/downloads/agupta07505/SmartIsland/total?color=2ea44f&logo=github)
* **Telegram Channel**: Join our active community at [telegram.me/SmartIslandApp](https://telegram.me/SmartIslandApp) to suggest features, get support, and discuss updates.
* **Security Verification**: To ensure complete safety, inspect packages via [VirusTotal](https://www.virustotal.com/gui/file/c07da408e7fa3e3fdeb25ec6415af07f1d56eff099840221138e9364c6065102/details) or review automatic GitHub Actions CI builds.

---

## Screenshots

<p align="center">
  <img src="/assets/screenshots/01_studio_simulation_lab.jpg" width="24%" alt="Studio Simulation Lab" />
  <img src="/assets/screenshots/02_notch_layout_presets.jpg" width="24%" alt="Notch & Layout Presets" />
  <img src="/assets/screenshots/03_precision_sizing_controls.jpg" width="24%" alt="Precision Sizing & Drop Shadow" />
  <img src="/assets/screenshots/04_features_hub.jpg" width="24%" alt="Features & Settings Hub" />
</p>

<p align="center">
  <img src="/assets/screenshots/05_notifications_privacy_rules.jpg" width="24%" alt="Notifications & Privacy Rules" />
  <img src="/assets/screenshots/06_live_activities_navigation.jpg" width="24%" alt="Live Activities & Navigation" />
  <img src="/assets/screenshots/07_app_shortcuts_launcher.jpg" width="24%" alt="App Shortcuts Launcher" />
  <img src="/assets/screenshots/08_feature_accent_colors.jpg" width="24%" alt="Feature Accent Colors Studio" />
</p>

<p align="center">
  <img src="/assets/screenshots/09_custom_rgb_picker_flashlight.jpg" width="24%" alt="Custom RGB Color Picker & Flashlight" />
  <img src="/assets/screenshots/10_system_hub.jpg" width="24%" alt="System, Gestures & About Hub" />
  <img src="/assets/screenshots/11_expanded_music_card.jpg" width="24%" alt="Expanded Music Player Card" />
  <img src="/assets/screenshots/12_about_smartisland.jpg" width="24%" alt="About Smart Island" />
</p>

<p align="center">
  <img src="/assets/screenshots/13_support_feedback.jpg" width="24%" alt="Support & Community Feedback" />
  <img src="/assets/screenshots/14_gesture_guide_5gestures.jpg" width="24%" alt="5 Gestures Step-by-Step Guide" />
  <img src="/assets/screenshots/15_permissions_setup_center.jpg" width="24%" alt="Permissions & Shizuku Setup" />
  <img src="/assets/screenshots/16_homescreen_music_floating.jpg" width="24%" alt="Home Screen Floating Music Island" />
</p>

---

## Features

| Feature Area | Capabilities & Details |
| --- | --- |
| **Material 3 Expressive UI** | Modern MD3 color tokens, dynamic wallpaper color adaptation (Android 12+), rounded shapes, and fluid physics. |
| **Inline Reply & IME Focus** | Direct text reply input right inside the expanded Island notification card with dynamic WindowManager focus switching, soft keyboard integration, and `RemoteInput` dispatch. |
| **Timer & Stopwatch Modes** | Intelligent clock notification parsing for Google Clock, Samsung Clock, MIUI/HyperOS, ColorOS, and Huawei Clock. Collapsed countdown/elapsed badges, expanded circular/linear timer progress, live millisecond stopwatch ticker, and interactive pause/resume/lap/reset controls. |
| **SQLite Notification History** | Persistent on-device SQLite database storing past notifications with real-time search, app filter chips, category filters, full notification details sheet, and swipe-to-delete. |
| **OEM Device Rules Engine** | Tailored background protection and autostart management for Xiaomi/HyperOS, Samsung OneUI, OnePlus/Oppo/Realme (ColorOS/OxygenOS), Huawei/Honor, Vivo/iQOO, and Asus to prevent aggressive background kills. |
| **In-App GitHub Release Checker** | In-app GitHub update checks and changelog insights with an explicit user toggle (`allowNetworkChecks`) and strict offline mode fallback. |
| **Full-Color App Icons** | Extracted launcher icons with LRU caching for crisp, authentic app icons in both collapsed pill and expanded cards. |
| **Battery Modes (Low & Saver)** | Dynamic Battery island displaying Green for Charging, Pulsing Red for Low Battery (&le; 20%), and Warm Amber for Battery Saver ON with live battery percentage badge. |
| **5-Gesture Control Engine** | Complete intuitive gesture system: Single Tap, Quick Swipe Up, Hold + Swipe Up (300ms haptic), Swipe Down (Floating window), and Horizontal Swipe Left/Right. |
| **Split Island Multi-State** | Secondary auxiliary bubble for concurrent background activities (e.g. Music + Hotspot, Call + Bluetooth, Timer + Music). |
| **Wavy Music Player** | Real-time audio waveform scrubber, ambient album artwork background glow, track metadata, and dynamic color customization. |
| **Message Sync Suppression** | Automatically filters background message polling notifications (e.g. Snapchat, WhatsApp sync) to prevent download mode hijacking. |
| **Live Activity Tracking** | Real-time delivery and rideshare tracking (Uber, Swiggy, Zomato, Blinkit, Rapido, Ola) with brand colors. |
| **Turn-by-Turn Navigation** | Live turn navigation indicators, maneuver direction arrows, remaining distance, and ETA. |
| **Download & Upload Progress** | Real-time transfer speed meters (MB/s), progress bars, and animated icons. |
| **Wi-Fi Hotspot Monitor** | Live tethering client counter, SSID badge, data usage, and quick turn-off action. |
| **Flashlight & Screen Recording** | Active torch toggle card and live screen recording timer overlay. |
| **App Shortcuts Launcher** | Quick-launch grid with up to 8 pinned apps or auto-filled recent applications. |
| **Custom RGB Color Studio** | Fine-grained Red, Green, Blue slider color picker with live Hex preview for all 13 dynamic modes. |
| **Notch & Layout Presets** | Instant 1-tap calibration for Center Hole, Wide Island, Left Corner, Right Corner, and Compact Pill. |
| **Precision Sizing Controls** | Millimeter-accurate sliders for Width, Height, X Offset, Y Offset, Corner Radius, and Drop Shadows. |
| **Shizuku 1-Tap Auto Setup** | Automated permission grants for Restricted Settings, Usage Access, Overlay, and Battery Optimization. |
| **Lock Screen Privacy Guard** | Opt-in lock screen display with customizable sensitive content hiding (App Icon Only vs Full Preview). |
| **Landscape Auto-Hide** | Automatically removes the floating overlay during full-screen landscape video playback and gaming. |

---

## 13 Dynamic Island Modes

Smart Island intelligently categorizes and presents live activities into 13 dedicated modes:

1. **Notification**: Standard incoming app alerts with full-color launcher icons, action buttons, and **Inline Reply**.
2. **Timer**: Live countdown timer with circular/linear progress indicator and pause/resume/stop controls.
3. **Stopwatch**: Live elapsed millisecond ticker with lap tracking and pause/reset controls.
4. **Music**: Media playback with interactive wavy seek bar, ambient album art glow, and repeat/like toggles.
5. **IncomingCall**: Active call timer, caller badge, and waveform animations.
6. **Battery**: Charging speed & time-until-full, low battery pulsing alerts (&le; 20%), and battery saver mode.
7. **LiveActivity**: Delivery and ride-sharing updates (Uber, Zomato, Swiggy, Blinkit, Rapido, Ola) with brand accents.
8. **Navigation**: Turn-by-turn routing indicators, maneuver direction arrows, remaining distance, and ETA.
9. **DownloadUpload**: Real-time download and upload progress bars with MB/s transfer speed meters.
10. **Hotspot**: Tethering status, connected device client counter, and 1-tap toggle.
11. **Bluetooth**: Connected device battery percentage and device status.
12. **Flashlight**: Active torch status indicator with 1-tap shutoff.
13. **ScreenRecording**: Live recording elapsed duration timer.

---

## Gesture Guide

Smart Island features a comprehensive gesture engine to manage notifications, multitasking, and media effortlessly:

| # | Gesture | Action | Instructions & Feedback |
|---|---|---|---|
| **1** | **Single Tap / Click** | **Expand & Collapse** | Tap once on the collapsed pill to open rich details; tap anywhere on the screen background to collapse back. *(Cutout region remains active when idle-hidden)* |
| **2** | **Quick Swipe Up** | **Dismiss Current** | Touch the expanded card and flick upward by &ge; 48dp to dismiss the active notification from the stack. |
| **3** | **Hold + Swipe Up** | **Clear ALL Notifications** | Press & hold the expanded card for **300ms** until a **haptic vibration pulse** occurs, then swipe up to dismiss all pending notifications simultaneously. |
| **4** | **Swipe Down** | **Floating Window** | Drag downward by &ge; 48dp on the expanded card to launch the application into a freeform floating window overlay. *(Requires Shizuku or OEM freeform)* |
| **5** | **Swipe Left / Right** | **Switch Notification Stack** | Swipe horizontally across expanded cards to navigate smoothly between multiple active notifications, timers, and media sessions. |

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
        OV -->|5-Gesture Engine| VM
    end
```

---

## Tech Stack

| Layer | Technology |
| --- | --- |
| **Language** | Kotlin 2.0+ (JVM Target 17) |
| **UI Framework** | Jetpack Compose & Material 3 Expressive Design System |
| **Architecture** | Clean Architecture, MVI/Repository Pattern |
| **Dependency Injection** | Dagger Hilt |
| **State & Async** | Kotlin Coroutines, StateFlow, Atomic Mutex |
| **Local Persistence** | AndroidX DataStore Preferences & SQLite (`NotificationHistoryDbHelper`) |
| **System Services** | NotificationListenerService, Accessibility Floating Service, WindowManager Overlays, Shizuku |
| **Network & Updates** | In-app GitHub Release API via `GitHubApiService` (strictly opt-in via `allowNetworkChecks`) |
| **Build & CI** | Gradle Wrapper, AGP 9.0+, GitHub Actions CI (@v4) |

---

## Requirements

- **Android Version**: Android 8.0+ (API 26 through API 36)
- **JDK**: Java Development Kit 17
- **Permissions**: Overlay (`SYSTEM_ALERT_WINDOW`) and Notification Listener access

---

## Getting Started

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

### 3. Install on a connected device
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.agupta07505.smartisland/.MainActivity
```

---

## Privacy And Permissions

Smart Island is strictly privacy-first:

| Permission | Purpose |
| --- | --- |
| `SYSTEM_ALERT_WINDOW` | Draws the floating dynamic island overlay above other applications. |
| `BIND_NOTIFICATION_LISTENER_SERVICE` | Intercepts notification metadata to display alerts, media playback, timers, and system states. |
| `FOREGROUND_SERVICE` & `SPECIAL_USE` | Keeps the overlay service active in the background without being killed by the OS. |
| `ACCESS_RESTRICTED_SETTINGS` (Optional Shizuku) | Grants 1-tap automated permissions without manual menu navigation. |
| `INTERNET` (Opt-in) | Used **strictly** for user-opted GitHub release update checks (`allowNetworkChecks`). **Zero** notification data, metadata, or telemetry is ever transmitted over the network. |

*Note: All notification processing and notification history are stored 100% locally on-device. See [PRIVACY.md](PRIVACY.md).*

---

## Contributing

Contributions are warmly welcomed! Please read [CONTRIBUTING.md](CONTRIBUTING.md) before submitting pull requests, and use our issue templates for bug reports or feature suggestions.

---

## License

Smart Island is licensed under the [GNU General Public License v3.0](LICENSE).  
Copyright (C) 2026 **Animesh Gupta**.

