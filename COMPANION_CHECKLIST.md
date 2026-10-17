# Companion (Buddy): execution checklist

Companion to `COMPANION_PLAN.md`. Verified on 2026-10-17 against this repo at `f6bba9f` (branch `UI_upgrade`) and the Vorssaint reference at `~/Desktop/vorssaint-utils`.

## 0. What "read and understand" found

**The plan is sound.** Every name and number I spot-checked matches the Swift source: `eyeCurveCount = 24`, gate `2.5 / 30 / 8`, motion durations `3.6 / 3.4 / 0.62 / 0.5 / 0.32 / 0.78 / 1.24`, `welcomeDelay 1.5`, `grooveInterval 600`, `hideDelay 30`, `wingRoom 10`. Vorssaint files carry `SPDX-License-Identifier: GPL-3.0-or-later`; Smart Island is GPL-3.0, so the port is compatible. `TRADEMARKS.md` bars the Vorssaint name, logo and icon, which the plan already respects.

**`feature_map.md` and `ui_map.md` do not exist.** They are gitignored and absent from this checkout and from disk. Their generators in `~/dev/codemap-gen` are hard-wired to `~/Desktop/SmartIsland` on branch `audit/hardening`, a checkout that is gone, so running them would produce maps of old code. I mapped the plan against the live code instead (section 2). Regenerating the maps for this repo is optional and not on the critical path.

## 1. Corrections to `COMPANION_PLAN.md` (each verified in code)

| # | Plan says | What the code shows | Change |
|---|---|---|---|
| C1 | Phase 2 plays reactions "at rest" for charging, flashlight, recording, timer, download, music | Each of those events *replaces* `Empty` with its own island mode (Battery, Flashlight, ...). The buddy only rests when mode is `Empty`, so an at-rest reaction would wait out the 8s patience and be dropped. Only unlock (WakeUp) fires while idle | Phase 2 ships infrastructure, preview, unlock and the sleepy drift. Event hooks move to the **Cameo** phase |
| C2 | Widen the pill by a wing whenever the buddy is on | The default pill is 112dp wide; a punch hole is about 24-30dp (`CameraCutoutDetector` adds 24dp padding to the raw width). Each side already has roughly 40dp free | Widen **only by the shortfall**: `max(0, needed - freeWing)`. Most devices need no widening, so no touch-region or window change |
| C3 | "Width is duplicated in three places" | Also duplicated: `isIdleHidden` (`IslandOverlayView.kt:233`, `SmartIslandOverlayService.kt:536`, `:639`). Width appears at `IslandOverlayView.kt:193-215, 259, 437, 845` and `SmartIslandOverlayService.kt:649, 749` | One helper for **width** and one for **idle-hidden**, both used by all call sites |
| C4 | Camera span from `CameraCutoutDetector.detect` | The detector returns a padded width (+24dp) for pill sizing, not the true hole | Add the **raw cutout rect** to the detector (with tests); the buddy must pass behind the real hole |
| C5 | UI name "Companion" | `strings.xml:596-603` already ships "Companion Circle Position" for the split-mode circle (also README and code: `hasCompanion`). Two features with one name | UI name **Buddy**; package `buddy`, classes `Mascot*`. Existing "companion circle" strings stay untouched |
| C6 | Separate `companionVisits` toggle plus frequency | Extra setting for no extra choice | Merge into one `Visits` choice: Off / Rare / Normal / Frequent (HIG `settings.md`: "Minimize the number of settings you offer") |
| C7 | Battery rule "stop loops when hidden" | A blink loop on an always-on overlay still wakes the renderer | Add a measured gate: idle frames between blinks must be zero (`dumpsys gfxinfo`, section 5 Phase 1) |
| C8 | `Linger` is the only rest reaction in phase 2 | Without cameos the buddy has nothing to react to while idle besides unlock | Covered by C1 |

## 2. Map: plan to feature to UI

Because the maps are absent, this table is the map. Paths are under `app/src/main/java/com/agupta07505/smartisland/`.

| Plan area | Feature (where logic lives) | UI (where it shows) | Anchors verified |
|---|---|---|---|
| Model, geometry, motion (`buddy/model, geometry, motion`) | New pure-Kotlin package, no Android imports | none | Swift: `NotchMascotSupport.swift` |
| Drawing (`MascotCanvas`, `MascotLayer`) | New | Collapsed pill: new layer inside the clipped pill `Box`, above `IslandCollapsedContent` | `ui/IslandOverlayView.kt` pill `Box` (`.clip(mainShape).background`); left slot `ui/IslandCollapsedContent.kt:105-125` (`Empty -> Unit` at `:222`, `:314`) |
| Resting state (`mode == Empty`) | `ui/IslandViewModel.kt` (`mode`, `expanded`) | Same pill | `IslandOverlayView.kt:179` (`activeMode`) |
| Pill width and idle hiding | `service/SmartIslandOverlayService.kt` (`collapsedParams`, window update, touch region) | Window and touch region | width `:649, :749`; idle `:536, :639`; overlay `IslandOverlayView.kt:193-215, 233, 259` |
| Event hooks | `service/SystemEventReceiver.kt` (`ACTION_POWER_CONNECTED :141`, `ACTION_BATTERY_LOW :157`); overlay service torch `onTorchModeChanged :82`, `ACTION_USER_PRESENT :137, :267`; `data/SmartIslandNotificationRepository.kt` (`postNotification :39`, `removeNotification :61`) for timer, download, music, recording | none (drives the buddy) | |
| Settings model | `data/SmartIslandSettings.kt` (+ JSON backup), `data/SmartIslandSettingsRepository.kt` (keys, flow, setters) | none | test: `SmartIslandSettingsTest` |
| Settings UI | new `ui/sections/BuddySection.kt`; row added to `SmartIslandHomeScreen.kt` `SettingsOverviewSection` (Appearance group) and `FeatureDetailSection` enum (`:174`) plus `DetailScreenHost` title map | Settings tab > Appearance > **Buddy** | uses `ui/components/GroupedList.kt`, `Chrome.kt` |
| Credits | `ui/sections/AboutSection.kt`, `README.md` | About screen | |
| Strings | `res/values{,-en,-pt,-pt-rBR,-zh,-zh-rCN}/strings.xml` | all | all six files stay in parity |
| Reduce Motion | `ui/Motion.kt` `rememberReduceMotion()` | all buddy motion | already shared |

## 3. Apple HIG decisions for everything the plan leaves unspecified

The skill is applied as principles, as before (Android). Sources are the HIG pages under `~/.claude/skills/apple-design/references/hig/`.

| Unspecified item | Decision | Source |
|---|---|---|
| Where the setting lives | One row, **Buddy**, in Settings > Appearance with the state as its value ("Off" or the look name). Not a new tab | `settings.md › General settings`, `layout.md › Visual hierarchy` |
| How many controls | Enable, Style, Shape, Color, Side, Visits, React to Events, Hide When Idle (8). Visits merged (C6) | `settings.md › Best practices` |
| Style and Side pickers | Segmented controls, text-only, equal-width: Style (2), Side (2), Visits (4), Shape (4). All at most 5 segments | `segmented-controls.md › Best practices, Content` |
| Color choice | Row of 7 swatches, each at least 44dp, selected = ring **plus** checkmark, current color name shown beside it and read by TalkBack. Never color alone | `color.md › Inclusive color`, `accessibility.md` |
| On/off settings | Switch rows inside the grouped list only | `toggles.md › "Use the switch toggle style only in a list row"` |
| Live preview | Header card at the top of the screen: a 2.5x black pill with the buddy. Below, a grouped list "Try It" with rows: Visit, Charging, Download Finished, Music, Timer Done, Wake Up (each plays the real reaction path). Preview is decorative: one `contentDescription` ("Buddy preview, Ball, Pearl") | `layout.md`, `accessibility.md` |
| First enable | No dialog and no tutorial. The 1.5s welcome hello **is** the feedback. If a conflicting setting is on (Hide When Idle), a one-line footer explains it instead of an alert | `onboarding.md › "Teach through interactivity"`, alerts stay rare |
| Motion | Idle blink and rare visits only; nothing on frequent interactions; Reduce Motion = face change only, no strolls or hops; every reaction also shows as the existing island content, so motion is never the only signal | `motion.md › Make motion optional, avoid motion on frequent interactions` |
| Haptics | None by default. If the pat-on-head phase adds one, it is a single short haptic, off with a switch | `playing-haptics.md › Avoid overusing, Make haptics optional` |
| Contrast | Body light and shade colors must be at least 3:1 against the black pill (non-text graphics); eyes at least 4.5:1 against the body. Checked by a unit test over all palettes | `accessibility.md › color contrast` |
| Touch targets | Buddy has no input of its own (plan 1). Settings controls at least 44dp | `accessibility.md` |
| Text | Row labels in title style to match the app (for example "Hide When Idle"), body 17sp, footers 13sp from the existing type scale | `typography.md`, existing `Type.kt` |
| TalkBack | Buddy on the island is hidden from accessibility (`clearAndSetSemantics`), since it conveys nothing | `accessibility.md` |
| Dark and light | The island is always black; settings follow the existing light/dark scheme | `color.md › Best practices` |

## 4. Decisions with my default (change any before build)

1. UI name **Buddy**; package `buddy`; classes `Mascot*`.
2. Widen the pill **only by the shortfall**, symmetrically (C2).
3. Default **off**; first enable plays the welcome hello.
4. Visits merged into one 4-way choice (C6).
5. Cameo and event reactions land together in the Cameo phase (C1).
6. v1 omits Vorssaint-only items (Command Bar, AI agents, mic mute, screenshot, calendar countdown), as the plan says.

## 5. Step-by-step checklist

Every step ends green on `lintDebug testDebugUnitTest` (lint has `warningsAsErrors`). Each phase ends with a signed release build installed over the existing app and checked on the phone. One commit per phase.

### Phase 0: Prep
- [x] 0.1 Confirm the decisions in section 4 (approved with all six defaults)
- [x] 0.2 Create the `buddy` package and the attribution header text: `buddy/MascotAttribution.kt` (NOTICE, SOURCE_URL) + `MascotAttributionTest`. Sub-packages (model, geometry, motion, runtime, ui) are created with their first real file in Phase 1, since an empty Kotlin package cannot exist
- [x] 0.3 Credit added: About > Acknowledgements row (opens the upstream repo, strings in all six locale files) and a README Acknowledgements section; no Vorssaint name or logo used elsewhere

### Phase 1: Static buddy at rest
- [x] 1.1 Model: `MascotStyle/Shape/Palette/Look/Mood/Eye/Expression/Reaction/Side/VisitFrequency` (pure Kotlin) + `MascotFaceLayout`
- [x] 1.2 Test: palette contrast (light and shade vs black at least 3:1, eyes vs body at least 4.5:1), mood-to-expression table
- [x] 1.3 Geometry: bodies, robot, `eyePoints` (exactly 24 points), `resample`, `smoothClosedPath`, `figureOffset`
- [x] 1.4 Test: `MascotGeometryTest` (24 points per eye, closed outline, finite bounds per shape)
- [x] 1.5 `CameraCutoutDetector`: add the raw cutout rect; extend `CameraCutoutDetectorTest`
- [x] 1.6 Shared helpers: `effectivePillWidthDp(...)` and `isIdleHidden(...)`; replace all duplicated sites (C3); behavior-neutral, existing tests plus a new helper test
- [x] 1.7 Settings: fields, JSON backup keys with clamping and invalid-enum fallback, DataStore keys, setters; extend `SmartIslandSettingsTest`
- [x] 1.8 `MascotCanvas` + `MascotPose` (draw-phase reads only); `MascotLayer` placed in the pill, shown only when enabled, `Empty` and collapsed; wing widening by shortfall (C2)
- [x] 1.9 Blink loop with `rememberReduceMotion()` and lifecycle/visibility gating (C7)
- [x] 1.10 `BuddySection`, Settings > Appearance row, `FeatureDetailSection.Buddy`, strings in all six locales. Phase 1 shows only controls that already do something (Show Buddy, Style, Shape, Color, Side); Visits, React to Events and Auto-Hide controls are persisted but stay hidden until Phases 3-4 so no dead controls appear
- [x] 1.11 Verified on the phone (punch-hole, 112dp pill, dark mode): buddy rests beside the camera; steps aside for a notification and returns after Clear; the Buddy settings screen previews every look; idle rendering is sparse (0 frames in most seconds, 6-21 frames in blink seconds, via `dumpsys gfxinfo`). **Not yet checked:** light mode, landscape, lock screen, Battery Saver, no-cutout and left-corner cameras (Phase 5 matrix)

Phase 1 build notes: `PillLayout` (width + idle-hide, used by overlay, window and touch region), `MascotTrack`, `MascotBlink` (single and double blink timings from the reference), `MascotCanvas/Pose/Layer`. Rest position follows the reference: beside the camera with a 4-8dp gap; a pill with no camera rests the buddy in the middle. 199 unit tests green, lint clean. 1.11 device check done on the phone (see its note).

### Phase 2: Moods, reaction gate, unlock
- [x] 2.1 `MascotReactionGate` (time injected) + test
- [x] 2.2 `MascotEvents` facade (`SharedFlow`) and `MascotController` (state only: reaction queue, rest state, resting mood)
- [x] 2.3 Eye morph (`from -> to` over 160ms) and expression changes; `Linger` reactions at rest
- [x] 2.4 Unlock to WakeUp, plus sleepy drift. This phone's ROM never delivers `ACTION_USER_PRESENT` to the app (SCREEN_ON/OFF arrive), so the service also watches the keyguard after SCREEN_ON (250ms check, only while locked, 4 min cap) and announces the unlock once (3s dedupe)
- [x] 2.5 Preview buttons route through `controller.post`
- [x] 2.6 Tests: `MascotControllerTest` (virtual time: queue patience, gate spacing); event mapping test
- [x] 2.7 Verified on the phone: "Try It > Charging" shows heart eyes on the preview; unlocking plays WakeUp on the real pill (happy-eyes frame captured). All 12 reactions are ported; only Love, WakeUp and blink were watched on the device

Phase 2 build notes: `MascotReactionGate`, `MascotMoment`, `MascotController` (virtual-time tested), `MascotEvents`, `MascotRuntime`, `MascotPose` face morph (220ms), `MascotReactions` (all 12), sleepy drift, Buddy settings gain "React to Events" and a "Try It" list. 215 unit tests green, lint clean.

### Phase 3: Visits
- [x] 3.1 Port `Step` sampler and `MascotMotion` paths (lap, pass, home, arrive, farewell, retreat, cross)
- [x] 3.2 `MascotTrack` from pill size and the raw cutout (capsule path when there is no hardware cutout)
- [x] 3.3 `pose.play(path)` driver, one animated clock
- [x] 3.4 Visit scheduler (frequency, skipped under Battery Saver, screen off, expanded, lock screen); welcome hello on enable, farewell on disable; hide-when-idle tuck after 30s with the idle-hide interaction from the plan
- [x] 3.5 Test: `MascotMotionTest` (every kind, hidden and capsule tracks, monotonic key times, mirror symmetry, durations)
- [x] 3.6 Verified on the phone: farewell (wink, hop, away behind the camera) then arrival on switching Buddy off and on; idle tuck about 30s after rest; the scheduled visit (Often) brought it back out about 150s later; a full Lap on the preview (out from behind the camera, hello, out the far end, back in at the near end, hop to rest). **Not yet checked:** Battery Saver skip, Reduce Motion, and the pill dropping its wing on tuck (this phone's 112dp pill needs no wing)

Phase 3 build notes (3.6 device check done): `MascotMotion` ports every path (lap, pass, home, arrive, farewell, cross, retreat, cameo, linger, countdown) with the reference sampler; `MascotPath` interpolates in the placement and draw phases (no recomposition while walking); the controller owns the visit, the tuck (30s idle: yawn then away behind the camera, the pill drops its wing) and the schedule (Off/Rare/Normal/Often, skipped under Battery Saver and Reduce Motion). Buddy settings gain Visits, Tuck Away When Idle and a Visit row in Try It; switching Buddy on plays an arrival and off plays a farewell. 241 unit tests green, lint clean.

### Phase 4: Cameos and event hooks (C1)
- [x] 4.1 `Cameo`, `Countdown`, `Retreat`, `Cross`; step-aside of slot content
- [x] 4.2 Hooks: charging (Love), battery low (Yawn), torch (Perk/Yawn), recording start (Ready), timer start/finish (Ready/Surprised), download done/failed (Celebrate/Confused, with a small failure-text parser and test), music start (Groove, at most once per 10 min)
- [x] 4.3 Tests for each mapping
- [x] 4.4 Verified on the phone: unlocking while a music pill is showing brings the buddy out over the pill's left wing (the album art steps aside), plays the wake-up, and the art returns; a charging event while the buddy was tucked brought it out (arrival) and it played the love reaction at rest. Charging was simulated with `dumpsys battery set ac 1`. **Not yet watched on the phone:** the charging/groove cameo (the music card stays expanded longer than a reaction waits, so those can expire), torch, recording, timer countdown/retreat, download events and the side-change crossing. These are covered by unit tests only

Phase 4 build notes: `MascotEventMapper` (pure; music/recording/timer/download-complete and failed) + `MascotDownloadText` (English, pt and zh keywords); controller cameo (`canCameo`), countdown/retreat, cross; `IslandCollapsedContent` steps the buddy's wing aside; the service posts torch, charging, low-battery, unlock, and watches notification changes and the last 5s of a timer; changing the Buddy side plays a crossing. 262 unit tests green, lint clean.

### Phase 5: Polish
- [x] 5.1 Gaze toward touch and pat (the pill's one gesture handler tells the controller where a finger landed; a held touch on the buddy gives hearts, at most once per 30s; a sleepy buddy wakes instead of looking; verified on the phone: long-press on the buddy shows heart eyes and does not expand the pill) (reuse the pill's one gesture handler; no second `pointerInput`; respects the slop and consumed-pointer rules already in place)
- [ ] 5.2 (partly done) Device matrix pass. Checked on this phone (punch hole at centre, 112dp pill, dark mode, 120 Hz, lock screen with the pill shown, unlocked home). **Still to check on other hardware or settings:** light mode, landscape, notch mode, no-cutout and waterdrop/left-corner cameras, Battery Saver, Reduce Motion (all covered by unit tests or code paths, not watched). Original line: Device matrix pass (punch hole, waterdrop, no cutout, left-corner camera, notch mode, landscape, lock screen, Battery Saver, 120 Hz)
- [x] 5.3 Backup and restore round-trip with new fields
- [x] 5.4 CHANGELOG entry, README feature row

Phase 5 build notes: `MascotGaze`, `MascotAttention`, `MascotTrack.touches`, controller `attend`/`pat`; CHANGELOG and README entries. 268 unit tests green, lint clean.

## 6. Risks to watch during build
- Window and touch region must use the same width helper (C3) or touches drift.
- Hidden-API touch-region reflection is unchanged; keep any widening small and only when needed.
- The pill is clipped, so the buddy cannot leave it.
- Reaction timing depends on the notification flow; test with virtual time, not wall clock.
