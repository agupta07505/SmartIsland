# Smart Island Product Roadmap

Last updated: August 26, 2026

## Product direction

Smart Island should become the most reliable, privacy-first, and customizable glanceable activity overlay for Android. It makes important live information easier to act on without replacing the notification shade or collecting personal data.

The roadmap follows three core principles:

1. **Reliability before feature count.** Overlay, notification, media, call, timer, stopwatch, and battery behavior must remain rock-solid across Android versions and major OEM ROMs.
2. **Local by default.** Notification content, history, and preferences stay strictly on-device. Zero analytics, zero tracking, and zero cloud lock-in.
3. **Fast, fluid, and unobtrusive.** The island must use negligible battery, avoid blocking touches, and provide instant haptic and visual feedback.

---

## Current baseline (v5.2.0)

Smart Island currently provides:

- A Compose-based floating overlay on Android 8+ (API 26 through API 36)
- Complete **Material 3 Expressive** design system with Android 12+ wallpaper dynamic color adaptation
- **Island Background Opacity & Transparency Controls**: Continuous opacity adjustment (20% to 100%) with 4 quick presets (Solid, Dark, Glass, Clear)
- **High-Performance Virtualized Notification History**: Ultra-fast `LazyColumn` log with `AppIconMemoryCache`, search bar, date grouping, and **Delete by App** bulk management
- **13 Dynamic Island Modes**:
  - `Notification` (with full-color app launcher icons, LRU caching, and **Inline Reply**)
  - `Timer` (live countdown, circular/linear progress bar, and pause/resume/stop controls)
  - `Stopwatch` (live millisecond ticker, lap counter, lap history list, and reset controls)
  - `IncomingCall` (call timer, caller badge & waveform animations)
  - `Music` (wavy audio scrubber, heart/repeat actions, ambient album art glow)
  - `Battery` (*Charging*, *Low Battery &le; 20%*, *Battery Saver ON*)
  - `LiveActivity` (real-time delivery & ride tracking with app brand colors)
  - `Navigation` (turn-by-turn routing arrows, distance, and ETA)
  - `DownloadUpload` (transfer speeds MB/s, progress meters, sync suppression)
  - `Hotspot` (active tethering client count & quick turn-off)
  - `Bluetooth` (device battery & connectivity)
  - `Flashlight` (torch state indicator & toggle)
  - `ScreenRecording` (live elapsed timer)
- **Inline Reply & Soft-Keyboard WindowManager Focus**: Seamless IME typing right inside the island with auto-collapse pause
- **OEM Device Rules Engine**: Vendor-specific autostart & battery optimization protection for Xiaomi/HyperOS, Samsung OneUI, OnePlus/Oppo/Realme, Huawei, Vivo, and Asus
- **In-App GitHub Release Checker**: Optional version update insights and changelogs with strict `allowNetworkChecks` privacy guard and offline mode fallback
- **Split Island Multi-State Pill** for concurrent background states
- **5-Gesture Control Engine with Responsive Interactive Guide**: Single Tap, Quick Swipe Up, Hold + Swipe Up (300ms haptic), Swipe Down (Floating window), and Swipe Left/Right (Pager)
- **Message Sync Filtering**: Background chat polling (Snapchat, WhatsApp) excluded from download mode
- **Custom RGB Slider Color Picker**: Fine-grained color customization for all 13 modes
- **Grouped Settings Hub Architecture** with minimal Category Hub Cards
- **Landscape orientation detection** for automatic overlay hiding during full-screen apps
- **Shizuku 1-Tap Auto Setup** for automated system permission granting
- **Lock screen privacy guard** with sensitive notification content protection
- **100% Local Processing** and DataStore persistence
- **Strict Lint & Stable @v4 GitHub Actions CI/CD** pipeline

---

## Roadmap at a glance

| Phase | Target window | Theme | Exit outcome |
| --- | --- | --- | --- |
| **v4.0** | Jul 2026 | Material 3 UI & Grouped Settings | Modern MD3 theme engine, wallpaper dynamic colors, minimal clean UI, and Category Hub settings *(Released)* |
| **v5.0.0** | Aug 2026 | Battery Saver, App Icons & 5-Gesture Engine | Low Battery/Saver modes, full-color app launcher icons, sync filtering, 5 gestures text guide, and stable CI *(Released)* |
| **v5.1.0** | Aug 2026 | Inline Reply, Timers, History & OEM Engine | Direct inline text replies, Timer & Stopwatch island modes, SQLite history hub, OEM background rules engine *(Released)* |
| **v5.2.0** | Aug 2026 | Opacity Customization, History Virtualization & Stability | Island opacity controls (20-100%), Delete-by-App history, virtualized LazyColumn, interactive gesture guide, and multi-device crash fixes *(Released)* |
| **v5.3** | Q4 2026 - Q1 2027 | Face Unlock, Custom Sounds & Dynamic Widgets | Face Unlock animation overlay, dynamic Calendar/Reminder glance widgets, and customizable micro-sound packs |
| **v5.4** | Q1 2027 | System Profiles & Floating Window Polish | Contextual environment profiles (Gaming, Work, Theater, Sleep), advanced Shizuku automation, and freeform docking presets |
| **v6.0** | Q2 2027 | Ecosystem & Local Cross-Device Sync | Plugin extension architecture and local cross-device status sharing (Bluetooth LE / LAN) with zero cloud dependencies |

*Dates are planning targets. A phase moves forward only after its release gates and automated CI suites are fully validated.*

---

## Released — v5.2.0: Opacity Controls, History Virtualization & Multi-Device Crash Fixes

Released August 26, 2026.

- **Island Opacity & Transparency Setting**: Added continuous opacity slider (20% to 100%) with 4 quick 1-tap presets (Solid, Dark, Glass, Clear) and live percentage indicators across Appearance Studio and Position settings.
- **Virtualized Notification History & Delete by App**: Overhauled history screen to use top-level `LazyColumn` virtualization with background `AppIconMemoryCache` and single-tap bulk deletion by application.
- **Interactive Gesture Guide Overhaul**: Replaced cramped single-row badge layout with responsive 2-column clickable cards that immediately switch to the selected gesture instructions.
- **Multi-Device Crash Hardening**: Null-safe system service lookups (KeyguardManager, NotificationManager, PowerManager), battery receiver export flag fixes for Android 14+, and regex state validation in LiveActivityParser.

---

## Released — v5.1.0: Inline Reply, Timer & Stopwatch, Notification History & OEM Rules Engine

Released August 22, 2026.

- **Inline Reply & Soft-Keyboard WindowManager Focus**: Direct typing inside expanded notification cards with dynamic focus switching (`FLAG_NOT_FOCUSABLE` to `FLAG_ALT_FOCUSABLE_IM`), auto-collapse suppression while typing, and `RemoteInput` pending intent dispatch.
- **Timer & Stopwatch Dynamic Island Modes**: Deep parsing across Google, Samsung, Xiaomi/HyperOS, ColorOS, and Huawei clock notifications. Dedicated collapsed glyphs, expanded progress views, live millisecond tickers, and pause/resume/lap/reset controls.
- **Persistent SQLite Notification History**: On-device SQLite database storing past notifications with real-time search, app filtering chips, category filters, notification details sheet, and swipe-to-delete.
- **OEM Device Rules Engine**: Vendor-specific autostart and background protection rules for Xiaomi/HyperOS, Samsung OneUI, OnePlus/Oppo/Realme (ColorOS/OxygenOS), Huawei/Honor, Vivo/iQOO, and Asus to eliminate aggressive background kills.
- **In-App GitHub Release Checker & Network Privacy Guard**: In-app GitHub update checks and changelog insights with an explicit user toggle (`allowNetworkChecks`) and strict offline mode fallback.
- **Hotspot & Navigation Parser Enhancements**: Regex parsing improvements for tethering clients and turn-by-turn navigation instructions.
- **Expanded Test Suite**: Added 52+ unit tests covering new clock parsers, OEM device rules, API services, SQLite entities, and ViewModels.

See [CHANGELOG.md](CHANGELOG.md) for the complete release notes.

---

## Released — v5.0.0: Battery Saver, App Icons, Split Island & 5-Gesture Engine

Released August 15, 2026.

- **Low Battery & Battery Saver Modes**: Added pulsing red `BatteryAlert` for low battery (&le; 20%) and warm amber `BatterySaver` glyph with live percentage badge.
- **Full-Color App Launcher Icons**: Restored `loadAppIconBitmap(packageName)` with `packageManager.getApplicationIcon(packageName)` and LRU caching for authentic app branding.
- **Message Sync Notification Filtering**: Implemented `NotificationFilter.isMessageSyncNotification` to prevent background sync notifications (Snapchat, WhatsApp) from falsely triggering download mode.
- **5-Gesture Controls & Text Guide**: Revamped `GesturesSection.kt` with step-by-step instructions, haptic vibration indicators, and quick reference chips for Single Tap, Quick Swipe Up, Hold + Swipe Up, Swipe Down, and Horizontal Swipe Left/Right.
- **Horizontal Pager Snapping Stability**: Synchronized expanded notification pager with `pagerState.settledPage` and guarded with `!pagerState.isScrollInProgress` to eliminate swipe lockups.
- **Flashlight & Screen Recording Modes**: Added `IslandMode.Flashlight` and `IslandMode.ScreenRecording` with live timer and toggle controls.
- **Custom RGB Color Picker**: Added fine-grained Red, Green, Blue slider dialog with live hex code preview for all 11 dynamic island modes.
- **Split Island Architecture**: Added secondary bubble pill support for concurrent background events.
- **GitHub Actions CI/CD Fix**: Updated all GitHub actions to stable official `@v4` and added lint suppressions for flawless builds.
- **Refreshed Screenshots Gallery**: Added 16 high-resolution screenshots showcasing the entire v5.0.0 feature set.

---

## Released — v4.0: Complete Material 3 UI Overhaul & Grouped Settings

Released July 27, 2026.

- **Material 3 Expressive Design Overhaul**: Redesigned UI with modern MD3 color tokens, rounded shapes, and clean minimal visual aesthetics.
- **Android 12+ Dynamic Wallpaper Colors**: Integrated `dynamicLightColorScheme` and `dynamicDarkColorScheme`.
- **Grouped Settings Architecture**: Consolidated 8 standalone settings sections into 4 Category Hub Cards.
- **Wi-Fi Hotspot Monitor**: Added `HotspotUtil` and `HotspotExpanded` for active tethering monitoring.
- **Live Activity Engine**: Added `LiveActivityParser` and `LiveActivityExpanded` for delivery/ride tracking.
- **Download/Upload Progress Mode**: Real-time progress bars with transfer speeds (MB/s).
- **Turn-by-Turn Navigation Mode**: Instruction cards, distance, ETA, and maneuver arrows.
- **Per-App Notification & Sound Controls**: Granular toggles under `NotificationsAndPrivacySection`.

---

## Future Roadmap Details

### Phase 5.2 — Face Unlock, Custom Sounds & Dynamic Widgets (Q4 2026 - Q1 2027)

**Goal:** Expand glanceable indicators to hardware authentication, ambient widgets, and micro-interactions.

- **Face Unlock Integration**: Detect keyguard face recognition events via Shizuku/Accessibility and render smooth animated face-scan rings in the island.
- **Calendar & Reminder Widgets**: Glance cards for upcoming meetings, agendas, and alarms.
- **Custom Sound Packs**:
  - Assign customizable micro-sound effects (subtle pop, bubble, mechanical click) to island expand and collapse animations.
- **App Whitelist / Blacklist Rules**:
  - Granular per-app rules for minimum notification importance, vibration suppression, and custom expanded card heights.

### Phase 5.3 — System Profiles & Floating Window Polish (Q1 2027)

**Goal:** Provide contextual island automation and desktop-class multitasking.

- **Contextual System Profiles**:
  - *Gaming Profile*: Auto-hide all non-urgent notifications; show only low-battery alerts and high-priority callers in compact pill.
  - *Work / Focus Profile*: Prioritize calendar, live activities, and direct messaging; suppress social media alerts.
  - *Sleep / Night Profile*: Dim island brightness, disable ambient glow, and enforce silent animations.
- **Enhanced Freeform Window Docking**:
  - Preset window sizes (Quarter screen, Half screen, Pip) when triggering Swipe Down gesture.
  - Smooth animation morphing from expanded island card directly into Android Freeform window.

### Phase 6.0 — Ecosystem & Local Cross-Device Status (Q2 2027)

**Goal:** Enable modular community widgets and private multi-device glanceability.

- **Plugin Extension Architecture**:
  - Expose a secure, local IPC API for third-party Android apps to feed live activity status into Smart Island.
- **Local Cross-Device Status Sync**:
  - Peer-to-peer sync over Bluetooth Low Energy (BLE) or local Wi-Fi to view tablet or secondary phone battery, call, or media status on your primary device.
  - Strictly zero cloud servers or remote accounts required.

