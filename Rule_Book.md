# Dynamic Island Application: Master Working Rules

This document serves as the absolute source of truth for the Android Dynamic Island application. It defines the state machine, UI physics, priority hierarchies, data flow, interaction matrices, lifecycle management, hardware adaptations, and system UI integrations to ensure a robust, crash-free, and battery-optimized user experience. 

---

## 1. System Architecture & Data Flow

The island's logic relies on a strict separation of concerns. The engine only decides what is *shown*; it never cancels background tasks.
*   **The Pipeline:** `SmartIslandNotificationListenerService` & `SystemEventReceiver` ➔ `SmartIslandNotificationRepository` (holds all unfiltered active alerts) ➔ `IslandViewModel` ➔ `AlertEngine.evaluate(list, now)` ➔ `visibleNotifications` (max 2 entries).
*   **View Binding:** Both `SmartIslandOverlayService` (window size/position) and `IslandOverlayView` (UI drawing) must read exclusively from `viewModel.visibleNotifications`. Never read the raw repository list for layout, or the window will misalign.
*   **Triggers:** `IslandViewModel` re-runs the engine whenever the alert list changes, the foreground app changes, settings change, a touch occurs, or a scheduled expiry hits.

---

## 2. Architectural UI Rules & Physics Engine

### 2.1 Spatial Anchoring & Layout
*   **Root Alignment:** The root container uses `Box(contentAlignment = Alignment.TopCenter)`. The main pill is positioned from the screen center *plus* the user's custom X offset. 
*   **Y-Axis Lock:** Top padding is hardcoded to the camera cutout height. Expansion only occurs downward and outward.
*   **Companion Bubble Positioning:** The companion bubble sits beside the main pill (left or right based on the `circlePosition` setting) and *never* shifts the main pill's absolute center.
*   **Pill Width (`contentWingDp`):** The pill grows symmetrically to keep the camera hole centered. The right-hand wing requires specific spacing based on content:
    *   *Music:* 40dp (waveform only).
    *   *Messages:* 26dp + ~6.2dp per character of the sender preview (capped at 42 characters).
    *   *Max Cap:* The pill never exceeds 280dp (`MAX_ALERT_PILL_WIDTH_DP`). Past this, text scrolls sideways with a faded right edge.

### 2.2 Physics Engine (Spring-Mass-Damper)
The island's width, height, corner radius, and offset animate using a 1D Spring-Mass-Damper system of unit mass: `force = -k * (x - target) - d * v`. 

| Preset Name | Stiffness (k) | Damping (d) | Damping Ratio | Behavior Description |
| :--- | :--- | :--- | :--- | :--- |
| **Standard (Default)**| Open: 224 / Close: 305 | n/a | Open: 0.86 / Close: 1.0 | Original tuning. Light spring to open, critically damped to close. |
| **Bouncy** | 150 | 12 | 0.49 | Highly elastic, overshoots. |
| **Snappy** | 300 | 22 | 0.64 | Fast and aggressive. |
| **Critical (Smooth)**| 180 | 27.5 | 1.02 | No overshoot. |
| **Loose** | 60 | 5 | 0.32 | Slow, floating, overshoots a lot. |

*Note: If the OS "Reduce Motion" accessibility setting is on, all presets are ignored and the island snaps instantly.*

### 2.3 Kinetic Glow Modifier
A faint light-blue (`#3186FF`) glow flares during movement and fades as the spring settles. Drawn as contiguous bands behind the island.
*   **Trigger:** Speed > 5 dp/s (taking the larger of width or height speed).
*   **Blur Formula:** Radius (dp) = `min(20, speed * 0.08)`.
*   **Performance:** The sampling coroutine runs per frame only during movement and halts after 6 frames of stillness. 

---

## 3. The Gatekeeper: Priority Hierarchy & Collision Rules

The `AlertEngine` permits a maximum of **two** visible slots (`AlertEngine.MAX_ACTIVE = 2`). Any third alert remains alive in the notification shade and takes a slot only when one frees up.

### 3.1 Alert Classification (Tiers)
`AlertClassifier.classify` maps every alert to a tier (First match wins):
1.  **Critical (Tier 0):** Alarms (`category == "alarm"`), ringing timers, and Battery <= 5% *and not charging*.
2.  **Persistent (Tier 1):** Incoming calls, navigation, voice recording, stopwatch. 
3.  **Ephemeral (Tier 3):** Messages, Battery (>5% or charging), Bluetooth, Flashlight. (Also applied to keys starting with `demo_`, quick-reply actions, or untagged notifications).
4.  **Background (Tier 2):** Music, live activities, hotspot, transfers. 
5.  **Passive:** Promos, likes, app updates, Play Store package. (Never shown on the island).

*Note: Ephemeral sits above Background so a brief text message can hijack a music pill for 7 seconds, then hand it back.*

### 3.2 Display Order & Sorting
Inside `AlertEngine.evaluate`, alerts are sorted into the two slots strictly as follows:
1.  Critical (Most recently started first).
2.  Persistent (Incoming calls *always* first, then most recently started).
3.  Ephemeral (Newest first).
4.  Background (Newest first).
*(Note: "Started" relies on the engine's first-seen key time, not the notification OS timestamp).*

### 3.3 Timing & Expiry
Timing logic is self-contained in `AlertEngine` via `nowMs`:
*   **Messages / Groups:** 7s (`communicationTimeoutMs`).
*   **Transient (Battery, BT, Flash):** 5s (`transientTimeoutMs`). Restarts *only* if the title changes (e.g., "Charging" -> "Low Battery").
*   **Critical / Persistent / Background:** Infinite. Leaves only when the source OS removes them.
*   **Touch Interactions:** Calling `touch(now)` restarts all ephemeral clocks. This fires on any interaction, card collapse, and every second while typing a reply.

### 3.4 Collisions at Arrival
*   **Auto-expand block:** If an Ephemeral alert arrives with auto-expand true, but the current main pill is Critical/Persistent, auto-expand is ignored. It waits safely as the companion bubble.
*   **Tap Behavior:** Tapping the main pill expands it. Tapping the companion bubble swaps it into the main pill slot and expands it.

---

## 4. Feature-Specific Specifications

### 4.1 Alert Manager (Messages & Notifications)
*   **Grouping:** Alerts from the same app fold into one entry via group key `packageName + "|" + groupId`.
*   **Small State (Single):** App Icon (left), Sender (bold), Message preview (gray, scrolling), Status dot (right).
*   **Small State (Group):** Stacked overlapping badges (left), text reads "N New Messages from App". Triggers a spring pulse (1.05x scale) on new additions instead of redrawing. Actions applied here affect all `memberKeys`.
*   **Medium State:** 340dp wide, pure black squircle (32dp corners). Header (app icon, sender, "Now"), up to 3 lines of body, Reply (expands inline field), Mark as Read.
*   **Large State:** This feature does not use the Large State Island.

### 4.2 Music Manager
*   **Small State:** Miniature album art (left), waveform visualizer (right; 40dp wing).
*   **Medium State:** Album Art, Track Title, Artist, scrubber, controls (Repeat, Previous, Play/Pause, Next, End).
*   **Large State:** This feature does not use the Large State Island.
*   **Action Rule ("End" Button):** Tapping "End" stops the media service, kills the background app, and immediately collapses the island to `IDLE`.

### 4.3 Voice Recorder / System Tools
*   **Small State:** Red recording dot breathing animation (infinite repeating tween, alpha 0.3 to 1.0) and active duration.
*   **Medium State:** Recording duration alongside controls (Pause/Resume, Stop/Save).
*   **Large State:** This feature does not use the Large State Island.

### 4.4 Timer (System Tools)
*   **Small State:** Live countdown.
*   **Medium State:** Live countdown alongside controls (Pause/Resume, Stop).
*   **Large State:** This feature does not use the Large State Island.
*   **Action Rule:** Reaching 00:00 promotes it from Tier 1 to Critical (Tier 0).

### 4.5 Idle Dashboard
*   **Small State:** Hardware camera cutout mask only (Idle).
*   **Medium State:** This feature does not use the Medium State Island.
*   **Large State:** 350dp x ~300dp (40dp squircle) containing app shortcuts, system toggles, and search.

---

## 5. Robustness: Memory, Battery, and Crash Prevention

*   **State Hoisting:** The UI layer is stateless and reacts purely to `GatekeeperViewModel.uiState`.
*   **Recomposition Throttling:** `derivedStateOf` is mandatory for all scroll and velocity calculations.
*   **Notification Listener Memory Leaks:** The `NotificationListenerService` uses a `Channel` or `Flow` with `.buffer(capacity = 10, onBufferOverflow = BufferOverflow.DROP_OLDEST)`.
*   **Screen Off State:** Listening for `Intent.ACTION_SCREEN_OFF` immediately halts all `infiniteRepeatable` animations (music equalizers, recording dots) to preserve CPU/battery.

---

## 6. Testing & Debugging

*   **Unit Tests:** Business logic is covered thoroughly in `AlertEngineTest` (21 tests), with immersive suppression and the face-unlock hold in `AlertEngineSuppressionTest` (5 tests) and `IslandViewModelSuppressionTest` (3 tests). Overlay guards (FLAG_SECURE / FLAG_NOT_TOUCHABLE flags, capture signals, face-unlock window, secure-app guess) are in `OverlayGuardsTest` (5), OEM panel selection in `OemAutostartUtilTest` (5) and the icon blacklist merge/restore in `StatusBarIconBlacklistTest` (6). Run via `./gradlew testDebugUnitTest`. Always update tests before altering Engine behavior.
*   **ADB Synthetic Intents:** Debug builds include `DebugAlertReceiver` to bypass the awkwardness of testing real messaging apps. 
    *   *Trigger format:* `adb shell "am broadcast -n com.niteeshbharadwaj.smartisland.personal/com.agupta07505.smartisland.debug.DebugAlertReceiver --es kind [TYPE] --es pkg [PKG] --es title [TITLE] --es text '[TEXT]'"`
*   **Visual Debugging:** 
    *   Debug builds log `IslandMotion: preset=... peakSpeed=... glow=...`.
    *   Force a fixed glow radius for UI inspection via: `adb shell settings put global debug_island_glow_dp 18` (Clear with `settings delete global debug_island_glow_dp`).

---

## 7. Permissions, Lifecycle & State Recovery Management

### 7.1 Permissions Onboarding & Runtime Revocation
The island relies on two critical permissions: `SYSTEM_ALERT_WINDOW` (Overlay) and `BIND_NOTIFICATION_LISTENER_SERVICE` (Notification Access). The engine must assume these can be revoked by the user or OS at any time.
*   **Initialization Gate:** `GatekeeperViewModel` must poll `Settings.canDrawOverlays(context)` and `NotificationManagerCompat.getEnabledListenerPackages(context)` on every app foreground. If either is missing, route immediately to the setup onboarding flow.
*   **Graceful Degradation (Partial Revocation):**
    *   *If Notification Access is revoked mid-session but Overlay remains:* The `SmartIslandOverlayService` must instantly flush all Tier 1-4 notification alerts from the `SmartIslandNotificationRepository`. The island remains alive but only displays internal system tools (Voice Recorder, Timer).
    *   *If Overlay is revoked mid-session:* `SmartIslandOverlayService` must invoke `stopSelf()` immediately to prevent `SecurityException` crashes.
*   **Recovery Path:** If the service is killed due to revocation, the app must fire a standard native Android local notification prompting the user to "Restore Smart Island Permissions."
    *   *Implementation:* `util/PermissionRecoveryNotifier` (channel `smart_island_permissions`). Fired from `SmartIslandOverlayService.onDestroy()` while the island is enabled (the accessibility overlay was turned off, or the service stopped itself after a revocation) and from `onListenerDisconnected()` when notification access is no longer granted. Tapping opens `MainActivity` on the Permissions screen (`EXTRA_OPEN_PERMISSIONS`). It is cleared once both permissions are back. On Android 13+ it needs `POST_NOTIFICATIONS` (requested from the Permissions screen) and is silently skipped without it.

### 7.2 Battery Optimization & OEM Task Killer Defenses
Persistent overlay services are aggressively targeted by Android Doze and OEM-specific memory managers. The app must establish maximum system priority.
*   **Foreground Service Mandate:** `SmartIslandOverlayService` must run as a Foreground Service (`ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE` or `TYPE_DISPLAY`). It must post a persistent, silent, minimized native notification to maintain process priority.
*   **Doze Exemption:** During onboarding, the app must request `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`. If the user declines, the app must display a permanent warning chip in the settings dashboard indicating that the island may arbitrarily disappear.
*   **OEM-Specific Autostart Routing:** For devices running MIUI (Xiaomi), ColorOS (Oppo/OnePlus), and FuntouchOS (Vivo), the app must include an `OEMIntentFactory`. During onboarding, this routes the user directly to the vendor's hidden "Auto-Start" and "Lock in Background" settings panels.
    *   *Implementation:* `util/OemIntentFactory` lists the vendor components in order of preference; `OemAutostartUtil.openBackgroundLockSettings` tries each, then App info. The Permissions screen shows the step only on those three skins. "Lock in background" itself lives in Recents (long-press the app card), which no intent can open, so the step explains it.

### 7.3 Process Death & State Recovery
When the OS forcibly kills the process for memory reclamation, the system must rebuild the island seamlessly upon automatic restart without orphaned timers or missing alerts.
*   **Notification State Rehydration:** `SmartIslandNotificationRepository` must never rely solely on in-memory app state. Upon `onCreate()` of the `SmartIslandNotificationListenerService`, it must synchronously call `getActiveNotifications()` from the OS.
    *   The `AlertEngine` must run this fresh array through the `AlertClassifier` to immediately rebuild the two visible slots before the overlay draws its first frame.
*   **Absolute Timestamp Persistence:** For internal transient tools (e.g., Timers, Stopwatches), the engine must never store relative time (e.g., "5 minutes remaining"). It must store the absolute target Unix epoch timestamp (e.g., `1716382920`) in `DataStore` or `SharedPreferences`.
    *   *Recovery Rule:* On process restart, `nowMs` compares against the stored absolute timestamp. If the time has already passed, the alert is immediately promoted to Tier 0 (Critical) and rings.
*   **Ghost Animation Prevention:** If the process is killed while the island was mid-spring animation, the rehydrated UI must skip the opening animation and snap instantly to the static target bounds of the highest-priority restored alert.

---

## 8. Security, Privacy & Lockscreen Integrity

### 8.1 Keyguard Interoperability & Notification Masking
The `SmartIslandOverlayService` must draw over the lockscreen using `WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED`, but it must strictly adhere to the user's OS-level lockscreen privacy preferences.
*   **State Detection:** `GatekeeperViewModel` must monitor device lock state via `KeyguardManager.isKeyguardLocked()` and update the `AlertEngine` dynamically.
*   **Visibility Rules (Applied only when locked):**
    *   `VISIBILITY_PUBLIC`: Display the alert normally (applies to Music, Timers, and unhidden messages).
    *   `VISIBILITY_PRIVATE`: The engine must strip the title and text payload. The UI must render only the app icon and a hardcoded fallback string (e.g., "1 New Message") for the Small and Medium states. Media controls and ongoing system tasks remain unmasked.
    *   `VISIBILITY_SECRET`: The `AlertEngine` must completely ignore and drop the notification. It cannot hold a slot in the visible array until the device is unlocked.
*   **Unlock Transition:** Upon receiving `Intent.ACTION_USER_PRESENT`, the engine must immediately trigger a recomposition to unmask any currently visible `VISIBILITY_PRIVATE` alerts, applying a crossfade animation to reveal the text.

### 8.2 Secure Window Bypassing & Biometric Clearance
As a `TYPE_APPLICATION_OVERLAY`, the island runs the risk of occluding critical system trust layers or leaking data during screen captures.
*   **Biometric Hardware Cutout Sharing:** On devices utilizing facial recognition hardware located within the camera cutout, the system Face Unlock UI shares physical space with the island. During an active lockscreen or biometric prompt, the island must immediately collapse to the `IDLE` state (hardware mask only) and disable touch interception to prevent blocking the native security camera indicator.
    *   *Implementation:* `BiometricClearance` (in `service/OverlayGuardController.kt`). When the screen turns on locked, `AlertEngine.biometricHold` empties the visible list (IDLE) and the window gets `FLAG_NOT_TOUCHABLE`, until unlock or 5 s (`BiometricClearance.WINDOW_MS`), whichever is first. The hold is bounded so lock-screen music, timers and calls (8.1) still show once the face scan is over. Third-party biometric prompts cannot be detected and are not covered.
*   **Screen Recording & Casting Protection:** When a `MediaProjection` session is active (detected via `MediaProjectionManager` callbacks or active screen-recording foreground services), the `SmartIslandOverlayService` must dynamically append `WindowManager.LayoutParams.FLAG_SECURE` to its window. This ensures that incoming personal messages (Tier 3 Ephemeral) displayed on the island are blacked out in the resulting video or cast stream.
    *   *Implementation:* `service/ScreenCaptureMonitor`. Ordinary apps cannot register `MediaProjectionManager` callbacks, so capture is any of: Android 15+ `WindowManager.addScreenRecordingCallback` (`DETECT_SCREEN_RECORDING`; the untouchable immersive probe window stays non-secure so the state does not flip once the island turns secure), a presentation display (Cast, Wi-Fi display, HDMI mirroring), or a `ScreenRecording` alert on the island (screen-recorder notifications). The flag is removed when all three clear.
*   **Secure App Passthrough:** If the foreground app sets `FLAG_SECURE` (e.g., banking apps, DRM video), the OS protects the app, but not the overlay. To respect the user's secure context, auto-expansion of incoming Tier 3 (Ephemeral) alerts is disabled when a `FLAG_SECURE` window is detected in the foreground. The alert will only appear as a minimal companion bubble.
    *   *Detection:* Another app's `FLAG_SECURE` cannot be read. `service/ForegroundSecureDetector` is a best-effort guess: the foreground package from the accessibility service's window events, matched against a small built-in list (password managers, authenticators, payments, DRM video). A miss only means the island behaves normally.

### 8.3 Interaction Locks & Authentication Gates
*   **Medium State Action Gating:** While `isKeyguardLocked()` is true, destructive or sensitive actions within the Medium State (e.g., "Reply" to a message, "Mark as Read", or opening the parent app) must be intercepted.
*   **Unlock Delegation:** Tapping a restricted action must collapse the island and invoke `KeyguardManager.requestDismissKeyguard()`. The pending action (e.g., opening the inline reply field) is cached and only executed upon successful authentication. Safe actions (Play/Pause media, Stop Timer) remain interactable without unlock.

---

## 9. Configuration Changes & Display Geometry

### 9.1 Native Cutout Anchoring (`WindowInsets`)
The overlay must never use hardcoded top-padding or arbitrary screen-center calculations, as hardware cutouts vary drastically across OEMs (center-punch, corner-punch, teardrop, or under-display).
*   **Hardware Masking:** `SmartIslandOverlayService` must attach a `View.OnApplyWindowInsetsListener` to the root view to query `WindowInsetsCompat.displayCutout`.
*   **Bounding Rect Calculation:** The `GatekeeperViewModel` must read `displayCutout.getBoundingRects()` to determine the exact pixel dimensions and coordinates of the physical camera hole. The `IDLE` state pill is generated dynamically to encompass this precise bounding box.
*   **Status Bar Avoidance:** The baseline Y-axis offset and maximum height of the small state must be calculated against the device's native status bar height to prevent the island from overlapping system icons (battery, wifi, time) when resting.

### 9.2 Screen Orientation Handling
Because the island is a physical hardware mask, it cannot arbitrarily float in software space when the device rotates.
*   **Orientation Listener:** The service must monitor `onConfigurationChanged` for transitions between `ORIENTATION_PORTRAIT` and `ORIENTATION_LANDSCAPE`.
*   **Landscape Anchoring:** In landscape, the physical cutout rotates to the lateral edge of the screen. The island must remain anchored to the physical `displayCutout` bounding rects, moving to the left or right bezel.
*   **Landscape State Constraints:** When docked on the lateral edge (landscape), the `AlertEngine` must suppress the Medium State expansion (which is designed for top-down portrait viewing). All active Ephemeral or Background alerts must render strictly in the Small State or as minimal companion bubbles to prevent obstructing full-screen media or gaming.

### 9.3 Multi-Window & Split-Screen Contexts
The island operates globally over the OS, which introduces coordinate conflicts when the user enters split-screen mode.
*   **Absolute Bounds Mandate:** As a `TYPE_APPLICATION_OVERLAY`, the island must ignore the localized UI bounds of the top-most active application window. All layout math, center-X offsets, and width limits must be calculated exclusively using `WindowManager.getCurrentWindowMetrics().bounds` to ensure the overlay maps to the physical display glass, not the software split-screen divider.
*   **Z-Order Priority:** The island must retain its Z-order priority above the split-screen divider handle to ensure touch events on the island are not intercepted by the OS multi-window resizing protocols.

---

## 10. System UI & Immersive Modes

This section defines the interaction matrix between the overlay and native Android OS UI states, ensuring the island does not obstruct media consumption or create redundant visual clutter.

### 10.1 Full-Screen & Immersive Mode Auto-Hiding
The island must not obstruct full-screen video playback, camera viewfinders, or gaming. Because standard overlays cannot directly read the window flags of other applications, the system relies on system bar visibility.
*   **State Detection:** If the app is granted Accessibility Service privileges, `GatekeeperViewModel` must monitor `WindowInsetsCompat.Type.systemBars()` for visibility changes. Alternatively, it must poll the display metrics to detect when the native status bar height reports as 0.
*   **Suppression Rule:** When the foreground application enters true immersive mode (status bar is hidden), the `AlertEngine` must force a `SUPPRESSED` state. The island scales down, animates to zero opacity, and immediately drops all touch interception flags (`FLAG_NOT_TOUCHABLE`).
    *   *Implementation:* `service/ImmersiveModeMonitor` adds an invisible, untouchable 1px accessibility-overlay probe and reads `WindowInsets.isVisible(statusBars())` (Android 11+) or `SYSTEM_UI_FLAG_FULLSCREEN` (older); entering waits 400 ms, leaving is immediate. It feeds `IslandViewModel.setImmersive` -> `AlertEngine.immersive`. `IslandViewModel.isSuppressed` (immersive and nothing allowed through) drives `Modifier.suppressedFade` (scale to 0.6 from the top centre and fade to 0 over 220 ms; Reduce Motion snaps), the window's `FLAG_NOT_TOUCHABLE`, and collapses an open card. Ephemeral clocks keep running while suppressed, so a message that arrives mid-video times out unseen.
*   **Restoration:** Upon exiting immersive mode (status bar becomes visible), the `AlertEngine` instantly restores the island to `IDLE` or resumes the highest-priority active alert.
*   **Critical Override:** Tier 0 (Critical) alerts—such as a ringing alarm or an incoming phone call—bypass the suppression rule and will force the overlay to appear even during full-screen playback.

### 10.2 Status Bar Integration & Clutter Management
The overlay shares physical Y-axis space with the native Android status bar. To prevent double-rendering (the island expanding while the native OS drops a heads-up banner), strict mitigation rules apply.
*   **Heads-Up Notification (HUN) Muting:** The `SmartIslandNotificationListenerService` cannot natively block HUNs without root. Instead, the onboarding flow must guide the user to disable native "Pop-up on screen" / "Bubbles" settings at the OS level, ensuring only the island renders the arrival animation. (The "Turn Off Pop-up Notifications" step on the Permissions screen opens the system notification settings.)
*   **Native Icon Blacklisting (ADB Mode):** To prevent the native, tiny notification icons from appearing next to the island and creating visual clutter, the app utilizes `WRITE_SECURE_SETTINGS`. If the user grants this permission via ADB, the app must write to `sysui_icon_blacklist` to hide the native notification icons entirely, leaving only system icons (battery, clock, WiFi).
    *   *Implementation:* `util/StatusBarIconBlacklist`, toggled in Developer options (off by default; shows `adb shell pm grant <applicationId> android.permission.WRITE_SECURE_SETTINGS` until granted). The real `Settings.Secure` key SystemUI reads is `icon_blacklist` ("sysui_icon_blacklist" is its tuner name). It lists status-bar *slots*, so it hides system clutter (alarm, Bluetooth, NFC, rotate, VPN, ...) and keeps battery, clock, Wi-Fi and signal; stock Android has no slot for app notification icons. Turning it off removes only the slots the app added.
*   **Unprivileged Fallback:** If `WRITE_SECURE_SETTINGS` is not granted, the `AlertEngine` must constrain the `MAX_ALERT_PILL_WIDTH_DP` dynamically based on the space between the hardware cutout and the innermost system icons, ensuring the island's expansion never overlaps the native clock or battery indicators.
