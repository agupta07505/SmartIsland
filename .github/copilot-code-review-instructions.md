# GitHub Copilot Code Review Instructions

## Project Overview

Smart Island is an open-source Android application (`com.agupta07505.smartisland`) built with Kotlin 2.0+, Jetpack Compose (Material 3), Dagger Hilt, and Android System Services. It transforms notifications, media playback, incoming calls, charging/battery states, timers, stopwatches, navigation, and live activities into a floating Dynamic Island overlay.

- Target SDK: API 36 | Minimum SDK: API 26 | JDK: 17
- License: GNU General Public License v3.0 (GPLv3)

---

## Review Scope & Priorities

When reviewing pull requests and changes, evaluate the code against these priority areas:

### 1. Privacy, Security & Trust Boundaries (CRITICAL)
- **Zero Telemetry / Local-Only Processing**: All notification metadata, bodies, contact names, media titles, and history logs MUST remain strictly on-device.
- **Internet Permission Boundary**: The `android.permission.INTERNET` is used exclusively for opt-in GitHub release checks via `GitHubApiService`, strictly guarded by the `allowNetworkChecks` DataStore setting. Reject any code introducing remote notification transmission, tracking, analytics SDKs, or unauthenticated remote endpoints.
- **Data Hygiene**: Never log sensitive user data (notification messages, contact details, private paths, or keystores) in release builds or logcat.
- **Safe Intent & Component Handling**: Ensure all `PendingIntent` instances specify explicit mutability flags (`FLAG_IMMUTABLE` or explicit `FLAG_MUTABLE` where `RemoteInput` requires it). Ensure exported activities/receivers have proper permission protection.

### 2. WindowManager Overlay & Service Lifecycle (HIGH)
- **Dynamic Focus Switching**: When inline reply is active, ensure the WindowManager overlay switches between `FLAG_NOT_FOCUSABLE` (touch pass-through) and `FLAG_ALT_FOCUSABLE_IM` (IME soft-keyboard input) with `SOFT_INPUT_ADJUST_PAN` / `SOFT_INPUT_ADJUST_RESIZE`.
- **Auto-Collapse Protection**: Ensure `isInputActive` in `IslandViewModel` pauses auto-collapse while the user is actively typing.
- **Touch Pass-Through Insets**: Verify that `OnComputeInternalInsetsListener` calculations properly bound touches to the physical pill area when collapsed, allowing user interaction with background apps.
- **Lifecycle & Resource Cleanup**: Verify that broadcast receivers, coroutine jobs, and WindowManager views are cleanly unregistered/removed during service destruction to prevent memory and window leaks.
- **Landscape Handling**: Ensure overlay is automatically hidden during landscape orientation to prevent blocking full-screen apps and video playback.

### 3. Notifications, Clock Parsers & Media Handling (MEDIUM)
- **Full-Color Icon Fidelity**: Verify application launcher icons are loaded via `loadAppIconBitmap(packageName)` with LRU caching, avoiding monochromatic 1-bit status bar silhouettes.
- **Sync Notification Suppression**: Ensure background message polling and sync alerts (*"Syncing messages..."*, *"Checking for messages..."*) are suppressed from triggering download mode.
- **Timer & Stopwatch Parsing**: When modifying `TimerStopwatchParser`, ensure parsing rules handle standard time formats (`MM:SS`, `HH:MM:SS`) and multi-OEM notifications (Google, Samsung, Xiaomi/HyperOS, ColorOS, Huawei) without throwing exceptions on malformed strings.
- **Expanded Pager Snapping**: Horizontal notification pagers in `IslandExpandedContent` must synchronize with `pagerState.settledPage` and guard programmatic scrolling with `!pagerState.isScrollInProgress`.

### 4. Jetpack Compose & State Management (MEDIUM)
- Ensure all state is lifecycle-aware and properly hoisted via `StateFlow` and `IslandViewModel`.
- Avoid heavy or blocking I/O on the main thread; use `Dispatchers.IO` for DataStore, SQLite, or network checks.
- Adhere to Material 3 design tokens and dynamic color schemes.
- Avoid unnecessary recompositions by using stable data classes and correct `key` parameters in lazy layouts and pagers.

### 5. Code Style, Testing & Documentation (LOW)
- Every new feature, parser, or repository method must include corresponding JUnit 4 / MockK unit tests under `app/src/test/`.
- Maintain GPLv3 license headers on all new files.
- Ensure changes pass `./gradlew lintDebug testDebugUnitTest assembleDebug` cleanly with zero warnings/errors.

---

## Review Output Format

Provide constructive, specific, and actionable feedback. When identifying issues, structure comments as follows:

`[Severity: Critical | High | Medium | Low] Short Title`
- **What is the problem:** Exact description of the issue or edge case.
- **Why it matters:** Impact on privacy, performance, lifecycle, or user experience.
- **Suggested fix:** Concrete Kotlin / Compose code snippet or architectural resolution.
