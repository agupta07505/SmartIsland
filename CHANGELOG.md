# Changelog

All notable changes to Smart Island should be documented in this file.

The format is inspired by Keep a Changelog, and this project uses the GNU General Public License v3.0.

## [3.2] - 2026-07-18

### Added

- **System Notification Suppression & Listener Service (`SmartIslandNotificationListenerService`)**: Implemented system notification filtering (`NotificationFilter`) and notification listener integration to present notifications in the island while suppressing duplicate heads-up alerts and system shade entries when active.
- **Landscape Orientation Listener (`IslandOrientationListener`)**: Added automatic orientation change detection to dynamically remove and hide the Smart Island overlay in landscape mode, ensuring uninterrupted full-screen gaming and video playback.
- **Battery Optimization Setup Section**: Expanded the Permissions configuration screen with a dedicated guidance card for setting Battery Optimization to 'No restrictions' to ensure OS background execution protection.
- **Lock Screen Privacy & Controls**: Introduced lock screen visibility settings (`showOnLockScreen`, `hideSensitiveOnLockScreen`) to allow opt-in island display while respecting user privacy.
- **Strict Permission Guard**: Enforced runtime permission validation before enabling the main Smart Island service toggle switch.
- **Overlay System Warning Toggle**: Added a direct settings action to guide users on hiding the persistent "displaying over other apps" system notification.
- **Community Contributions & PR Merges**: Integrated Pull Request [#11](https://github.com/agupta07505/SmartIsland/pull/11) contributed by [@likhithkrishna1103-tech](https://github.com/likhithkrishna1103-tech) (Likhith Krishna) bringing foundational improvements to lock screen privacy settings, hidden API restriction bypass for touchable regions, notification icon handling, and music player responsiveness.

### Changed

- **Pass-Through Touch Region Registration**: Bypassed hidden API restrictions (`WindowTouchBounds`) to refine non-touchable window inset boundaries, restoring reliable tap and gesture pass-through to underlying applications.
- **App Shortcuts & Visual Spacing**: Updated expanded island app shortcut grid rendering and improved overall dashboard card padding and layout spacing.
- **Documentation & UI Assets**: Refreshed high-resolution application screenshots showcasing updated permissions, positions, customizations, app shortcuts grid, and wavy music player.
- **Release Version**: Updated application target version to `3.2` (versionCode `3`).

### Fixed

- **Collapsed Pill Touch Reliability**: Resolved non-responsive tap and gesture detection on the collapsed island pill.
- **Large Window Expansion Shifting**: Fixed content jump and height layout shifting when expanding notifications or switching pages.
- **App Clear Crash**: Fixed background service crash when clearing the application from the recent apps launcher.
- **Overlay Memory & Lifecycle**: Resolved service lifecycle leakage and improved state persistence across theme changes and service restarts.

### Planned next

- Onboarding wizard & setup checklist enhancements for first-time users.
- Custom notification filtering rules by package and priority level.
- Dynamic island shape templates and expansion animation presets.

## [3.1] - 2026-07-15

### Added

- **First-Run Welcome Experience**: Added a polished welcome dialog that introduces Smart Island's privacy-first approach and gives new users a clear starting point.
- **Community Shortcuts**: Added direct actions in the welcome dialog for starring the project on GitHub and joining the Smart Island Telegram community.
- **Persistent Welcome State**: Added a local DataStore preference so the welcome dialog is shown only once after installation.

### Changed

- **Support & Feedback**: Improved community links and support actions, including clearer access to the Telegram community.
- **Release Tooling**: Updated GitHub Actions dependencies used by the Android build and release workflow.
- **Release Version**: Updated the application version to `3.1`.

### Planned next

- Fix collapsed-pill touch reliability as the first v3.2 release blocker.
- Add opt-in, privacy-safe Smart Island support on the lock screen after the touch fix is verified.

## [3.0.0] - 2026-07-10

### Added

- **Interactive Gesture Guide**: Added a dedicated "Gesture guide" option to the home screen settings dashboard routing to an interactive tabbed guide (`GesturesSection.kt`).
- **Looping Gesture Animations**: Built looping animations with pulsing finger paths demonstrating Swipe Up (dismiss), Swipe Down (launch in popup/floating window), and Swipe Left/Right (horizontal paging) on a mock status bar island.
- **Try-It-Yourself Playground**: Added interactive gesture detectors utilizing drag thresholds to let users horizontally/vertically swipe the mock island and trigger responses live in a sandbox.
- **App Shortcuts**: Added a shortcut picker for selecting up to eight installed apps and opening them directly from the expanded island, with recent apps used as a fallback.
- **Expanded Color Customization**: Added preset palettes and a custom RGB picker for the notification dot, battery indicator, and music visualizer.
- **Dark Theme Support**: Added theme-aware settings and overlay styling for improved readability in dark mode.

### Changed

- **Key-Based Height Synchronization**: Refactored the dynamic height tracking (`pageHeights`) in the expanded pager to key by unique notification ID instead of page index, permanently preventing height-jumping artifacts when notifications update or switch categories.
- **Density-Independent Gesture Limits**: Adjusted the touch offsets and swipe trigger limits to be calculated in DP and translated dynamically to pixels using `LocalDensity`, correcting the horizontal pagination guide metrics and vertical drag thresholds on high and low-DPI devices.
- **Overlay Lifecycle**: Improved service, Compose view-tree, and background lifecycle handling to keep the island stable during recomposition, theme changes, and service restarts.
- **Release Automation**: Refined Android CI artifact naming and added structured GitHub release notes.

### Fixed

- **Expanded Content Sizing**: Fixed shifting, overshooting, and incorrect minimum-height behavior while paging between notifications with different content heights.
- **Swipe-Down Gesture**: Fixed unreliable drag-down recognition when launching an app in a floating window.
- **RGB Picker**: Fixed the custom color picker's RGB slider rendering and selection behavior.
- **Unexpected App Closure**: Fixed multiple lifecycle and animation paths that could force-close the app or overlay.
- **Notification Filtering**: Improved filtering of system and ongoing notifications after removing the unused AndroidX Window dependency.

## [2.2.0] - 2026-07-07

### Added
- **Battery Charging Island Mode**: Complete charging status indicator that slides down automatically when a charger is plugged in (`ACTION_POWER_CONNECTED`).
- **Pulsing Battery & Gradient Animations**: Implemented pulsing charging icons (infinite scale transition) in the collapsed state and flowing multicolor gradient indicators in the expanded state.
- **Time-until-full & Progress**: Dynamic battery percentage and charging-time remaining estimates computed directly via `BatteryManager`.
- **Battery Demo Button**: Added a dedicated "Battery" button to the Quick Test controls on the home screen to preview and test the charging island mode.
- **Unit Tests**: Added unit test suites verifying priority/suppression rules for non-system apps (`NotificationPriorityTest`), media controller action mappings, and `SystemEventReceiver` battery events.

### Changed
- **Reorganized Home Dashboard UI**: Restructured Quick Test buttons into a 2x2 grid layout and repositioned status texts under subtitles for improved spacing and visual appeal.
- **Center Header Alignment**: Center-aligned the main header description text on the home screen.
- **Battery Charging Updates**: Refactored `SystemEventReceiver` to update battery percentages silently without re-triggering expand and auto-collapse cycles.
- **Media Controller Resolution**: Improved media playback robustness by prioritizing active playing sessions (`PlaybackState.STATE_PLAYING`) when resolving media controllers by package name.
- **Better reflection diagnostics**: Switched key reflection-based API hooks (pass-through touch insets and freeform window launching) to use `runCatchingLogged` utility for easier debugging.

### Fixed
- **License Header Typo**: Corrected `GNU GPL v3License` to `GNU GPL v3 License` in workflow, configuration, ignore, and helper files.
- **Architecture Doc Tracked Status**: Removed `analysis.md` from `.gitignore` list to ensure the codebase analysis documentation remains fully tracked in Git.

## [2.1.0] - 2026-07-07

### Added

- **Centralized Notification Repository (`SmartIslandNotificationRepository`)**: Created to manage notification streams and commands flow reactively.
- **Dependency Injection**: Registered `SmartIslandApp` Application class to hold repositories singletons.
- **Material 3 Unified Theme**: Introduced day/night theme structure with DayNight Material3 scheme support.
- **Automated Tests**: Created JUnit/MockK test suites: `IslandModeMappingTest`, `NotificationPriorityTest`, `SmartIslandSettingsTest`.
- **Utility Modules**: Created isolated helper scripts `TimeUtils` and `LogUtils`.

### Changed

- **Modularized UI Structure**: Split monolithic screen `SmartIslandHomeScreen.kt` into clean components: `HeaderSection`, `PermissionsSection`, `PositionsSection`, `SupportSection`, and `AboutSection`.
- **Decoupled Architecture**: Migrated views `IslandOverlayView.kt` and `IslandExpandedContent.kt` from calling static service instances to reactively communicating via repositories.
- **Resource Maintainability**: Moved hardcoded screen UI strings into standard `strings.xml` resource tags.
- **Build Configurations**: Enabled ProGuard/R8 minification, resource shrinking, and strict lint checks in `build.gradle.kts`.

### Fixed

- **License Header Typo**: Corrected `GNU GPL v3License` to `GNU GPL v3 License` globally.
- **Theme Parent Reference**: Migrated parent configuration in `styles.xml` to `Theme.DeviceDefault.NoActionBar` to prevent XML resource linking compilation errors.

## [2.0.0] - 2026-07-05

### Added

- **Custom Wavy Music Seek Bar (`WavyMusicSeekBar`)**: Renders playing progress as a smooth, filled organic wave with custom wave animations that automatically freeze when paused, and flat damping at layout boundaries. Includes a circular thumb for direct drag-to-seek support.
- **Enhanced Player Controls**: Integrated a Song Like button (with outline/fill favorite heart status) on the left side of the skip-back button, and a Repeat/Loop button on the right side of the skip-forward button.
- **Home Screen Dashboard Reorganization**: Redesigned the main menu into neat topic cards: Permissions, Positions, Support & Feedback, and About.
- **Dashboard Slide Transitions**: Implemented slide-in/slide-out horizontal enter/exit animations using Compose `AnimatedContent` for menu subtopics, complete with a system back handler (`BackHandler`) to reverse the slides.

### Changed

- **App Logo Header**: Replaced the visual pill visual placeholder in the home screen header with the actual application icon embedded inside a sleek black background card, adjusted to a filled 60dp icon size.
- **Visual Spacing**: Shifted dashboard and category headers down by applying spacious 36dp top paddings to achieve a modern, relaxed design.
- **Active Session Seeker**: Refactored the notification listener to correctly pass its listener component name, enabling accurate fallback media controllers querying when token actions are restricted.

### Fixed

- **App Startup Crash**: Resolved startup crash on launch due to Compose `painterResource` trying to parse the adaptive vector launcher icon xml. Fixed by dynamically extracting the launcher icon drawable and rendering it to a Canvas-backed bitmap.
- **Overlay "Non-Touchable" Dead Zone**: Corrected height calculation of the collapsed overlay window by removing the status bar height offset and extra padding, keeping the window size tight to the visual bounds.
- **Pill Gestures and Swipe Interceptions**: Fixed direct tap gesture failures in the collapsed pill and swipe-up/down failures by capturing reactive state delegates to prevent stale state retention.
- **Controls State Synchronization**: Fixed state mismatch issues where loop state changes made inside external media apps (e.g., Spotify) did not reflect in the Smart Island overlay. Added custom extras query prioritization and strict toggling logic to avoid double-activation cycles.
- **Live Progress Catchup**: Fixed seek bar staying at its previous position on re-expand by querying live playback state offsets immediately upon window expansion.

## [1.0.0] - 2026-07-04

### Added

- Initial open-source baseline for the Smart Island Android app.
- Floating overlay service with animated collapsed and expanded island states.
- Notification listener integration for notification, incoming-call, and media modes.
- Local customization for island size, position, and corner radius.
- Demo states for notification, call, and music previews.
- App screenshots added to the README to showcase UI features.

### Changed

- Replaced default launcher icon with custom adaptive and legacy icons generated from the transparent logo (`logo.png`) on a pure black background.

### Fixed

- Fixed support/feedback links in the app to correctly load GitHub issue templates (`bug_report.md` and `feature_request.md`).
