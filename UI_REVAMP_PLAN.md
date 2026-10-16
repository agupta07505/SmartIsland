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
