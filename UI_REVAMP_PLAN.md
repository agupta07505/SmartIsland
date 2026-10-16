# Smart Island — Apple HIG UI Revamp: Plan & Checklist

Branch: `UI_upgrade`. Android/Compose app, so Apple's HIG is applied as **principles** (the skill's rule for non-Apple platforms): accessibility, color, typography, layout, writing, materials, motion. Platform mechanics that don't exist on Android (SF Symbols, Dynamic Type API) are translated: `sp` text scaling, Material icons matched to text weight.

## Review findings that drive the plan (from reading the code)

| # | Finding | HIG source | Severity |
|---|---|---|---|
| 1 | 123 hard-coded `fontSize` values; 48 at 11sp, 12 at 10sp (below the 11pt floor); no type scale | `typography.md › Ensuring legibility` | High |
| 2 | 134 hard-coded `Color(0x…)` literals bypass the theme and break dark mode / contrast | `color.md › Best practices` | High |
| 3 | Controls under 44dp: slider ± buttons are 24dp, icon buttons 32dp, mode chips ~34dp | `accessibility.md`, skill Lens 1 | Critical |
| 4 | 61 literal `contentDescription` strings (English only, not localized) | `accessibility.md › Vision` | Medium |
| 5 | Status shown by color alone in places (dot + tinted text) | `color.md › Inclusive color` | Medium |
| 6 | Settings is a stack of bordered cards with heavy tinted icon tiles; no grouped-list hierarchy, no large titles | `settings.md`, `layout.md › Visual hierarchy` | High |
| 7 | Accent #D84315 as fill on chips/borders/values everywhere: one color meaning many things | `color.md › Best practices` | High |
| 8 | Weights ExtraBold/Black and 0.5–1.5dp borders compete for attention | `typography.md › Conveying hierarchy` | Medium |
| 9 | Motion has no Reduce Motion path | `accessibility.md`, `motion.md` | Medium |
| 10 | Overlay expanded cards each define their own spacing, radii and type | `layout.md › Visual hierarchy` | Medium |

**Thesis:** a quiet, content-first utility. The island itself (black pill, live content) is the one signature element; the settings app stays calm, grouped and legible so the island is what you remember.

## Design system (tokens)

- **Accent:** keep brand burnt orange only for primary actions, switches-on and selection (`#C73A10` light, `#FF7A4D` dark). Everything else is neutral grays.
- **Surfaces (light / dark):** grouped background `#F2F2F7` / `#000000`; grouped row surface `#FFFFFF` / `#1C1C1E`; separator `#3C3C43` at 29% / `#545458` at 60%.
- **Text:** label `#000` / `#FFF`; secondary label `#3C3C43` 60% / `#EBEBF5` 60% (checked ≥ 4.5:1).
- **Semantic status:** green `#248A3D` / `#30D158`, amber `#B25000` / `#FF9F0A`, red `#D70015` / `#FF453A`, each always paired with an icon or text.
- **Type scale (sp):** Large Title 34/bold, Title 1 28, Title 2 22, Title 3 20/semibold, Headline 17/semibold, Body 17, Callout 16, Subhead 15, Footnote 13, Caption 12, Caption 2 11 (floor). Max weight Bold.
- **Shape:** grouped section radius 12dp; controls 10dp; island expanded card 32dp; chips fully rounded.
- **Layout:** 16dp screen margin (20dp ≥ 600dp), 44dp minimum touch target, 8dp grid.
- **Motion:** existing island spring (520 / 0.72) kept; settings transitions 300ms ease; every non-essential animation gated by system animator scale (Reduce Motion).

## Checklist

### Phase 0 — Setup
- [x] Read HIG references (accessibility, layout, typography, color, settings, materials)
- [x] Locate JDK 17, Android SDK 36, adb; phone connected over Wi-Fi ADB
- [x] Baseline `testDebugUnitTest assembleDebug` green before any change

### Phase 1 — Foundation
- [x] `Theme.kt`: new light/dark color schemes + extra semantic colors (`LocalSemanticColors`)
- [x] `Type.kt`: Apple-style type scale wired into `MaterialTheme.typography`
- [x] `Shapes` + `Dimens` tokens
- [x] Reduce-motion helper (`rememberReduceMotion`)
- [x] Shared components: `GroupedSection`, `GroupedRow` (icon, title, subtitle, value, chevron, switch), `AppleSwitch` styling, `LargeTitleScaffold`, `InlineNavBar`, `StatusLabel` (icon + text), `PrimaryButton`/`TintedButton`
- [x] `SliderSettingItem`: 44dp ± targets, value label, a11y semantics
- [x] `ClickableRowItem`, `PermissionCard` rebuilt on the new row

### Phase 2 — App shell and settings
- [x] `SmartIslandHomeScreen`: large-title header, translucent bottom tab bar with text labels, status as icon+text
- [x] Studio tab: power card, simulation lab (44dp chips, single-accent selection)
- [x] Position tab (`PositionsSection`)
- [x] Settings overview: inset-grouped lists replace bordered cards
- [x] Detail screens get a standard back-chevron nav bar: Notifications & Privacy, App Shortcuts, Notification History, Colors (Customizations), Gestures, Permissions, Backup & Restore, About, Support, Developer Options
- [x] Dialogs (welcome, confirms, pickers) restyled; destructive actions have Cancel
- [x] Localized `contentDescription`s (new string resources, en + existing locales fall back)

### Phase 3 — Island overlay
- [x] `IslandCollapsedContent`: minimum 11sp, consistent glyph sizes, status never color-only
- [x] `IslandExpandedContent` container: shared padding, radius, spacing tokens
- [x] Expanded cards: Notification, Music, IncomingCall, Battery, Timer, Stopwatch, Navigation, LiveActivity, DownloadUpload, Hotspot, Bluetooth, Flashlight, ScreenRecording — 44dp buttons, type scale, tinted symbol buttons
- [x] Reduce-motion respected for visualizers / pulsing
- [x] Touch pass-through, inline-reply focus, pager snapping behavior unchanged

### Phase 4 — Verify
- [x] `./gradlew lintDebug testDebugUnitTest assembleDebug` clean
- [x] Install on phone; walked Home, Layout, Settings tabs and Notification/Timer island modes (other detail screens and modes not individually checked)
- [ ] Light mode, largest font scale, landscape check (only dark mode viewed on device)
- [x] CHANGELOG entry (user-visible change)

## Out of scope / constraints
- No behavior, permission, networking or data-model changes; DataStore keys untouched.
- Version code/name and signing untouched.
- Android has no SF Pro/SF Symbols; system font and Material icons are used, sized and weighted to match.

---

# Follow-up: Music popup (Now Playing card)

Reference: the system media popup in the user's screenshot — album art full-bleed, app header with an "open" chip, title/artist, wavy slider with times, heart / previous / pause / next / close.

## Root causes found in the code
| # | Symptom | Cause |
|---|---|---|
| 1 | Tapping a button also opens the app | The pill's root gesture handler in `IslandOverlayView` never checks `change.isConsumed`, so a child's tap still reaches its "tap expanded card → open app" branch; the music page also has a card-wide `clickable { onOpenNotification }` |
| 2 | Dragging the slider moves the card / can trigger swipe actions | Same handler accumulates drags the slider already consumed |
| 3 | Popup disappears after 5s of use | Fixed 5s auto-collapse even for music (timer is only reset on button taps) |
| 4 | Art is dimmed to 45% behind a heavy gradient and the card is cramped (115–180dp clamp) | `MusicExpanded` layout + `clampHeightForMode` |
| 5 | No way to end the song; loop button instead | No stop control |
| 6 | Slider hit area 24dp | `WavyMusicSeekBar` canvas height |

## Design (HIG: Now Playing + the always-dark island palette)
- Album art full-bleed behind a bottom-weighted scrim so white text stays ≥ 4.5:1; fallback tinted gradient when no art.
- Header row (≥ 44dp, the only area that opens the app): app icon + app name, trailing "Open app" chip.
- Title 22sp bold, artist 15sp secondary; wavy slider with 44dp touch height and elapsed / total times.
- Controls: like, previous, play/pause (largest, 64dp), next, close (stops playback and dismisses). All ≥ 44dp, labelled for TalkBack.
- Taps on the card body do nothing except keep it open; tapping outside collapses it.
- Music card stays up 10s after the last interaction instead of 5s.

## Checklist
- [x] Gesture handler honors consumed events (no app launch from child taps, no card drag from slider)
- [x] Remove card-wide open-app click for Music; header opens app
- [x] `MusicExpanded` rebuilt: art background, header, title block, slider + times, 5 controls (loop removed, stop added)
- [x] Slider 44dp touch height
- [x] Height clamp / estimate raised for music
- [x] Music auto-collapse 10s, reset on every interaction
- [x] Strings (en, pt, zh) for new labels
- [x] lint + 144 unit tests green (no new unit test: the consumed-gesture rule lives inside a Compose pointer handler)
- [ ] Release build installed and checked on the phone (play music, tap each control)

## Music popup round 2 (compact, matte, reliable buttons)
- Compact: title 17sp, artist 13sp, 36dp header, 32dp slider touch height, 44–46dp buttons with 22–28dp icons; card ≈ 175dp (was ≈ 230dp).
- Matte background: artwork blurred (28dp, API 31+), desaturated to 80%, scrimmed; stronger scrim on API < 31 where blur is unavailable.
- Button reliability root cause: the pill's root gesture handler consumed every pointer move over 0.5px. Compose cancels a child's tap in the Final pass if any ancestor consumed the pointer, so a real finger's natural drift cancelled button taps (adb taps have no drift, which hid it). The handler now holds back until movement exceeds touch slop. Verified on device with drifting taps: 4/4 pause/play toggles plus next, previous and seek.

## Music popup round 3 (Apple motion, real like)
- **Like root cause:** the tap called `setRating(heart)` *and* the player's "Add to favorites" custom action. Musicolet toggles on both, so the song flipped twice and ended unchanged while the island showed a locally faked heart. Now exactly one mechanism is used (custom action if the player offers one, else a heart rating), and the heart reflects the player's own label ("Add to…" / "Remove from…"). If the player doesn't confirm in 1.8s the heart reverts instead of lying. Logic lives in `util/MediaLike.kt` with unit tests.
- **Motion (Apple spring values; stiffness = (2π/response)²):** open = spring response 0.42s / damping 0.86; close = response 0.36s / damping 1.0 (critically damped, no bounce). Content is staggered: opening content fades in after a 70ms delay (240ms, ease 0.25/0.1/0.25/1); closing content fades out first (110ms) so the shell never shrinks around visible content. Reduce Motion (system animations off) snaps geometry and shortens fades to 120ms.
