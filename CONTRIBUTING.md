# Contributing To Smart Island

Thanks for helping make Smart Island better. This project is public, so please keep issues, pull requests, screenshots, and logs free of private notification content, secrets, keystores, tokens, and local machine paths.

## Ways To Contribute

- Report bugs with clear reproduction steps.
- Suggest focused feature ideas.
- Improve Compose UI, animations, accessibility, or Android compatibility.
- Add tests, documentation, and device-specific notes.
- Translate Smart Island into your language or polish existing strings (see `app/src/main/res/values-*/` and `scripts/translate_strings.py`).
- Refactor carefully when it reduces real complexity.

## Development Setup

Requirements:

- JDK 17
- Android Studio with Android SDK 36
- Android 8.0+ device or emulator

Build locally on Windows:

```powershell
.\gradlew.bat assembleDebug
```

Build locally on macOS or Linux:

```bash
./gradlew assembleDebug
```

Install on a connected device:

```bash
./gradlew installDebug
```

## Pull Request Checklist

- The app builds with the Gradle Wrapper.
- Overlay and notification-listener flows were tested when relevant.
- User-facing changes are documented in `README.md` or `CHANGELOG.md` when needed.
- No generated APKs, AABs, build folders, keystores, secrets, or personal notification screenshots are committed.
- New code follows the existing Kotlin and Compose style.
- The pull request explains the reason for the change, not only the implementation.

## Code Style

- Prefer small, focused changes over broad rewrites.
- Keep UI state explicit and lifecycle-aware.
- Use existing models and repositories before adding new abstractions.
- Add comments only for behavior that is not obvious from the code.
- Keep public documentation clear, friendly, and accurate.

## Reporting Bugs

Use the bug report issue template and include:

- Android version and device model
- App version or commit hash
- Steps to reproduce
- Expected behavior
- Actual behavior
- Relevant logs with private notification content removed

## License Of Contributions

This project is licensed under the GNU General Public License v3.0. By submitting a contribution, you agree that it may be distributed under the same license.
