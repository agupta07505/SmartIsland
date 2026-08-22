# Code Review Guidelines for GitHub Copilot

Apply these checks whenever reviewing code changes in Smart Island:

1. **Privacy Guard**:
   - Check if any new network calls or third-party SDKs are introduced.
   - Confirm all notification data remains strictly on-device.
   - Confirm `allowNetworkChecks` guards any network requests to GitHub releases.

2. **Overlay & Touch Safety**:
   - Verify WindowManager view removal on service destroy.
   - Verify non-touchable region reflection calculations for collapsed pill.
   - Check soft-keyboard IME focus handling during inline reply (`FLAG_NOT_FOCUSABLE` <-> `FLAG_ALT_FOCUSABLE_IM`).

3. **Compose & Lifecycle**:
   - Avoid side effects directly in composition.
   - Verify state collection uses lifecycle-aware patterns (`collectAsStateWithLifecycle` or repository Flow bindings).
   - Use Material 3 color tokens and surface containers.

4. **Testing**:
   - Verify unit tests exist for newly added logic, regex parsers, and viewmodel states.
