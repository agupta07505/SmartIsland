# Pull Request Review Guidelines for GitHub Copilot

When assisting or reviewing Pull Requests for Smart Island:

- **Verify PR Template Completion**: Ensure the PR description details the motivation, type of change, affected app areas, and testing checklist.
- **UI Changes**: If UI/UX was modified, check that before/after screenshots or recordings are provided and do not expose private user notification data.
- **CI / Build Verification**: Confirm that all CI checks pass (`lintDebug`, `testDebugUnitTest`, `assembleDebug`).
- **GPLv3 Compliance**: Ensure copyright headers are preserved or included on new files.
- **Performance & Animations**: Ensure Compose animations run efficiently without triggering high-frequency frame drops or infinite recompositions.
