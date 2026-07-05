# Changelog

All notable changes to Smart Island should be documented in this file.

The format is inspired by Keep a Changelog, and this project uses the GNU General Public License v3.0.

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
