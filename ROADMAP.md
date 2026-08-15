# Smart Island Product Roadmap

Last updated: August 15, 2026

## Product direction

Smart Island should become the most reliable, privacy-first, and customizable glanceable activity overlay for Android. It makes important live information easier to act on without replacing the notification shade or collecting personal data.

The roadmap follows three core principles:

1. **Reliability before feature count.** Overlay, notification, media, call, and battery behavior must remain rock-solid across Android versions and major OEM ROMs.
2. **Local by default.** Notification content and preferences stay strictly on-device. Zero analytics, zero tracking, and zero internet permission.
3. **Fast, fluid, and unobtrusive.** The island must use negligible battery, avoid blocking touches, and provide instant haptic and visual feedback.

---

## Current baseline (v5.0.0)

Smart Island currently provides:

- A Compose-based floating overlay on Android 8+ (API 26 through API 36)
- Complete **Material 3 Expressive** design system with Android 12+ wallpaper dynamic color adaptation
- **11 Dynamic Island Modes**:
  - `Notification` (with full-color app launcher icons & LRU caching)
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
- **Split Island Multi-State Pill** for concurrent background states
- **5-Gesture Control Engine**: Single Tap, Quick Swipe Up, Hold + Swipe Up (300ms haptic), Swipe Down (Floating window), and Swipe Left/Right (Pager)
- **Message Sync Filtering**: Background chat polling (Snapchat, WhatsApp) excluded from download mode
- **Custom RGB Slider Color Picker**: Fine-grained color customization for all modes
- **Grouped Settings Hub Architecture** with 4 minimal Category Hub Cards
- **Landscape orientation detection** for automatic overlay hiding during full-screen apps
- **Shizuku 1-Tap Auto Setup** for automated system permission granting
- **Lock screen privacy guard** with sensitive notification content protection
- **Zero Internet Permission** and 100% local DataStore persistence
- **Strict Lint & Stable @v4 GitHub Actions CI/CD** pipeline

---

## Roadmap at a glance

| Phase | Target window | Theme | Exit outcome |
| --- | --- | --- | --- |
| **v4.0** | Jul 2026 | Material 3 UI & Grouped Settings | Modern MD3 theme engine, wallpaper dynamic colors, minimal clean UI, and Category Hub settings |
| **v5.0.0** | Aug 2026 | Battery Saver, App Icons & 5-Gesture Engine | Low Battery/Saver modes, full-color app launcher icons, sync filtering, 5 gestures text guide, and stable CI |
| **v5.1** | Oct-Nov 2026 | Face Unlock, Custom Sounds & Widgets | Face Unlock animation overlay, dynamic widget extensions (Calendar/Timer/Reminders), and custom sound packs |
| **v5.2** | Dec 2026-Jan 2027 | System Profiles & Floating Window Polish | Preset environment profiles (Gaming, Work, Theater, Sleep), advanced Shizuku automation, and freeform docking presets |
| **v6.0** | Q1-Q2 2027 | Ecosystem & Local Cross-Device Sync | Plugin extension architecture and local cross-device status sharing (Bluetooth LE / LAN) with zero cloud dependencies |

*Dates are planning targets. A phase moves forward only after its release gates and automated CI suites are fully validated.*

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

See [CHANGELOG.md](CHANGELOG.md) for the complete release notes.

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

### Phase 5.1 — Face Unlock, Custom Sounds & Dynamic Widgets (Q4 2026)

**Goal:** Expand glanceable indicators to hardware authentication and custom ambient widgets.

- **Face Unlock Integration**: Detect keyguard face recognition events via Shizuku/Accessibility and render smooth animated face-scan rings in the island.
- **Dynamic Widgets**:
  - Native Timer & Stopwatch cards with direct pause/reset controls.
  - Calendar & Reminder glance cards for upcoming meetings and alarms.
- **Custom Sound Packs**:
  - Option to assign custom micro-sound effects (subtle pop, bubble, mechanical click) to island expand and collapse animations.
- **App Whitelist / Blacklist Rules**:
  - More granular per-app rules for minimum notification importance, vibration suppression, and custom expanded card heights.

### Phase 5.2 — System Profiles & Floating Window Polish (Q1 2027)

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
