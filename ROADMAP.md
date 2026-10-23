# Smart Island Product Roadmap

Last updated: October 18, 2026

## Product direction

Smart Island should become the most reliable, privacy-first, and customizable glanceable activity overlay for Android. It makes important live information easier to act on without replacing the notification shade or collecting personal data.

The roadmap follows three core principles:

1. **Reliability before feature count.** Overlay, notification, media, call, timer, stopwatch, and battery behavior must remain rock-solid across Android versions and major OEM ROMs.
2. **Local by default.** Notification content, history, and preferences stay strictly on-device. Zero analytics, zero tracking, and zero cloud lock-in.
3. **Fast, fluid, and unobtrusive.** The island must use negligible battery, avoid blocking touches, and provide instant haptic and visual feedback.

---

## Current baseline (v7.5.0)

Smart Island currently provides:

- A Compose-based floating overlay on Android 8+ (API 26 through API 36)
- **Idle Widgets**: Cutout rotates through Live Clock, Date, Weather, Wind, Battery, Next Alarm, Net Speed, unread Notifications and Do Not Disturb / Modes widgets, with an hourly tick, Day Moments glyphs, smooth or LED tick styles, work / night / charging schedules, drag-to-reorder and pin-first
- **Weather, Net Speed & Calendar Widgets**: Auto, manual or last-known location, Open-Meteo or a custom provider, rain, humidity, UV, sun, air quality and hourly rows; net speed with threshold, Wi-Fi or mobile filter and a daily usage counter; next calendar event on the date widget
- **Face Unlock Animation**: Face scan and fingerprint animations over the cutout for face unlocks, finger unlocks and app lock prompts
- **Per-Event Switches & Lock Screen Presets**: 32 events, each with its own on/off, auto-expand and hide-on-lock-screen switch, plus Show all, Messages only, Calls only and Off presets
- **Quick-Launch Window & Utilities**: Apps, Recents and a 2x4 Utilities grid with resizable app widgets, a pinned apps picker, and floating windows through Android freeform or LMO Freeform
- **Landscape Overhaul**: Independent portrait and landscape profiles, Landscape Lab, rotation veil and a split island with a beside picker
- **Richer Notification Cards**: Reply drafts that survive interruptions, same-app sibling picker, expandable text, picture reader and selector, and silent notifications skipped when cycling
- **Gesture Tuning**: New swipe actions (next stored event, next same-app card, long-press to hide the pill), plus gesture sensitivity and haptic strength settings
- **Media & Call Controls**: Audio output switcher and artwork tint on the music card, call mic mute and earpiece, speaker or Bluetooth routing
- **Material You & Liquid Glass Theming**: Material 3 palette from one accent or the wallpaper, optional liquid glass theme, bundled Lora, Inter Display and Poppins fonts, custom TTF/OTF fonts, font mixing and a gooey bubble merge style
- **Settings Search & Exempt Apps**: In-settings search for every option, and per-orientation lists of apps that switch the island off (or make the pill static in portrait) while in the foreground
- **Shizuku & Diagnostics Hardening**: Broader Shizuku detection, one-tap sensitive notification grant, hotspot control, a diagnostic log that survives process kills, and a main-thread watchdog
- **Parser & Anti-Spam Improvements**: Sturdier timer, stopwatch, navigation, call and transfer detection, plus per-event cooldown for repeating system events
- **Baseline Profile Module**: Bundled `:baselineprofile` module and CI workflow for ahead-of-time compiled startup and animation paths
- **Dedicated App Updates & Downloads Hub**: Specialized settings screen featuring live Total Downloads counter (15,648+), Current Version line, 1-tap GitHub update checker, in-app changelogs viewer, Top Contributors gallery, and Recent Commits feed
- **In-Pill Swipe Gestures & Media Controls**: Compact collapsed pill independent gesture engine supporting horizontal swipes for next/prev song skipping (`skipToNext`/`skipToPrevious`) or notification cycling, vertical swipes, and individual gesture disable options
- **Selective App Alerts & Sound Manager Modal**: On-demand selective app management modal preventing UI stutter by loading packages selectively, with search, per-app alert toggles, custom ringtone sound toggling, and island exclusion
- **Configurable Companion Circle Position**: Place the multi-tasking companion circle on either the **Left** or **Right** of the pill with anti-overlap layout geometry clamping for corner and punch-hole displays
- **iPhone Notch Mode**: Authentic iPhone-style top-docked notch (`y = 0`) with rounded bottom corners, downward expansion, and complete suppression of the split right companion circle
- **Developer Mode & In-App Log Recording**: 7-tap version unlock, in-memory diagnostic buffer (3,000 entries), process logcat capture, live status monitor, and Scoped Storage `.txt` export
- **Full Settings Backup & Restore System**: Scoped Storage JSON export/import via Android SAF, atomic single-transaction DataStore restore, factory reset, and bounds clamping
- **Bluetooth Battery & Earbuds Alternating Animation**: Dual-path battery percentage extraction via intent extra and reflection, with a smooth 3-second spring transition between live battery gauge and earbuds icon in both collapsed and expanded states
- **Intelligent Notification Cooldown & Anti-Spam Engine**: Sliding window burst detection with customizable quiet duration (1–30 min), trigger threshold (2–10 msgs), and per-app whitelist exclusions
- **App Shortcuts Launcher Master Control**: Complete user enable/disable toggle for quick-launch apps grid on empty island with zero background overhead when disabled
- **Ultra-Fluid Spring Physics Engine**: Unified 520f spring morphing, instant 190ms cross-fades, and 220ms overlay window resize
- **Tap Anywhere to Open Target App**: Neutral taps on expanded cards open source apps with ongoing music protection
- **Auto-Hide Pill Inactivity Timer**: Configurable timeout (1s–60s) with 1-tap presets and tap-to-reveal gesture
- **Landscape Mode Visibility Control**: Opt-in toggle to keep island active and repositioned in landscape orientation
- **Island Background Opacity & Transparency Controls**: Continuous opacity adjustment (20% to 100%) with 4 quick presets (Solid, Dark, Glass, Clear)
- **High-Performance Virtualized Notification History**: Ultra-fast `LazyColumn` log with `AppIconMemoryCache`, search bar, date grouping, and **Delete by App** bulk management
- **14 Dynamic Island Modes**:
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
  - `Bluetooth` (dual-path battery extraction & smooth spring-animated alternation between battery info and earbuds icon)
  - `Flashlight` (torch state indicator & toggle)
  - `ScreenRecording` (live elapsed timer)
  - `AudioRecording` (voice recorder status and elapsed timer)
- **Inline Reply & Soft-Keyboard WindowManager Focus**: Seamless IME typing right inside the island with auto-collapse pause
- **OEM Device Rules Engine**: Vendor-specific autostart & battery optimization protection for Xiaomi/HyperOS, Samsung OneUI, OnePlus/Oppo/Realme, Huawei, Vivo, Asus, and Nubia/RedMagic
- **Split Island Multi-State Pill** for concurrent background states
- **13 High-Resolution Screenshots & Visual Documentation Refresh**: Complete asset showcase across all settings and overlay modes
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
| **v6.0.0** | Aug 2026 | Ultra-Fluid Spring Physics, Tap-to-Open & Auto-Hide | Synchronized 520f spring physics, tap-to-open apps, inactivity auto-hide timer, landscape mode visibility, and flicker elimination *(Released)* |
| **v7.0.0** | Sep 2026 | In-Pill Gestures, App Updates Hub, Selective Alerts & Design System | In-Pill gestures & media skip, App Updates & Downloads Hub (15,648+ downloads), selective app alerts modal, unified #D84315 theme, companion placement, iPhone notch mode, developer diagnostics, backup & restore, 13 new screenshots *(Released)* |
| **v7.5.0** | Oct 2026 | Idle Widgets, Face Unlock, Quick Launch & Landscape | Idle widget rotation with weather, net speed and calendar, face unlock animation, per-event switches, quick-launch utilities, landscape overhaul, Material You and liquid glass theming *(Released)* |
| **v8.0** | Q4 2026 | Custom Sounds & Per-App Rules | Customizable micro-sound packs for island animations and granular per-app rules for importance, vibration and card height |
| **v8.1** | Q2 2027 | System Profiles, Floating Window Polish & Ecosystem | Contextual environment profiles (Gaming, Work, Theater, Sleep), advanced Shizuku automation, freeform docking presets, plugin extension architecture and local cross-device status sharing (Bluetooth LE / LAN) with zero cloud dependencies |

*Dates are planning targets. A phase moves forward only after its release gates and automated CI suites are fully validated.*

---

## Released — v7.5.0: Idle Widgets, Face Unlock, Quick Launch & Landscape Overhaul

Released October 18, 2026.

- **Idle Widgets**: Cutout rotates through Live Clock, Date, Weather, Wind, Battery, Next Alarm, Net Speed, unread Notifications and Do Not Disturb / Modes widgets, with an hourly tick, Day Moments glyphs, smooth or LED tick styles, work / night / charging schedules, drag-to-reorder and pin-first
- **Weather, Net Speed & Calendar Widgets**: Auto, manual or last-known location, Open-Meteo or a custom provider, rain, humidity, UV, sun, air quality and hourly rows; net speed with threshold, Wi-Fi or mobile filter and a daily usage counter; next calendar event on the date widget
- **Face Unlock Animation**: Face scan and fingerprint animations over the cutout for face unlocks, finger unlocks and app lock prompts
- **Per-Event Switches & Lock Screen Presets**: 32 events, each with its own on/off, auto-expand and hide-on-lock-screen switch, plus Show all, Messages only, Calls only and Off presets
- **Quick-Launch Window & Utilities**: Apps, Recents and a 2x4 Utilities grid with resizable app widgets, a pinned apps picker, and floating windows through Android freeform or LMO Freeform
- **Landscape Overhaul**: Independent portrait and landscape profiles, Landscape Lab, rotation veil and a split island with a beside picker
- **Richer Notification Cards**: Reply drafts that survive interruptions, same-app sibling picker, expandable text, picture reader and selector, and silent notifications skipped when cycling
- **Gesture Tuning**: New swipe actions (next stored event, next same-app card, long-press to hide the pill), plus gesture sensitivity and haptic strength settings
- **Media & Call Controls**: Audio output switcher and artwork tint on the music card, call mic mute and earpiece, speaker or Bluetooth routing
- **Material You & Liquid Glass Theming**: Material 3 palette from one accent or the wallpaper, optional liquid glass theme, bundled Lora, Inter Display and Poppins fonts, custom TTF/OTF fonts, font mixing and a gooey bubble merge style
- **Settings Search & Exempt Apps**: In-settings search for every option, and per-orientation lists of apps that switch the island off (or make the pill static in portrait) while in the foreground
- **Shizuku & Diagnostics Hardening**: Broader Shizuku detection, one-tap sensitive notification grant, hotspot control, a diagnostic log that survives process kills, and a main-thread watchdog
- **Parser & Anti-Spam Improvements**: Sturdier timer, stopwatch, navigation, call and transfer detection, plus per-event cooldown for repeating system events
- **Baseline Profile Module**: Bundled `:baselineprofile` module and CI workflow for ahead-of-time compiled startup and animation paths

See [CHANGELOG.md](CHANGELOG.md) for the complete release notes.

---

## Released — v7.0.0: In-Pill Gestures, App Updates Hub, Selective Alerts & Design Refresh

Released September 20, 2026.

- **In-Pill Swipe Gestures & Media Control Engine**: Compact collapsed pill independent gesture engine with customizable actions for Swipe Left/Right (track skip or notification cycling), Swipe Up/Down, master toggle, and individual disable controls.
- **Dedicated App Updates & Downloads Hub**: Specialized settings screen featuring live Total Downloads counter (15,648+), Current Version line (v7.0.0), 1-tap GitHub update checker, in-app changelogs viewer, Top Contributors gallery, and Recent Commits feed.
- **Selective App Alerts & Sound Manager Modal**: Fast on-demand selective app management modal with search, per-app alert toggles, custom ringtone sound toggles, and island exclusion without heavy upfront package loading.
- **Unified Deep Burnt-Orange (#D84315) Design System**: Cohesive dark slate and burnt-orange theme across all settings sections with unified Material icon tinting.
- **Configurable Companion Circle Placement**: Position the multi-tasking companion circle on either the Left or Right of the main pill with anti-overlap layout geometry clamping.
- **iPhone Notch Mode**: Authentic iPhone-style top-docked notch (`y = 0`) with rounded bottom corners, downward expansion, and complete suppression of the split companion bubble.
- **Developer Mode & In-App Log Recording**: 7-tap version unlock, in-memory diagnostic ring buffer (3,000 entries), process logcat capture, live status monitor, and Scoped Storage `.txt` report export.
- **Full Settings Backup & Restore System**: Scoped Storage JSON export/import via Android SAF, atomic single-transaction DataStore restore, factory reset, and bounds clamping.
- **Bluetooth Battery & Earbuds Alternating Animation**: Dual-path battery percentage extraction via intent extra and reflection, with a smooth 3-second spring transition between live battery gauge and earbuds icon.
- **Intelligent Notification Cooldown & Anti-Spam Engine**: Sliding window burst detection with customizable quiet duration (1–30 min), trigger threshold (2–10 msgs), and per-app whitelist exclusions.
- **13 High-Resolution Screenshots**: Refreshed visual documentation capturing every new UI section and feature.

---

## Released — v6.0.0: Ultra-Fluid Spring Physics, Tap-to-Open, Auto-Hide & Landscape Mode

Released August 29, 2026.

- **Ultra-Fluid Spring Animation Engine**: Synchronized width and height morphing to high-fluidity spring curves (`stiffness = 520f`, `dampingRatio = 0.72f`), reduced cross-fade durations to 190ms, and tuned window collapse cleanup to 220ms.
- **Tap Anywhere on Expanded Cards to Open App**: Direct application launch from background/text/artwork on all expanded cards with ongoing music state preservation.
- **Auto-Hide Pill Inactivity Timer with Tap-to-Reveal**: Customizable timeout (1s to 60s) with 1-tap presets and invisible touch target to awaken the pill on tap.
- **Landscape Mode Visibility Toggle**: Option to keep Smart Island active and dynamically repositioned during device rotation.
- **Animation Flicker & Cutout Artifact Fixes**: Eliminated height jumps, window clipping, redundant updateLayout churn, and cutout double alpha blending.
- **Complete Chinese & Portuguese Localization**: 100% translation parity across all new settings and dialogs.

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

### Phase 8.0 — Custom Sounds & Per-App Rules (Q4 2026)

**Goal:** Add audio personality and finer per-app control on top of the idle widget and face unlock work.

- **Custom Sound Packs**:
  - Assign customizable micro-sound effects (subtle pop, bubble, mechanical click) to island expand and collapse animations.
- **Per-App Rules**:
  - Granular per-app rules for minimum notification importance, vibration suppression, and custom expanded card heights.

### Phase 8.1 — System Profiles, Floating Window Polish & Ecosystem (Q2 2027)

**Goal:** Provide contextual island automation, desktop-class multitasking and private multi-device glanceability.

- **Contextual System Profiles**:
  - *Gaming Profile*: Auto-hide all non-urgent notifications; show only low-battery alerts and high-priority callers in compact pill.
  - *Work / Focus Profile*: Prioritize calendar, live activities, and direct messaging; suppress social media alerts.
  - *Sleep / Night Profile*: Dim island brightness, disable ambient glow, and enforce silent animations.
- **Enhanced Freeform Window Docking**:
  - Preset window sizes (Quarter screen, Half screen, Pip) when triggering Swipe Down gesture.
  - Smooth animation morphing from expanded island card directly into Android Freeform window.
- **Plugin Extension Architecture**:
  - Expose a secure, local IPC API for third-party Android apps to feed live activity status into Smart Island.
- **Local Cross-Device Status Sync**:
  - Peer-to-peer sync over Bluetooth Low Energy (BLE) or local Wi-Fi to view tablet or secondary phone battery, call, or media status on your primary device.
  - Strictly zero cloud servers or remote accounts required.
