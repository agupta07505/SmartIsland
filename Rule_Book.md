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
    *   *Max Cap:* The pill never exceeds 280dp (`MAX_ALERT_PILL_WIDTH_DP`), and less when the status bar leaves less room (§10.2). Past this, text scrolls sideways with a faded right edge.

### 2.2 Physics Engine (Spring-Mass-Damper)
The island's width, height, corner radius, and offset animate using a 1D Spring-Mass-Damper system of unit mass: `force = -k * (x - target) - d * v`. 

| Preset Name | Stiffness (k) | Damping (d) | Damping Ratio | Behavior Description |
| :--- | :--- | :--- | :--- | :--- |
| **Standard (Default)**| Open: 224 / Close: 305 | n/a | Open: 0.86 / Close: 1.0 | Original tuning. Light spring to open, critically damped to close. |
| **Bouncy** | 150 | 12 | 0.49 | Highly elastic, overshoots. |
| **Snappy** | 300 | 22 | 0.64 | Fast and aggressive. |
| **Critical (Smooth)**| 180 | 27.5 | 1.02 | No overshoot. |
| **Loose** | 60 | 5 | 0.32 | Slow, floating, overshoots a lot. |

*Note: Android has no separate "Reduce Motion" switch. `rememberReduceMotion()` (`ui/Motion.kt`) treats `Settings.Global.ANIMATOR_DURATION_SCALE == 0` as reduce motion, which is what Accessibility > "Remove animations" sets (as does Developer options > "Animator duration scale: Animation off"). When it is on, all presets are ignored: width, height, corner radius and offset snap instantly, content cross-fades shorten to 120ms, the kinetic glow is off, and looping decorations (`animateFloatCalm`) rest at their start value. The setting is read when the island composes, so a change applies the next time the overlay is created.*

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
1.  **Critical (Tier 0):** A ringing alarm (`IslandMode.Alarm`, §4.10) and any other `category == "alarm"` alert unless it says it is not ringing ("Upcoming", "Snoozed", "Missed alarm": `RingingAlarm.saysNotRinging`), ringing timers, and Battery <= 5% *and not charging*.
2.  **Persistent (Tier 1):** Incoming calls, navigation, screen recording, voice recording (`IslandMode.VoiceRecording`, §4.3), timers, stopwatch, and the camera / microphone privacy indicator (`IslandMode.PrivacyIndicator`, §4.15/4.16).
3.  **Ephemeral (Tier 3):** Messages, Battery (>5% or charging), Bluetooth, Flashlight, and alarm notes that are not ringing. (Also applied to keys starting with `demo_`, quick-reply actions, or untagged notifications).
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
*   **Auto-expand block:** If an Ephemeral alert arrives with auto-expand true, but the current main pill is Critical/Persistent, auto-expand is ignored. It waits safely as the companion bubble. The same happens in landscape (§9.2) and over a `FLAG_SECURE` foreground app (§8.2); all three rules live in `ExpansionPolicy.allowAutoExpand`.
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

### 4.3 Voice Record (Refined)
*   **Classification & Priority:** Tier 1 (Persistent).
*   **Non Expanded Small State:** Red microphone/recording dot (breathing animation, alpha 0.3 to 1.0).
*   **Expanded Small State:** Red breathing dot (left), live active duration counter MM:SS (right).
*   **Medium State:** 340dp wide. Active recording duration alongside controls (Pause/Resume, Stop/Save).
*   **Large State:** This feature does not use the Large State Island.
*   **Action Rule:** To preserve battery, the breathing animation must pause when `Intent.ACTION_SCREEN_OFF` is received and resume on screen on.
*   **Implementation:** `toIslandMode` returns `VoiceRecording` for a known recorder package (`OemDeviceRules.isVoiceRecorderPackage`: Samsung Voice Recorder `com.sec.android.app.voicenote`, Google Recorder `com.google.android.apps.recorder`, the MIUI / ColorOS / Vivo / Huawei / Motorola / Transsion / Nubia sound recorders, ...) before the timer/stopwatch heuristics, and for a voice-recording phrase ("voice recording", "audio recorder", ...) before the screen-recorder rule, so the screen-recorder sets that also list some voice apps no longer swallow them (and a voice recording never turns on the capture `FLAG_SECURE`). Text that mentions the screen ("screen", "capturing", "video") is never a voice recording; a finished one ("saved", not ongoing) falls back to a normal alert. The notification stays in the shade. The duration comes from `VoiceRecordingClock`: the chronometer (`when` + `showChronometer`), else the shown "00:42" counted back from now; while paused (a Resume and no Pause action, or "paused" text: `NotificationActionMatcher.isRecorderPaused`) the count stands still. `ui/expanded/VoiceRecordingExpanded.kt` draws the dot, MM:SS and the Pause/Resume and Stop/Save buttons, each firing the recorder's own action (matched by label, hidden when absent; `GatedAction.RecorderControl` works locked). The breathing transition is not created at all while `rememberIslandAnimationsActive()` is false (the overlay lifecycle below STARTED, i.e. the screen is off, §5), while paused, or with Reduce Motion.

### 4.4 Timer (System Tools)
*   **Small State:** Live countdown.
*   **Medium State:** Live countdown alongside controls (Pause/Resume, Stop).
*   **Large State:** This feature does not use the Large State Island.
*   **Action Rule:** Reaching 00:00 promotes it from Tier 1 to Critical (Tier 0). `AlertClassifier` treats a Timer as finished when its category is `alarm`, or when its shown title/text says so (`TimerStopwatchParser.isFinishedTimerText`: "Time's up", "Timer finished/expired/ended/done", or a standalone `0:00` / `00:00` / `0:00:00`).

### 4.5 Idle Dashboard
*   **Small State:** Hardware camera cutout mask only (Idle).
*   **Medium State:** This feature does not use the Medium State Island.
*   **Large State:** 350dp x ~300dp (40dp squircle) containing app shortcuts, system toggles, and search.

### 4.6 Bluetooth Connectivity
*   **Small State:** Device-type icon (e.g., headphones, speaker, or generic Bluetooth logo) (left), overall Battery % (right).
*   **Medium State:** 340dp wide. Expanded device graphic/icon, Device Name, detailed battery gauges (e.g., individual L/R earbuds and case percentages if broadcasted via `BluetoothDevice`), and a direct shortcut to native OS Bluetooth settings.
*   **Large State:** This feature does not use the Large State Island.
*   **Action Rule:** Operates strictly as a Tier 3 Ephemeral alert. Auto-dismisses after 5s (`transientTimeoutMs`). Tapping the Small state expands it to Medium, which resets the 5s clock. Tapping the native settings shortcut collapses the island instantly and launches the OS Bluetooth dashboard.

### 4.7 Incoming Calls (Cellular & VoIP / WhatsApp)
*   **Classification & Priority:** Classified as Tier 1 (Persistent). Takes absolute precedence within Tier 1 and always claims Slot 1 (Main Pill).
*   **Small State:** Caller Avatar or App Icon (left), pulsing call indicator icon (right). For WhatsApp/VoIP calls, an overlaid app badge must render at the corner of the avatar.
*   **Medium State:** 340dp wide, pure black squircle (32dp corners).
    *   *Header:* App icon/label, Caller Name, subtext ("Incoming call...").
    *   *Body/Controls:* Contact avatar, dual action buttons: Decline (red) and Accept (green).
*   **Large State:** This feature does not use the Large State Island.
*   **Action Rules:**
    *   *Accept:* Dispatches the accept pending intent. Transitions to Active Call state (Small: duration counter; Medium: duration, Mute, Speaker, End Call).
    *   *Decline:* Triggers decline intent, terminates alert, drops island to IDLE or resumes preempted background alert.
    *   *Keyguard:* Accept/Decline remain actionable directly from the lockscreen without requiring device unlock.
*   **Implementation:** `ui/expanded/IncomingCallExpanded.kt`. `CallAvatar` (small pill, companion bubble and Medium card) shows the contact photo, the app icon or a phone glyph; a call from anything but the phone's own dialer (`OemDeviceRules.isDialerCall`) gets the app icon as a badge on the photo's corner. The Medium card has the header (app icon + label), the caller and "Incoming call…", and Decline / Accept. Accept keeps the card open; when the call's notification turns ongoing (no Answer action, so `isCallRinging` is false) it becomes the Active Call card: "Ongoing call" + live duration and Mute, Speaker, End Call, each shown only when the notification offers that action (`NotificationActionMatcher`: semantic action first, e.g. `SEMANTIC_ACTION_MUTE`, then whole-word labels). The small pill's right side is a pulsing call icon while ringing and the duration once answered; the duration counts from the call notification's chronometer when it shows one, else from the moment it stopped ringing. Every call control runs through `KeyguardGate` as `GatedAction.CallControl` (never needs unlock), and a ringing call shows and stays touchable through the face-unlock hold (§8.2).

### 4.8 Hotspot (Tethering)
*   **Classification & Priority:** Tier 2 (Background).
*   **Non Expanded Small State:** Static tethering/hotspot icon.
*   **Expanded Small State:** Hotspot icon (left), active connection count or "On" (right).
*   **Medium State:** 340dp wide. Hotspot icon, network name (SSID), number of connected devices, live data usage (if broadcasted by OS), and a shortcut to disable the hotspot.
*   **Large State:** This feature does not use the Large State Island.
*   **Action Rule:** Actively monitors `WifiManager.WIFI_AP_STATE_CHANGED_ACTION`. Tapping the Medium state toggle disables the hotspot and instantly drops the island to `IDLE`.

### 4.9 Flashlight
*   **Classification & Priority:** Tier 3 (Ephemeral).
*   **Non Expanded Small State:** Flashlight icon (yellow when on, gray when off).
*   **Expanded Small State:** Flashlight icon (left), status text "Flashlight On" or "Flashlight Off" (right).
*   **Medium State:** Flashlight icon, "Flashlight" title, and a large interactive toggle switch to turn it off/on.
*   **Large State:** This feature does not use the Large State Island.
*   **Action Rule:** Operates on the 5s `transientTimeoutMs` clock. Expands to announce the state change then dismisses. While ON, the user can manually tap the IDLE pill to invoke the Medium State and turn it off via `CameraManager.setTorchMode()`.

### 4.10 Alarm (Ringing)
*   **Classification & Priority:** Tier 0 (Critical). Absolute highest priority. Preempts all other active alerts.
*   **Non Expanded Small State:** Pulsing alarm bell icon.
*   **Expanded Small State:** Pulsing alarm bell icon (left), current system time (right).
*   **Medium State:** 340dp wide. Alarm icon, "Alarm Ringing" header, currently active time, and dual prominent buttons: "Snooze" (gray) and "Stop" (red).
*   **Large State:** This feature does not use the Large State Island.
*   **Action Rule:** Appears only when the alarm is actively firing (via `Notification.CATEGORY_ALARM` full-screen intents). Dismissing or Snoozing executes the respective `PendingIntent` and removes the alert from the engine.
*   **Implementation:** `RingingAlarm.isRinging` (checked right after timers in `toIslandMode`): `CATEGORY_ALARM` with a full-screen intent or a Snooze action, or a clock app offering Snooze and Stop together; "Upcoming", "Snoozed", "Missed alarm" text never rings (those notes are Ephemeral, §3.1). The alarm's notification stays in the shade (its full-screen UI is the clock app's). `ui/expanded/AlarmExpanded.kt`: the pulsing bell (`AlarmCollapsedGlyph`, also the companion bubble), the current time in the user's 12/24h format, and Snooze (gray) / Stop (red), each firing the clock app's matching action (Snooze; Stop / Dismiss / Turn off) and removing the alert; a missing action hides its button. Both are `GatedAction.AlarmControl` (work locked), and a ringing alarm shows through the face-unlock hold (§8.2). Before this the alarm used the message card, whose app actions needed an unlock.

### 4.11 Stopwatch
*   **Classification & Priority:** Tier 1 (Persistent).
*   **Non Expanded Small State:** Static stopwatch icon.
*   **Expanded Small State:** Stopwatch icon (left), live MM:SS:ms timer text (right).
*   **Medium State:** 340dp wide. Large live timer display, "Lap" button, "Pause/Resume" toggle, and "Stop" button.
*   **Large State:** This feature does not use the Large State Island.
*   **Action Rule:** `GatekeeperViewModel` must use `Choreographer` or a high-refresh `Flow` to render the milliseconds (ms) smoothly without causing layout thrashing in the Expanded Small State.

### 4.12 Battery & Charging
*   **Classification & Priority:** Tier 3 (Ephemeral) for standard events; Tier 0 (Critical) for <= 5% uncharged.
*   **Non Expanded Small State:** Battery icon filled to current capacity (Green for charging, White for standard, Red for low).
*   **Expanded Small State:** Battery icon (left), Percentage + Status (e.g., "85% Charging" or "15% Low Battery") (right).
*   **Medium State:** 340dp wide. Large battery graphic, exact percentage, status subtext ("Fast Charging", "Discharging"), and estimated time until full/empty.
*   **Large State:** This feature does not use the Large State Island.
*   **Action Rule:** Triggered dynamically via `ACTION_POWER_CONNECTED`, `ACTION_POWER_DISCONNECTED`, and `ACTION_BATTERY_CHANGED`. State updates invoke the Expanded Small State for 5s before dismissing. If battery hits <=5% and is not charging, it locks into the UI as a Tier 0 Critical alert until a charger is connected.

### 4.13 Screen Record
*   **Classification & Priority:** Tier 1 (Persistent).
*   **Non Expanded Small State:** Recording indicator (red circle with inner dot).
*   **Expanded Small State:** Recording indicator (left), live MM:SS duration (right).
*   **Medium State:** 340dp wide. Recording icon, active duration, and a "Stop Recording" button.
*   **Large State:** This feature does not use the Large State Island.
*   **Action Rule:** Must strictly observe Section 8.2 (`FLAG_SECURE`). The island must not inadvertently record itself displaying private notifications. Clicking "Stop" fires the intent to the system screen recorder and collapses the island.

### 4.14 Navigation & Ride-Sharing (Maps, Uber, Ola, Rapido)
*   **Classification & Priority:** Tier 1 (Persistent).
*   **Non Expanded Small State:** Directional arrow (Maps) or App Icon (Uber/Ola/Rapido) (left), simple distance/ETA badge (right).
*   **Expanded Small State:** App Icon/Arrow (left), Next Turn Distance (e.g., "200m") or Ride ETA (e.g., "Drop in 5m") (right).
*   **Medium State:** 340dp wide.
    *   *Maps:* Large directional arrow, Turn-by-turn instruction, ETA, and distance remaining.
    *   *Ride-Sharing:* Driver vehicle graphic/avatar, License Plate number, Driver Name, and live ETA to destination/pickup.
*   **Large State:** This feature does not use the Large State Island.
*   **Action Rule:** Relies on parsing `Notification.BigTextStyle` or scraping `RemoteViews` from the active navigation notification to extract live text streams. Prioritizes ride-sharing apps over standard map directions if both are active simultaneously.

### 4.15 Camera Privacy Indicator
*   **Classification & Priority:** Tier 1 (Persistent) - Hardware Security Override.
*   **Non Expanded Small State:** Static solid green dot (matches native Android privacy indicator color).
*   **Expanded Small State:** Green camera icon (left), "Camera in use" (right).
*   **Medium State:** 340dp wide. Green camera icon, text indicating the active package (e.g., "Instagram is using your camera"), and a shortcut to the OS privacy dashboard.
*   **Large State:** This feature does not use the Large State Island.
*   **Action Rule:** Hooks into `AppOpsManager.OnOpActiveChangedListener` (`OPSTR_CAMERA`). If the native camera is opened, the island collapses its Non Expanded Small State to a simple green dot positioned at the extreme right edge of the cutout to mimic native OS behavior.
*   **Implementation:** `service/PrivacyIndicatorMonitor` (started with the overlay session) posts one merged alert, key `system_privacy_indicator` (`PrivacyIndicatorAlert`: the category says which sensors are on, the title names the camera's user and the text the microphone's). Sources:
    *   `AppOpsManager.startWatchingActive(OPSTR_CAMERA, OPSTR_RECORD_AUDIO)` on Android 11+, which names the app but reports other apps only to a holder of `WATCH_APPOPS` (declared in the manifest). That permission is signature|privileged: a Play or sideloaded install never gets it, and Shizuku does not help (its shell uid can `pm grant` only runtime and development permissions, and watching through the hidden `IAppOpsService` binder would need a hidden-API callback stub). So this path runs only on a privileged or system install.
    *   Everywhere else (the normal case) the fallback: `CameraManager.AvailabilityCallback` (some app opened a camera) and `AudioManager.AudioRecordingCallback` (some app records; hotword and silenced clients excluded). They need no permission but do not say who: the camera is attributed to the foreground app from the accessibility events (only a visible app may open the camera), the microphone to "An app". Camera events are ignored while the keyguard is locked so face unlock does not light it.
    *   `ui/expanded/PrivacyIndicatorExpanded.kt`: the green / orange / split dot is the companion bubble (`PrivacyIndicatorDot`); the main pill shows the icon(s) and "Camera in use" / "Microphone in use" / "Camera & mic in use" (the pill grows for the label, `PillLayout.contentWingDp`); the Medium card names the app(s) and opens the privacy dashboard (`android.intent.action.REVIEW_PERMISSION_USAGE`, else `Settings.ACTION_PRIVACY_SETTINGS`, else Settings; gated `OpenApp` since it leaves the lockscreen). Tapping the card opens the same dashboard. Not done: moving the dot to the cutout's right edge (the bubble keeps its usual place).

### 4.16 Microphone Privacy Indicator
*   **Classification & Priority:** Tier 1 (Persistent) - Hardware Security Override.
*   **Non Expanded Small State:** Static solid orange dot (matches native Android privacy indicator color).
*   **Expanded Small State:** Orange microphone icon (left), "Microphone in use" (right).
*   **Medium State:** 340dp wide. Orange mic icon, text indicating the active package (e.g., "WhatsApp is using your microphone"), and a shortcut to the OS privacy dashboard.
*   **Large State:** This feature does not use the Large State Island.
*   **Action Rule:** Hooks into `AppOpsManager.OnOpActiveChangedListener` (`OPSTR_RECORD_AUDIO`). Behaves identically to the Camera indicator. If both Camera and Mic are active simultaneously, the Non Expanded Small State displays a split Green/Orange pill.
*   **Implementation:** Same monitor and alert as §4.15: camera and microphone never take both slots; with both on the category is `privacy_camera_microphone` and the bubble is the split green/orange pill.

---

## 5. Robustness: Memory, Battery, and Crash Prevention

*   **State Hoisting:** The UI layer is stateless and reacts purely to `IslandViewModel`'s state flows (`visibleNotifications`, `mode`, `expanded`, `selectedIndex`).
*   **Recomposition Throttling:** `derivedStateOf` is mandatory for all scroll and velocity calculations.
*   **Notification Listener Memory Bounds:** There is no event `Channel` between the listener and the island, so nothing can queue up unbounded. `SmartIslandNotificationListenerService` handles each callback in a short coroutine on its own `serviceScope` (removals are debounced 350ms per key and a newer event for the same key cancels the pending one) and writes straight into `SmartIslandNotificationRepository`:
    *   The alert list is a `MutableStateFlow` (conflated: collectors only ever see the latest list), capped at the newest 50 entries (`MAX_STORED_NOTIFICATIONS`).
    *   One-shot signals (`autoExpandEvent`, `resetTimerEvent`, `commands`) are `MutableSharedFlow(extraBufferCapacity = 16)` written with `tryEmit`, so a burst beyond 16 unconsumed events drops the newest instead of suspending or growing.
*   **Screen Off State:** On `Intent.ACTION_SCREEN_OFF`, `SmartIslandOverlayService` calls `overlayOwners.pause()`, which moves the overlay's lifecycle down to `CREATED`. That fires `ON_STOP`, and Compose's window recomposer pauses its frame clock, so every `infiniteRepeatable` animation (music equalizers, recording dots) and recomposition halts until `ACTION_SCREEN_ON` resumes it. `IslandViewModel` and the alert engine keep running, so the island is current the moment the screen wakes.

---

## 6. Testing & Debugging

*   **Unit Tests:** Business logic is covered thoroughly in `AlertEngineTest` (30 tests), with the expansion, recovery and lockscreen rules in `ExpansionPolicyTest`, `TimerRecoveryTest` and `KeyguardGateTest`, immersive suppression and the face-unlock hold in `AlertEngineSuppressionTest` (6 tests) and `IslandViewModelSuppressionTest` (3 tests). The arrival collision rule (§3.4) is covered in `IslandViewModelTest` (`testEphemeralAutoExpandIsIgnoredWhileACallHoldsTheMainPill`). Overlay guards (FLAG_SECURE / FLAG_NOT_TOUCHABLE flags, capture signals, face-unlock window, secure-app guess) are in `OverlayGuardsTest` (6), the alarm / call / recorder / privacy-indicator / landscape-docking rules in `IslandAlertParsersTest`, OEM panel selection in `OemAutostartUtilTest` (5) and the icon blacklist merge/restore in `StatusBarIconBlacklistTest` (6). Run via `./gradlew testDebugUnitTest`. Always update tests before altering Engine behavior.
*   **ADB Synthetic Intents:** Debug builds include `DebugAlertReceiver` (declared in `app/src/debug/AndroidManifest.xml`, guarded by `android.permission.DUMP` so only the adb shell or the system can send to it) to bypass the awkwardness of testing real messaging apps. The component is `<applicationId>/<class>`: the applicationId is `com.niteeshbharadwaj.smartisland.personal` (no debug suffix) while the class keeps the source namespace `com.agupta07505.smartisland`.
    *   *Trigger format:* `adb shell "am broadcast -n com.niteeshbharadwaj.smartisland.personal/com.agupta07505.smartisland.debug.DebugAlertReceiver --es kind [TYPE] --es pkg [PKG] --es title [TITLE] --es text '[TEXT]'"`
    *   *`kind`:* `message` (default) | `promo` | `call` | `ongoing_call` | `alarm` (a ringing `Alarm` with Snooze / Stop) | `nav` | `music` | `timer` | `stopwatch` | `record` | `voice` (voice recorder) | `camera` | `mic` | `camera_mic` (the privacy indicator) | `live` | `battery` | `bluetooth` | `flash` | `clear` (removes every alert). Optional extras: `--es app [NAME]`, `--es key [KEY]`, `--es group [GROUP_ID]`, `--ez reply true|false`, `--ez expand true` (auto-expand on arrival).
*   **Visual Debugging:** 
    *   Debug builds log `IslandMotion: preset=... peakSpeed=... glow=...`.
    *   Force a fixed glow radius for UI inspection via: `adb shell settings put global debug_island_glow_dp 18` (Clear with `settings delete global debug_island_glow_dp`).

---

## 7. Permissions, Lifecycle & State Recovery Management

### 7.1 Permissions Onboarding & Runtime Revocation
The island relies on two critical permissions: `SYSTEM_ALERT_WINDOW` (Overlay) and `BIND_NOTIFICATION_LISTENER_SERVICE` (Notification Access). The engine must assume these can be revoked by the user or OS at any time.
*   **Initialization Gate:** `IslandViewModel` must poll `Settings.canDrawOverlays(context)` and `NotificationManagerCompat.getEnabledListenerPackages(context)` on every app foreground. If either is missing, route immediately to the setup onboarding flow.
*   **Graceful Degradation (Partial Revocation):**
    *   *If Notification Access is revoked mid-session but Overlay remains:* The `SmartIslandOverlayService` must instantly flush all Tier 1-4 notification alerts from the `SmartIslandNotificationRepository`. The island remains alive but only displays internal system tools (Voice Recorder, Timer).
    *   *If Overlay is revoked mid-session:* `SmartIslandOverlayService` must invoke `stopSelf()` immediately to prevent `SecurityException` crashes.
*   **Implementation:** `MainActivity.onResume` runs `PermissionGate.check` and, if either grant is missing, routes to the Permissions setup screen. Because the island is a `TYPE_ACCESSIBILITY_OVERLAY`, "overlay granted" means the overlay accessibility service is enabled (`canDrawOverlays` alone cannot start it). The overlay service re-checks on screen-on, configuration change and unbind, and treats a `SecurityException`/`BadTokenException` from `WindowManager` as revocation only if the grant is really gone; `onPermissionLost(reason)` removes the window, stops the foreground notification and calls `stopSelf()`. The listener's `onListenerDisconnected` flushes every listener-sourced alert (`AlertSource.listenerSourcedKeys`), keeping `system_*` tools.
*   **Recovery Path:** If the service is killed due to revocation, the app must fire a standard native Android local notification prompting the user to "Restore Smart Island Permissions."
    *   *Implementation:* `util/PermissionRecoveryNotifier` (channel `smart_island_permissions`). Fired from `SmartIslandOverlayService.onDestroy()` while the island is enabled (the accessibility overlay was turned off, or the service stopped itself after a revocation) and from `onListenerDisconnected()` when notification access is no longer granted. Tapping opens `MainActivity` on the Permissions screen (`EXTRA_OPEN_PERMISSIONS`). It is cleared once both permissions are back. On Android 13+ it needs `POST_NOTIFICATIONS` (requested from the Permissions screen) and is silently skipped without it.

### 7.2 Battery Optimization & OEM Task Killer Defenses
Persistent overlay services are aggressively targeted by Android Doze and OEM-specific memory managers. The app must establish maximum system priority.
*   **Foreground Service Mandate:** `SmartIslandOverlayService` must run as a Foreground Service (`ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE` or `TYPE_DISPLAY`). It must post a persistent, silent, minimized native notification to maintain process priority.
*   **Doze Exemption:** During onboarding, the app must request `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`. If the user declines, the app must display a permanent warning chip in the settings dashboard indicating that the island may arbitrarily disappear. (Implemented as the battery warning card at the top of the Settings tab in `SmartIslandHomeScreen`, shown while `PowerManager.isIgnoringBatteryOptimizations(packageName)` is false and re-checked on every resume; tapping it re-requests the exemption.)
*   **OEM-Specific Autostart Routing:** For devices running MIUI (Xiaomi), ColorOS (Oppo/OnePlus), and FuntouchOS (Vivo), the app must include an `OemIntentFactory`. During onboarding, this routes the user directly to the vendor's hidden "Auto-Start" and "Lock in Background" settings panels.
    *   *Implementation:* `util/OemIntentFactory` lists the vendor components in order of preference; `OemAutostartUtil.openBackgroundLockSettings` tries each, then App info (`OemAutostartUtil.openAutostartSettings` covers the auto-start panel). The Permissions screen shows the step only on those three skins. "Lock in background" itself lives in Recents (long-press the app card), which no intent can open, so the step explains it.

### 7.3 Process Death & State Recovery
When the OS forcibly kills the process for memory reclamation, the system must rebuild the island seamlessly upon automatic restart without orphaned timers or missing alerts.
*   **Notification State Rehydration:** `SmartIslandNotificationRepository` must never rely solely on in-memory app state. Upon `onCreate()` of the `SmartIslandNotificationListenerService`, it must synchronously call `getActiveNotifications()` from the OS.
    *   The `AlertEngine` must run this fresh array through the `AlertClassifier` to immediately rebuild the two visible slots before the overlay draws its first frame.
    *   *Implementation:* `getActiveNotifications()` throws before the listener is connected, so the sync runs synchronously in `onListenerConnected` (`rehydrateFromActiveNotifications`, waiting at most 400ms for settings). Only ongoing modes (music, calls, timers, stopwatches, navigation, live activities, transfers, hotspot, recording) are imported; stale one-off messages are not. It then marks `IslandRestoreState.markSynced()`.
*   **Absolute Timestamp Persistence:** For internal transient tools (e.g., Timers, Stopwatches), the engine must never store relative time (e.g., "5 minutes remaining"). It must store the absolute target Unix epoch timestamp (e.g., `1716382920`) in `DataStore` or `SharedPreferences`.
    *   *Recovery Rule:* On process restart, `nowMs` compares against the stored absolute timestamp. If the time has already passed, the alert is immediately promoted to Tier 0 (Critical) and rings.
    *   *Implementation:* `TimerAnchorStore` (SharedPreferences, read synchronously) keeps `T:<end epoch ms>` / `S:<start epoch ms>` per notification key, rewritten only when the anchor moves by a second or more and dropped when the notification goes. `TimerRecovery.recover` is the pure rule: while restoring, a stored anchor of the same kind beats the freshly parsed one; a running timer whose end is `<= nowMs` is promoted, which the listener does by posting it with the `alarm` category (Timer + alarm = Critical). Paused clocks are never promoted.
*   **Ghost Animation Prevention:** If the process is killed while the island was mid-spring animation, the rehydrated UI must skip the opening animation and snap instantly to the static target bounds of the highest-priority restored alert.
    *   *Implementation:* `RestoreSnap` (in `ui/IslandMotion.kt`): the island's geometry uses `snap()` until the listener's initial sync has finished plus 600ms (`SETTLE_MS`), counted from the overlay's first frame, and never longer than 5s (`MAX_WAIT_MS`) after it.

---

## 8. Security, Privacy & Lockscreen Integrity

### 8.1 Keyguard Interoperability & Notification Masking
The `SmartIslandOverlayService` must draw over the lockscreen using `WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED`, but it must strictly adhere to the user's OS-level lockscreen privacy preferences.
*   **State Detection:** `IslandViewModel` must monitor device lock state via `KeyguardManager.isKeyguardLocked()` and update the `AlertEngine` dynamically.
*   **Visibility Rules (Applied only when locked):**
    *   `VISIBILITY_PUBLIC`: Display the alert normally (applies to Music, Timers, and unhidden messages).
    *   `VISIBILITY_PRIVATE`: The engine must strip the title and text payload. The UI must render only the app icon and a hardcoded fallback string (e.g., "1 New Message") for the Small and Medium states. Media controls and ongoing system tasks remain unmasked.
    *   `VISIBILITY_SECRET`: The `AlertEngine` must completely ignore and drop the notification. It cannot hold a slot in the visible array until the device is unlocked.
*   **Unlock Transition:** Upon receiving `Intent.ACTION_USER_PRESENT`, the engine must immediately trigger a recomposition to unmask any currently visible `VISIBILITY_PRIVATE` alerts, applying a crossfade animation to reveal the text.
*   **Implementation:** The overlay service sets `IslandViewModel.setLocked` from `isKeyguardLocked()` (screen on/off, `USER_PRESENT`, window changes) and the view model passes it to `AlertEngine.evaluate(list, now, isLocked)`. `LockscreenPrivacy` holds the rules: only app messages (`IslandMode.Notification`, not `call`/`alarm` categories) are maskable; a masked copy has `isMasked = true`, title "1 New Message" / "N New Messages", empty text, no sender avatar. A folded group takes its most private member's visibility. The listener records each alert's effective visibility: `Notification.visibility`, tightened by the ranking's `lockscreenVisibilityOverride`, and secret when the OS shows no notifications on the lockscreen. The pill (`AlertPillText`) and the Medium card crossfade (220ms) when `isMasked` flips. The older "App icon only" privacy setting still applies on top.

### 8.2 Secure Window Bypassing & Biometric Clearance
As a `TYPE_APPLICATION_OVERLAY`, the island runs the risk of occluding critical system trust layers or leaking data during screen captures.
*   **Biometric Hardware Cutout Sharing:** On devices utilizing facial recognition hardware located within the camera cutout, the system Face Unlock UI shares physical space with the island. During an active lockscreen or biometric prompt, the island must immediately collapse to the `IDLE` state (hardware mask only) and disable touch interception to prevent blocking the native security camera indicator.
    *   *Implementation:* `BiometricClearance` (in `service/OverlayGuardController.kt`). When the screen turns on locked, `AlertEngine.biometricHold` empties the visible list (IDLE) and the window gets `FLAG_NOT_TOUCHABLE`, until unlock or 5 s (`BiometricClearance.WINDOW_MS`), whichever is first. The one exception is what woke the screen: a ringing call or a ringing alarm (`AlertEngine.breaksBiometricHold`) still shows and keeps the window touchable (`OverlayWindowGuards.untouchable`), so Accept / Decline / Snooze / Stop work at once (§4.7, §4.10). The hold is bounded so lock-screen music, timers and calls (8.1) still show once the face scan is over. Third-party biometric prompts cannot be detected and are not covered.
*   **Screen Recording & Casting Protection:** When a `MediaProjection` session is active (detected via `MediaProjectionManager` callbacks or active screen-recording foreground services), the `SmartIslandOverlayService` must dynamically append `WindowManager.LayoutParams.FLAG_SECURE` to its window. This ensures that incoming personal messages (Tier 3 Ephemeral) displayed on the island are blacked out in the resulting video or cast stream.
    *   *Implementation:* `service/ScreenCaptureMonitor`. Ordinary apps cannot register `MediaProjectionManager` callbacks, so capture is any of: Android 15+ `WindowManager.addScreenRecordingCallback` (`DETECT_SCREEN_RECORDING`; the untouchable immersive probe window stays non-secure so the state does not flip once the island turns secure), a presentation display (Cast, Wi-Fi display, HDMI mirroring), or a `ScreenRecording` alert on the island (screen-recorder notifications). The flag is removed when all three clear.
*   **Secure App Passthrough:** If the foreground app sets `FLAG_SECURE` (e.g., banking apps, DRM video), the OS protects the app, but not the overlay. To respect the user's secure context, auto-expansion of incoming Tier 3 (Ephemeral) alerts is disabled when a `FLAG_SECURE` window is detected in the foreground. The alert will only appear as a minimal companion bubble.
    *   *Detection:* Another app's `FLAG_SECURE` cannot be read. `service/ForegroundSecureDetector` is a best-effort guess: the foreground package from the accessibility service's window events, matched against a small built-in list (password managers, authenticators, payments, DRM video). A miss only means the island behaves normally.
    *   *Implementation:* `IslandViewModel.setForegroundSecure(Boolean)` feeds `ExpansionPolicy.allowAutoExpand(..., foregroundIsSecure)`, which also holds the §3.4 collision rule.

### 8.3 Interaction Locks & Authentication Gates
*   **Medium State Action Gating:** While `isKeyguardLocked()` is true, destructive or sensitive actions within the Medium State (e.g., "Reply" to a message, "Mark as Read", or opening the parent app) must be intercepted.
*   **Unlock Delegation:** Tapping a restricted action must collapse the island and invoke `KeyguardManager.requestDismissKeyguard()`. The pending action (e.g., opening the inline reply field) is cached and only executed upon successful authentication. Safe actions (Play/Pause media, Stop Timer) remain interactable without unlock.
*   **Implementation:** `KeyguardGate.run(context, GatedAction, onCollapse) { ... }` wraps Reply, Mark as Read and the app's own action in `NotificationExpanded`, and every open-app path in the overlay service (open notification, floating window, shortcut launch). While locked it caches one action (`PendingActionGate`: newest wins, handed out once), collapses the island and starts the invisible `UnlockTrampolineActivity`, which calls `requestDismissKeyguard`; only `onDismissSucceeded` runs the action. A deferred Reply reopens that alert's card with the reply field open (`KeyguardGate.requestReply`). Safe controls are explicit `GatedAction`s that never wait: `MediaControl`, `TimerControl`, `CallControl` (Accept, Decline, Mute, Speaker, End Call), `AlarmControl` (Snooze, Stop) and `RecorderControl` (Pause/Resume, Stop/Save). Opening the privacy dashboard is `OpenApp` (gated).

---

## 9. Configuration Changes & Display Geometry

### 9.1 Native Cutout Anchoring (`WindowInsets`)
The overlay must never use hardcoded top-padding or arbitrary screen-center calculations, as hardware cutouts vary drastically across OEMs (center-punch, corner-punch, teardrop, or under-display).
*   **Hardware Masking:** `SmartIslandOverlayService` must attach a `View.OnApplyWindowInsetsListener` to the root view to query `WindowInsetsCompat.displayCutout`.
*   **Bounding Rect Calculation:** The `IslandViewModel` must read `displayCutout.getBoundingRects()` to determine the exact pixel dimensions and coordinates of the physical camera hole. The `IDLE` state pill is generated dynamically to encompass this precise bounding box.
*   **Implementation:** The overlay service attaches the listener to the ComposeView root and records the rects in screen pixels in `CutoutInsets`, together with the display size; `CameraCutoutDetector.detect` prefers them while that size matches, then falls back to `Display.cutout`, then to the status-bar estimate. `PillLayout.effectiveWidthDp` keeps the pill at least the hole's width plus 4dp a side (`HOLE_MARGIN_DP`).
*   **Status Bar Avoidance:** The baseline Y-axis offset and maximum height of the small state must be calculated against the device's native status bar height to prevent the island from overlapping system icons (battery, wifi, time) when resting.

### 9.2 Screen Orientation Handling
Because the island is a physical hardware mask, it cannot arbitrarily float in software space when the device rotates.
*   **Orientation Listener:** The service must monitor `onConfigurationChanged` for transitions between `ORIENTATION_PORTRAIT` and `ORIENTATION_LANDSCAPE`.
*   **Landscape Anchoring:** In landscape, the physical cutout rotates to the lateral edge of the screen. The island must remain anchored to the physical `displayCutout` bounding rects, moving to the left or right bezel.
*   **Landscape State Constraints:** When docked on the lateral edge (landscape), the `AlertEngine` must suppress the Medium State expansion (which is designed for top-down portrait viewing). All active Ephemeral or Background alerts must render strictly in the Small State or as minimal companion bubbles to prevent obstructing full-screen media or gaming.
*   **Implementation:** The service calls `IslandViewModel.setLandscape` on every configuration change. `ExpansionPolicy.canExpand(hasAlerts, isLandscape)` blocks `expand()`/`toggleExpanded()` while alerts are shown (the idle Large dashboard still opens), `allowAutoExpand` blocks every auto-expand, and turning sideways closes an open card. `CameraCutoutDetector.lateralAnchor` finds the side-bezel rect (side + centre Y). While collapsed in landscape (not in notch mode), `SmartIslandOverlayService.updateWindowLayoutParams` docks the window there: `LandscapeAnchoring.windowPosition` places it (`Gravity.TOP or Gravity.LEFT`) so the main pill encloses the hole, 6dp in from the pill's outer end, centred on the hole vertically and clamped to the screen; the window is then only the pill group wide, the content is laid out centred in it (`isFullWidth = false`) and the whole window is the touch region. Expanded (only the idle dashboard opens sideways) or without a lateral cutout, the window goes back to the top centre.

### 9.3 Multi-Window & Split-Screen Contexts
The island operates globally over the OS, which introduces coordinate conflicts when the user enters split-screen mode.
*   **Absolute Bounds Mandate:** As a `TYPE_APPLICATION_OVERLAY`, the island must ignore the localized UI bounds of the top-most active application window. All layout math, center-X offsets, and width limits must be calculated exclusively using `WindowManager.getCurrentWindowMetrics().bounds` to ensure the overlay maps to the physical display glass, not the software split-screen divider.
*   **Implementation:** `DisplayBounds` (`util/DisplayBounds.kt`) is the one helper: `currentWindowMetrics.bounds` on Android 11+, display metrics before that. The overlay service's window params, touch region and floating-window bounds, and `CameraCutoutDetector`, all read it.
*   **Z-Order Priority:** The island must retain its Z-order priority above the split-screen divider handle to ensure touch events on the island are not intercepted by the OS multi-window resizing protocols.

---

## 10. System UI & Immersive Modes

This section defines the interaction matrix between the overlay and native Android OS UI states, ensuring the island does not obstruct media consumption or create redundant visual clutter.

### 10.1 Full-Screen & Immersive Mode Auto-Hiding
The island must not obstruct full-screen video playback, camera viewfinders, or gaming. Because standard overlays cannot directly read the window flags of other applications, the system relies on system bar visibility.
*   **State Detection:** If the app is granted Accessibility Service privileges, `IslandViewModel` must monitor `WindowInsetsCompat.Type.systemBars()` for visibility changes. Alternatively, it must poll the display metrics to detect when the native status bar height reports as 0.
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
    *   *Implementation:* `PillLayout.maxAlertPillWidthDp(screenWidthDp, pillCenterOffsetDp, iconsManaged, capDp = 280)` keeps the pill (centred at its X offset) 72dp clear of the left edge (clock and notification icons) and 88dp clear of the right (Wi-Fi, signal, battery), capped at 280dp; with the permission it returns the cap. `AlertWidthBudget` supplies the Android inputs and both the overlay service and the composable pass the result to `effectiveWidthDp(..., maxAlertWidthDp)`, which never goes below the base width.
