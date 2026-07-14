# Smart Island Product Roadmap

Last updated: July 2026

## Product direction

Smart Island should become the most reliable, privacy-first, and customizable glanceable activity overlay for Android. It should make important live information easier to act on without replacing the notification shade or collecting personal data.

The roadmap follows three principles:

1. **Reliability before feature count.** Overlay, notification, media, and call behavior must remain stable across Android versions and major OEMs.
2. **Local by default.** Notification content and preferences stay on the device. Any future network feature must be optional, clearly explained, and independently disableable.
3. **Fast and unobtrusive.** The island must use little battery, avoid blocking touches, and never make urgent actions harder to reach.

## Current baseline (v3.x)

Smart Island currently provides:

- A Compose-based floating overlay on Android 8+
- Notification grouping, paging, opening, and dismissal
- Incoming-call, media playback, and battery-charging modes
- App shortcuts and best-effort floating-window launching
- Size, position, corner-radius, color, and theme customization
- Local DataStore settings with no Internet permission or analytics
- Unit tests, an overlay smoke test, strict lint, and automated APK releases

This is a strong feature-complete baseline. The immediate constraint is platform and device reliability, not a lack of visible features.

## Roadmap at a glance

| Phase | Target window | Theme | Exit outcome |
| --- | --- | --- | --- |
| 3.1 | Jul 2026 | Welcome and community release | New users receive a clear introduction and direct access to project support channels |
| 3.2 | Jul-Sep 2026 | Fix pill touch and add safe lock-screen support | A dependable pill on unlocked and locked devices, followed by documented compatibility and diagnostics |
| 3.3 | Oct-Nov 2026 | Onboarding and control | Users can set up, filter, pause, and recover the island without confusion |
| 3.4 | Dec 2026-Jan 2027 | Richer live activities | Timer, navigation, and progress experiences work through a common mode architecture |
| 4.0 | Jan-Mar 2027 | Adaptability and polish | Per-app behavior, profiles, accessibility, and responsive layouts are production-ready |
| Later | Apr 2027 onward | Ecosystem and distribution | Sustainable distribution and carefully scoped integrations without weakening privacy |

Dates are planning targets, not promises. A phase moves forward only after its release gates are met.

## Released — v3.1: Welcome and community

Released July 15, 2026.

- Added a first-run welcome dialog describing Smart Island's privacy-first experience.
- Added direct GitHub and Telegram community actions.
- Persisted welcome completion locally so the dialog appears only once.
- Improved support links and updated Android release tooling.

See [CHANGELOG.md](CHANGELOG.md) for the complete release notes.

## Phase 1 — v3.2: Fix pill touch, support the lock screen, and stabilize

**Goal:** Make the pill reliably interactive everywhere it is shown, then make it safely available on the lock screen.

### Priority 0 — fix the pill touch issue

This is the first release blocker. No new activity mode should ship until collapsed-pill taps and gestures are dependable.

- Reproduce and document failures by Android version, display density, cutout position, pill position, and OEM.
- Audit the collapsed overlay window bounds, Compose hit target, touchable region, pass-through inset calculation, and transitions between collapsed and expanded windows.
- Ensure the pill captures taps and supported swipes only inside its visible hit area while all space outside the pill passes through to the app underneath.
- Prevent stale bounds during animation, density/configuration changes, rotation, theme changes, notification replacement, and service recreation.
- Add debug visualization for the actual overlay bounds and touchable region, available only in debug builds.
- Add instrumented tests for tap-to-expand, swipe up, swipe down, horizontal paging, outside-touch pass-through, rapid repeated input, and notification changes during a gesture.

**Acceptance criteria:** 100 consecutive taps and each supported gesture succeed on every reference device; touches immediately outside the visible pill reach the underlying app; and rotation, process recreation, or notification updates do not create a dead zone.

### Priority 1 — add Smart Island to the lock screen

- Add an opt-in **Show on lock screen** setting, disabled by default on upgrade until the user reviews its privacy behavior.
- Detect locked, unlocked, and secure-keyguard state and update overlay behavior immediately when that state changes.
- Provide lock-screen privacy choices: hide all content, show app/icon only, or show full content. Default to app/icon only.
- Allow glanceable status for calls, media, alarms/timers, charging, and permitted notifications while locked.
- Require device authentication before opening an app, executing sensitive notification actions, showing hidden content, replying, or launching a floating window.
- Respect Android lock-screen notification visibility, secret/private notification metadata, Do Not Disturb, and system security restrictions. Never attempt to bypass the keyguard.
- Prevent the island from covering emergency, PIN/pattern/password, biometric, or critical system UI. Fall back to non-interactive or hidden mode when a safe touch surface cannot be guaranteed.
- Add tests for screen off/on, secure and non-secure locks, biometric/PIN flows, content visibility levels, incoming calls, media controls, rotation, and unlock transitions.

**Acceptance criteria:** no private notification text appears under the default setting; sensitive actions always require authentication; the pill never blocks authentication or emergency controls; and lock/unlock transitions do not duplicate or strand overlay windows.

### Product work

- Add a first-run compatibility check for overlay permission, notification access, battery optimization, and OEM background restrictions.
- Add a local diagnostics screen showing permission state, service state, last handled event category, and app version. Provide a user-triggered copy/export action that redacts notification content by default.
- Add clear recovery actions when Android revokes access or kills the overlay service.
- Publish a compatibility matrix covering Android 8-16 and the most reported Samsung, Xiaomi/Redmi, OnePlus, Oppo/Realme, Vivo, Pixel, and Motorola behaviors.
- Reconcile version naming across the app, README, changelog, APK, and Git tags.

### Engineering work

- Add lifecycle and command-state tests around listener reconnects, rapid notification replacement, service restart, process recreation, and permission revocation.
- Expand instrumented tests for collapsed/expanded rendering and gestures on multiple SDK levels.
- Add release smoke checks for minified builds and settings migration.
- Measure cold start, overlay appearance latency, idle memory, and idle battery use with repeatable benchmark scripts. Store only aggregate test results in the repository.
- Remove committed signing material from the repository and rotate the release key if any real signing secret has ever been exposed. Keep signing credentials only in the release secret store.

### Release gates

- The pill-touch acceptance criteria pass before lock-screen work is promoted to beta.
- Lock-screen support passes its privacy and secure-keyguard acceptance criteria.
- No known crash in the core notification-to-overlay path.
- Unit tests, lint, debug build, release build, and instrumented smoke tests pass.
- Overlay appears within 500 ms of a handled event on the reference devices.
- No persistent foreground-service restart loop.
- Setup and recovery are documented for the supported OEM test set.

## Phase 2 — v3.3: Better onboarding, filtering, and everyday control

**Goal:** Give users confidence and control over when the island appears.

### Product work

- Replace permission setup with a guided checklist that explains the benefit and risk of each permission before opening Android settings.
- Add per-app allow/deny controls and category controls for calls, messages, media, progress, battery, and other notifications.
- Add quiet hours, temporary pause, and "hide while" rules for fullscreen apps, landscape video, screen sharing, and Do Not Disturb where platform APIs permit.
- Add backup and restore to a local file for non-sensitive preferences; exclude notification content.
- Improve empty states, permission-loss messages, and in-app help based on common support questions.
- Add a short, skippable interactive tutorial using the existing demo modes and gesture playground.

### Engineering work

- Version the settings schema and test forward migrations and malformed imports.
- Separate notification eligibility rules from presentation priority so each can be tested independently.
- Cache installed-app metadata efficiently and refresh it on package changes.
- Add screenshot and accessibility checks for light/dark themes, large fonts, RTL, and reduced-motion settings.

### Release gates

- A new user can complete setup and trigger a demo in under two minutes.
- Every supported mode can be disabled independently.
- Settings survive upgrade, process death, backup, and restore.
- Core screens remain usable at 200% font scale and with TalkBack.

## Phase 3 — v3.4: Richer live activities

**Goal:** Expand utility through reusable, well-tested activity modes rather than one-off UI branches.

### Product work

- Add timer, stopwatch, alarm, download/upload progress, and calendar-event modes where reliable notification metadata is available.
- Add navigation glance mode for supported navigation notifications, initially showing the next instruction, distance, and app-open action.
- Add smarter priority and queue behavior so calls and time-critical events pre-empt passive progress updates without losing the previous item.
- Add action templates for reply, mark as read, pause/resume, stop, and open when the originating notification safely exposes them.
- Add user-selectable auto-collapse duration by mode.

### Engineering work

- Introduce a common mode-renderer contract with capability flags for progress, artwork, actions, priority, timeout, and privacy level.
- Build parser fixtures from synthetic notifications; never commit real personal notification payloads.
- Add deterministic priority/queue tests and visual regression coverage for every mode.
- Gracefully fall back to a generic notification card when an OEM or app omits expected metadata.

### Release gates

- New modes do not regress call or media handling.
- Unsupported apps always fall back safely.
- Priority behavior is deterministic under simultaneous events.
- Each new mode ships with parser, state, UI, and accessibility tests.

## Phase 4 — v4.0: Personalization, accessibility, and responsive design

**Goal:** Make Smart Island adapt naturally to different users, screens, and contexts.

### Product work

- Add profiles such as Default, Work, Gaming, Driving, and Bedtime with manual switching first.
- Add per-app size, position, color, timeout, and expansion behavior.
- Add device-layout presets for centered cameras, corner cameras, tablets, foldables, landscape, and cutout-free screens.
- Add reduced motion, high contrast, color-independent indicators, larger touch targets, and complete screen-reader labels.
- Add optional import/exportable theme presets with a human-readable, versioned format.
- Redesign the settings information architecture if usability testing shows the dashboard no longer scales.

### Engineering work

- Move large mode-specific UI and service responsibilities behind clear domain interfaces.
- Add window-size and fold-state adaptation tests.
- Establish performance budgets for recomposition, animation frame time, memory, and battery.
- Add Baseline Profiles and startup benchmarks for release builds.

### Release gates

- No major layout breakage across phone, tablet, foldable, portrait, and landscape reference configurations.
- Accessibility audit has no critical findings.
- Profile changes are reversible and cannot strand the overlay off-screen.
- v3 settings migrate automatically with a tested rollback-safe path.

## Later opportunities — validate before committing

These ideas should enter development only after user research or issue demand demonstrates value:

- Play Store and F-Droid distribution, subject to overlay, notification-access, foreground-service, signing, and reproducible-build requirements.
- Optional, privacy-preserving update checks. This would require the Internet permission and therefore needs a separate product and privacy decision.
- Companion integrations with wearables, desktop devices, or automation apps through explicit opt-in interfaces.
- A documented extension model for community-contributed notification parsers or visual modes, if it can be sandboxed and kept maintainable.
- Localization beyond the initial language set, led by community-reviewed translations.

## Explicit non-goals

- Storing or syncing notification content to a server by default
- Replacing the Android notification shade or lock screen
- Circumventing Android security, background execution, or OEM restrictions
- Advertising, behavioral profiling, or selling usage data
- Shipping app-specific hacks without a generic fallback and automated tests

## Success measures

Because the app currently has no analytics or Internet permission, success should be measured with privacy-safe operational signals:

| Area | Measure | Initial target |
| --- | --- | --- |
| Reliability | Crash-free core test runs and unresolved core-path reports | Zero known release-blocking core crashes |
| Setup | Moderated users completing setup and demo | At least 90% without developer help |
| Responsiveness | Event-to-overlay latency on reference devices | p95 under 500 ms |
| Efficiency | Overnight idle battery impact in controlled testing | Under 1% over 8 hours on reference devices |
| Compatibility | Maintained device/Android test matrix | Android 8-16 plus six major OEM families |
| Quality | Required CI and release gates | 100% passing before release |
| Community | Median first response to reproducible bug reports | Under seven days |

Targets should be revised after the v3.2 reliability baseline is measured. Do not add telemetry merely to satisfy a metric; prefer opt-in surveys, GitHub issue labels, release downloads, and controlled device tests.

## Delivery process

1. Maintain one GitHub milestone per planned release and label work as `reliability`, `privacy`, `accessibility`, `performance`, `feature`, or `documentation`.
2. Prioritize work using: user impact, number of affected devices, privacy/security risk, confidence, and implementation cost.
3. Keep no more than one major mode and one platform-quality initiative in active development at once.
4. Release small betas from the `dev` branch, publish known limitations, and promote only after the release gates pass.
5. Require every feature proposal to specify fallback behavior, permissions, battery impact, accessibility, tests, and removal/migration strategy.
6. Review this roadmap after every minor release and at least once per quarter.

## Recommended next five issues

1. **Release blocker: fix collapsed-pill touch handling.** Reproduce failures, correct bounds and touch-region synchronization, and meet the repeated-input/pass-through acceptance criteria.
2. **Core feature: implement privacy-safe lock-screen support.** Start with the opt-in setting, keyguard state model, redacted default UI, and authenticated sensitive actions.
3. **Security: remove and rotate exposed signing material.** Audit tracked files and release history, remove secrets from the working tree, rotate if required, and document secret-only signing.
4. **Quality: create the Android/OEM compatibility matrix.** Define reference devices/emulators and a repeatable unlocked/locked core-flow checklist.
5. **Reliability: test service death and listener reconnect.** Cover process recreation, permission loss, rapid event replacement, and lock/unlock transitions.
