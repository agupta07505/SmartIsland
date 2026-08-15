# SmartIsland Copilot Instructions

## Project Context

SmartIsland is an Android overlay application that displays notifications,
calls, media, charging & battery status, navigation, live activities, downloads,
flashlight, screen recording, bluetooth devices, and system events inside a floating
Dynamic Island-style interface.

Project details:

- Package: `com.agupta07505.smartisland`
- Language: Kotlin
- UI: Jetpack Compose and Material 3
- Dependency injection: Hilt
- State: Kotlin Coroutines, StateFlow and DataStore Preferences
- Platform integration: NotificationListenerService, foreground services,
  WindowManager overlays, system broadcast receivers and Shizuku
- Minimum Android version: Android 8.0 / API 26
- Compile and target SDK: API 36
- Java and Kotlin JVM target: 17
- License: GNU GPL v3

SmartIsland is privacy-first. Notification data and settings must remain on
the device. The app currently has no INTERNET permission, remote analytics
or remote notification processing. Do not introduce networking, analytics,
tracking or notification-content transmission unless the pull request
explicitly documents and justifies that product-level change.

## Architecture

Preserve the existing event flow:

1. `SmartIslandNotificationListenerService` receives notification and media events.
2. `NotificationFilter` decides filtering, suppression, and mode classification.
3. `SystemEventReceiver` listens to battery, power, bluetooth, and system broadcasts.
4. `SmartIslandNotificationRepository` manages centralized notification & system island state.
5. `SmartIslandOverlayService` manages the floating WindowManager overlay lifecycle.
6. `WindowManager` hosts the Jetpack Compose collapsed pill and expanded cards.

Use existing models, repositories, services and utilities before creating
new abstractions. Prefer small, focused changes over broad rewrites.

## Island Modes & Event Handling

SmartIsland supports the following primary modes:

- `Notification`: Standard incoming app notifications with full-color app launcher icons.
- `IncomingCall`: Ringing / active phone call timer and caller badge.
- `Music`: Media playback controls, live progress scrubber, animated visualizer, and artwork.
- `Battery`: 
  - *Charging*: Green electric flow gradient with remaining charge time.
  - *Low Battery* ($\le 20\%$): Pulsing red warning glyph with live battery percentage.
  - *Battery Saver*: Warm amber energy savings glyph with live battery percentage.
- `LiveActivity`: Real-time tracking and delivery updates (e.g. Uber, delivery apps).
- `Navigation`: Turn-by-turn routing indicators and directions.
- `DownloadUpload`: Active file transfers with animated progress meters. Background message synchronization/polling (e.g. Snapchat, WhatsApp) is explicitly excluded.
- `Hotspot`: Tethering status and connected client counter.
- `Bluetooth`: Device connectivity and battery level.
- `Flashlight`: Torch state indicator and toggle controls.
- `ScreenRecording`: Active recording elapsed timer and controls.

## General Development Rules

- Follow `CONTRIBUTING.md`, `PRIVACY.md`, `SECURITY.md` and the existing code style.
- Use the committed Gradle Wrapper. Do not require system Gradle.
- Keep compatibility with API 26 through API 36.
- Preserve existing copyright and GPL headers.
- Never commit secrets, tokens, keystores, signing values, local paths,
  generated APKs or private notification information.
- Do not log notification titles, messages, contact names, media information
  or other sensitive user content in production.
- Do not add a dependency or Android permission without explaining why it
  is necessary.
- Avoid unrelated formatting, renaming and refactoring in focused changes.
- Update `README.md` or `CHANGELOG.md` when user-visible behaviour changes.
- Do not modify version codes, version names or release signing unless the
  pull request is specifically about a release.

## Code Review Priorities

When reviewing a pull request, focus on concrete problems introduced by the
changed code. Do not report existing unrelated problems or subjective style
preferences.

### Privacy and Security

Check for:

- Notification or personal information being logged, stored insecurely or shared.
- New INTERNET, storage, accessibility or privileged permissions.
- Exported activities, services or receivers without appropriate protection.
- Unsafe Intent, PendingIntent or URI handling.
- Incorrect PendingIntent mutability flags.
- Secrets, credentials, signing files or private paths committed to Git.
- Shizuku operations without permission checks, failure handling or fallback.
- Lock-screen exposure of sensitive notification content.

Treat privacy regressions, credential exposure and unsafe exported components
as high-severity findings.

### Android Service and Overlay Lifecycle

Check for:

- Foreground services not calling `startForeground` correctly.
- Services, receivers, listeners or callbacks that are not unregistered.
- WindowManager views added multiple times or not removed safely.
- Window leaks, duplicated overlays or crashes during service restart.
- Coroutines continuing after their owning service or UI is destroyed.
- Incorrect handling of configuration, orientation or process recreation.
- Missing permission checks before displaying an overlay.
- Overlay flags that block touches outside the island.
- Regressions in landscape auto-hide, lock-screen behaviour or touch pass-through.
- Behaviour that may fail on API 26 or common OEM Android versions.

### Notifications, App Icons and Media

Check for:

- **App Icon Fidelity**: `SmartIslandNotificationListenerService` must load full-color application launcher icons via `loadAppIconBitmap(packageName)`. Do not substitute monochromatic 1-bit status bar `smallIcon` for the main app launcher icon in the collapsed pill or expanded card.
- **Message Sync Exclusion**: Background polling or message sync notifications (e.g. *"Syncing messages..."*, *"Checking for messages..."*) must be suppressed from hijacking the download/upload progress mode.
- **Expanded Pager Snapping**: Horizontal notification pagers in `IslandExpandedContent` must synchronize against `pagerState.settledPage` and guard programmatic scrolling with `!pagerState.isScrollInProgress` to avoid mid-swipe freezing.
- Duplicate notifications or incorrect system-shade suppression.
- Stale notifications remaining after cancellation.
- Invalid or cancelled notification actions.
- Media state, progress or album artwork becoming stale.
- Call actions being executed without checking permissions or current call state.
- Race conditions between notifications, calls, charging and media events.
- A single malformed notification causing the listener service to crash.

Notification suppression must fail safely. SmartIsland must not accidentally
remove or hide a notification that it cannot represent correctly.

### Jetpack Compose

Check for:

- State that is not observable, lifecycle-aware or properly hoisted.
- Incorrect `remember`, `rememberSaveable`, `LaunchedEffect` or `DisposableEffect` keys.
- Side effects performed directly during composition.
- Infinite recomposition or unnecessary high-frequency recomposition.
- Animations that shift the island position, flicker or leave stale state.
- Long-running or blocking work on the main thread.
- Missing accessibility semantics, content descriptions or usable touch targets.
- Hard-coded colours that break dark theme or dynamic colour behaviour.

Preserve smooth animations and avoid expensive work during every animation frame.

### State, Coroutines and DataStore

Check for:

- Mutable shared state accessed from multiple threads without protection.
- Flow collection that is not lifecycle-aware.
- Jobs that are not cancelled when their owner stops.
- Blocking I/O on the main thread.
- DataStore key changes that lose existing user settings.
- Missing or unsafe defaults for newly introduced preferences.
- Race conditions that can show an incorrect island mode.

### Build, CI/CD and Compatibility

Check whether the change:

- Compiles with JDK 17 and Android SDK 36.
- Preserves minimum API 26 support.
- Uses official stable GitHub Actions versions (`actions/checkout@v4`, `actions/setup-java@v4`, `actions/upload-artifact@v4`, `actions/download-artifact@v4`).
- Passes the full local CI command with zero errors:
  ```bash
  ./gradlew --no-daemon --stacktrace lintDebug testDebugUnitTest assembleDebug
  ```
- Breaks release shrinking or requires an appropriate ProGuard rule.
- Introduces lint errors or unhandled `NewApi` violations on supported API levels (API 26–36).

## Review Comment Format

Only leave a comment when there is an actionable issue.

Use this format:

`[Severity] Short problem title`

Then explain:

- What can go wrong.
- The exact condition that triggers it.
- Why it matters to SmartIsland users.
- A focused fix or code suggestion.

Severity levels:

- `Critical`: credential exposure, serious privacy leak or unusable application.
- `High`: crash, data loss, security issue or major feature regression.
- `Medium`: incorrect behaviour, lifecycle leak or meaningful compatibility issue.
- `Low`: minor but real maintainability, accessibility or performance problem.

Do not claim that something fails unless the failure can be traced to the
changed code. Ask a question when required context is missing.
