# Companion for Smart Island: implementation plan

Port of Vorssaint's "companion" (code name: mascot) to Kotlin + Jetpack Compose.
Reference: `vorssaint-utils/Sources/Vorssaint/Services/Notch/NotchMascotSupport.swift` (model + motion math),
`UI/Notch/NotchMascotView.swift` (Core Animation rig), `Services/Notch/NotchService.swift` (scheduling, reactions).

## 0. Licence and attribution

Smart Island is GPL-3.0 and Vorssaint is GPL-3.0-or-later, so a port is licence-compatible.
Do these when porting:
- Keep a notice in every ported file: `Ported from Vorssaint (c) 2026 Vorssaint, GPL-3.0-or-later, https://github.com/vorssaint/vorssaint-utils`.
- Add an acknowledgement in `AboutSection.kt` and `README.md`.
- Do not use the Vorssaint name or logo (`TRADEMARKS.md`). Name the feature "Companion" and keep your own icon.

## 1. What the existing app gives us (and constrains)

| Fact in your code | Consequence for the companion |
|---|---|
| One overlay `ComposeView` in an accessibility-overlay window, height `(settings.height + 16)dp`, width `MATCH_PARENT` (fallback: group width + 32dp). `SmartIslandOverlayService.collapsedParams` | The companion can only draw inside the pill's 16dp margin band and the window width. Hops must stay under the window top (`track.hop` clamps this in the Swift original). |
| Touch region is restricted to the pill group + 16dp padding via reflection on `OnComputeInternalInsets` (`setupTouchableRegion`) | The companion must never need touches outside the pill. Pat-on-head only works when the pointer is over the pill. The companion is a pure drawing layer with no input of its own. |
| The pill is clipped: `.clip(mainShape).background(...)` in `IslandOverlayView` | Anything drawn inside the pill is clipped to it. The companion walks inside the pill, not outside it. |
| `IslandCollapsedContent` has a left slot and a right slot around the camera hole, driven by `IslandMode` (`Empty` shows nothing) | `Empty` is exactly Vorssaint's "idleContent == .none". That is when the companion rests. |
| `hideWhenIdle` shrinks the pill to 0 width when `notifications.isEmpty()` | Conflicts with a resting companion. Rule: companion enabled and idle overrides `hideWhenIdle` (see 6.3). |
| Notch mode on: no split companion circle. Notch mode off: 2+ notifications split into main pill + circle | Name clash: the code already says "companion" for the secondary circle (`hasCompanion`, `companionSwap`). Use `Mascot*` / `Buddy*` names in code, "Companion" only in UI strings. |
| Springs are in `IslandOverlayView` (`dpSpec`, `floatSpec`), and `rememberReduceMotion()` already exists in `Motion.kt` | Reuse `rememberReduceMotion()`. Use the same spring family for companion hops. |
| Events: `SystemEventReceiver` (power connected/low/OK, Bluetooth, battery saver), torch callback in the service, `INotificationRepository.postNotification` for everything else | Reactions hook in at these three places. Nothing new to register. |
| Settings: `SmartIslandSettings` data class + DataStore keys + `setX` suspend setters + JSON backup (`toJson`/restore) + `SmartIslandSettingsTest` | Companion preferences follow the same four-step pattern (see 5). |
| `minSdk 26`, Compose BOM 2026.02.01, coroutines-test already in test deps | Use plain Compose, no extra libraries. Pure-Kotlin model is unit-testable on the JVM. |

## 2. Package layout

```
app/src/main/java/com/agupta07505/smartisland/
  companion/                       (name the package "companion" or "buddy"; classes use Mascot prefix)
    model/
      MascotLook.kt               style, shape, palette, colour
      MascotMood.kt               moods + MascotExpression + MascotEye
      MascotReaction.kt           reactions, MascotMoment, ReactionGate
      MascotVisit.kt              Visit.Kind, MascotTrack, MascotPath
    geometry/
      MascotGeometry.kt           body/robot/eye outlines as point lists, resample, smooth path
      MascotFaceLayout.kt         eye positions per body
    motion/
      MascotMotion.kt             path(for kind, track), step sampler
    runtime/
      MascotController.kt         state holder, visit scheduler, reaction queue
      MascotEvents.kt             event bus (SharedFlow) other code posts to
      MascotEventSources.kt       small mappers: notification/system event -> reaction
    ui/
      MascotCanvas.kt             the drawing composable
      MascotLayer.kt              track-aware layer used inside the pill
      MascotSettingsSection.kt    settings UI + live preview
  test/.../companion/             unit tests mirroring each model/motion file
```

## 3. Model: direct port (pure Kotlin, no Android imports)

Port these from `NotchMascotSupport.swift` one-to-one. Keep the same names and values.

```kotlin
enum class MascotStyle { Minimal, Robot }
enum class MascotShape { Ball, Egg, Squircle, Pill }
enum class MascotPalette(val light: Long, val shade: Long) { Pearl(...), Lemon(...), Peach(...), Rose(...), Lilac(...), Sky(...), Mint(...) }
data class MascotLook(val style: MascotStyle, val shape: MascotShape, val palette: MascotPalette) {
    companion object { val Standard = MascotLook(MascotStyle.Minimal, MascotShape.Ball, MascotPalette.Pearl) }
}
enum class MascotMood { Idle, Happy, Wink, Love, Sleepy, Determined, Surprised, Searching, Thinking, Confused, Alert }
enum class MascotEye { Open, Closed, Happy, Heart, Sleepy, Wide, Squint, Determined }   // blinks = Open|Wide|Squint
data class MascotExpression(val left: MascotEye, val right: MascotEye, val gazeX: Float = 0f, val gazeY: Float = 0f, val tilt: Float = 0f)
enum class MascotReaction(val lengthSec: Float) { Celebrate(1.0f), Love(1.4f), Surprised(0.9f), Flash(0.5f), Confused(1.2f),
    WakeUp(1.6f), Yawn(1.2f), Perk(1.0f), Bounce(1.0f), Hush(1.0f), Ready(1.0f), Groove(1.6f) }
enum class MascotSide { Left, Right }
enum class MascotVisitFrequency(val delaySec: ClosedFloatingPointRange<Float>) { Rare(600f..1200f), Normal(240f..540f), Frequent(90f..180f) }
```

- Colour values: copy the RGB triples from `NotchMascotPalette` (light / shade).
- `MascotMood.expression`: copy the `when` table from `extension NotchMascotMood`.
- `MascotFaceLayout`: copy the per-shape/robot layout numbers from `extension NotchMascotLook.face`.
- `MascotReactionGate`: copy as-is (spacing 2.5s, repeat 30s, patience 8s). Take `nowMs` as a parameter so tests are deterministic.
- `MascotVisit.Kind` becomes a sealed interface: `Lap`, `Pass`, `Home`, `Cameo(reaction)`, `Countdown(totalSec)`, `Retreat`, `Farewell`, `Cross`, `Arrive`, `Linger(reaction)`. Keep `endsOutOfSight`, `takesOnlyItsWing`, `reaction`, `watchesTimer`.
- Drop what has no Android meaning: `CommandBarPresentation`, `NotchCommandBarStyle`, `readingGaze` (Command Bar only). Revisit later if you add a Command Bar.

## 4. Geometry and rendering

### 4.1 Outlines as point lists, not CGPath
Compose `Path` is write-only and cannot be morphed. Keep geometry as `FloatArray` / `List<Offset>` and build the `Path` at draw time.

- `MascotGeometry.body(shape, size)`: ball = ellipse; egg = 4 cubic curves with the control points in the Swift file; squircle = 72-point superellipse (`exponent = 2/3.4`); pill = rounded rect. Return `Path` directly for bodies (bodies do not morph, they cross-fade on look change).
- `MascotGeometry.robot(size)`: head rounded rect (0.12, 0.26, 0.76, 0.68, r 0.20), ears, antenna, bulb, visor. Same numbers.
- `eyePoints(eye, size, right): List<Offset>`: port `stadium`, `ellipse`, `arch`, `heart`, `lid`, `clipBelow`, `resample`. Always return exactly `EYE_CURVE_COUNT = 24` points, clockwise from top of the middle line. This fixed count is what makes any eye morph into any other.
- `smoothClosedPath(points)`: Catmull-Rom to cubic Bezier as in Swift (control points `current + (next - prev)/6` and `next - (after - current)/6`). Build with `Path.cubicTo`.
- `figureOffset(look)`: port, used to centre the figure vertically.

### 4.2 Eye morph and blink
- State: `eyeFrom`, `eyeTo`, `eyeProgress` (0..1 `Animatable`). Drawn points = `lerp(from[i], to[i], progress)`.
- Mood change: set `eyeFrom = currentDrawn`, `eyeTo = target`, animate progress with `tween(160, FastOutSlowInEasing)`.
- Blink: a blink is a transition `Open -> Closed -> Open` over about 120ms. Run it in a `LaunchedEffect` loop: `delay(random 2.5..6s)` (use the blink interval from `NotchMascotSupport.blinkInterval`, longer when battery saver is on), skip while a visit or reaction plays (the Swift rig uses `blinksResume`). Only moods where `expression.blinks` is true blink.
- Sleepy drift: after `blinksBeforeSleep(hour)` blinks with no event, ease into `Sleepy`; any event wakes it (`dozed` flag in the Swift rig).

### 4.3 Drawing
`MascotCanvas(look, pose, modifier)` is a single `Canvas`. `pose` is a stable holder of `State` reads so it only invalidates the draw phase.

```kotlin
class MascotPose {
    val x = Animatable(0f)          // centre x inside the track, px
    val lift = Animatable(0f)       // hop height
    val squash = Animatable(0f)     // + flat, - tall
    val tilt = Animatable(0f)
    val gazeX = Animatable(0f); val gazeY = Animatable(0f)
    val eyeProgress = Animatable(1f)
}
```
Read these only inside `drawWithCache {}` / `Canvas {}` / `graphicsLayer {}` lambdas, never in composition, so a frame costs a draw, not a recomposition.

Draw order (matches the layer stack in `NotchMascotRig.init`): ears, antenna, bulb (robot), body with vertical gradient `light -> shade`, visor (robot), shine ellipse (`rotate` + `drawOval`), then eyes on top in ink / visor-lit colour. Apply `translate(x, -lift)`, `rotate(tilt)` and `scale(1+squash, 1-squash)` about the figure's base.

### 4.4 Battery rule
Stop all companion animation loops when the pill is collapsed-hidden, the window is invisible, or the screen is off. Gate with `LocalLifecycleOwner` plus the existing `view.visibility` handling in the service. Visits are scheduled once, minutes ahead, as in the Swift code (no polling ticker).

## 5. Motion: port `NotchMascotMotion` to a keyframe sampler

The Swift code samples paths densely so Core Animation plays them. In Compose, do one of:

**Recommended:** port `sample(steps, start, track, duration)` unchanged, producing `MascotPath(keyTimes, x, lift, squash, gaze, duration, greeting)`, then play it with a single driver:

```kotlin
suspend fun MascotPose.play(path: MascotPath) {
    val clock = Animatable(0f)
    coroutineScope {
        launch { clock.animateTo(1f, tween((path.duration * 1000).toInt(), easing = LinearEasing)) }
        snapshotFlow { clock.value }.takeWhile { it < 1f }.collect { t ->
            val i = path.indexFor(t)         // binary search on keyTimes
            x.snapTo(path.xAt(i, t)); lift.snapTo(...); squash.snapTo(...); gazeX.snapTo(...)
        }
    }
}
```
This keeps the exact choreography (`lap`, `pass`, `cameo`, `linger`, `countdown`, `retreat`, `farewell`, `cross`, `arrive`, `home`) and one animated value drives everything.

Port the `Step` enum (`Stay(sec, bounces)`, `Hop(to, sec, height)`, `Walk(to, sec)`, `Jump(to)`) and its sampler. Keep the durations: lap 3.6s, pass 3.4s, home/arrive 0.62s, cameo = 0.5 + hold + 0.32, farewell 0.78s, cross 1.24s.

`MascotTrack` for Android (replaces the Swift `track(stripWidth:stripHeight:wing:...)`):
- `width` / `height`: pill width/height in px.
- `size`: `height - 6dp` clamped, roughly 20-24dp. The Swift `size(stripHeight:floats:)` is the model.
- `hidden`: the camera span. Compute from `CameraCutoutDetector.detect(context)`: the cutout width centred in the pill (`xOffset` is already handled by the pill position). With no hardware cutout (`hasHardwareCutout == false`) set `hidden = null`; the Swift code has a "capsule" path for exactly this (walks in at the near end, out at the far end).
- `rest`: centre of the left wing = `(0 + hidden.lowerBound) / 2`, or the right wing if `side == Right` (`mirrored = true`, as in Swift).
- `ceiling = 0`, `baseline = height / 2`.

Respect `rememberReduceMotion()`: no strolls, no hops; reactions become a face change only (the Swift code does the same under Reduce Motion).

## 6. Integration with the existing island

### 6.1 State: `MascotController`
Singleton owned by `SmartIslandOverlayService` next to `IslandViewModel` (or created in `SmartIslandRepositories`). Exposes:

```kotlin
class MascotController(scope: CoroutineScope, settings: StateFlow<SmartIslandSettings>, mode: StateFlow<IslandMode>, expanded: StateFlow<Boolean>) {
    val visit: StateFlow<MascotVisit?>
    val reaction: StateFlow<MascotReactionEvent?>   // id + reaction + startMs
    val atRest: StateFlow<Boolean>                  // enabled && mode == Empty && !expanded && !tucked
    val restingMood: StateFlow<MascotMood>          // Alert while Keep Awake / torch on, else Idle
    fun post(moment: MascotMoment)                  // goes through ReactionGate
}
```
Responsibilities, all in `viewModelScope`/service scope coroutines:
- Visit scheduler: one `Job` that `delay`s `frequency.delaySec.random()`, then starts a `Lap` (if at rest) or `Pass` (otherwise), then waits for `visit.duration`, clears it, reschedules. Cancel/reschedule when settings change. Skip visits when Battery Saver is on (`PowerManager.isPowerSaveMode`), the screen is off, the pill is expanded, or the lock screen is showing.
- Reaction queue: reactions that can't show wait up to `ReactionGate.patience` (8s), else dropped.
- Cameo vs linger: if `atRest`, play the reaction in place (`Linger`). If another mode is showing, the companion comes out of the wing for it (`Cameo`) and hands the slot back, mirroring `handBack` in Swift. Start with only the `atRest` case; add cameo in phase 4.
- Welcome hello 1.5s after being switched on; farewell hop when switched off.

### 6.2 Event sources (all reactions go through `MascotController.post`)

| Moment -> reaction | Where to hook in your code |
|---|---|
| Charging started -> Love | `SystemEventReceiver` `ACTION_POWER_CONNECTED` branch |
| Battery low -> Yawn | `ACTION_BATTERY_LOW` branch |
| Timer finished -> Surprised, Timer started -> Ready | Where a `Timer` notification is posted / the countdown reaches 0 (`TimerCountdown` / `TimerStopwatchParser`). Better: detect in `IslandViewModel` by watching `visibleNotifications` for a `Timer` entry disappearing or `progress` hitting its end |
| Download/transfer finished -> Celebrate | `IslandMode.DownloadUpload` notification with `progress == progressMax` |
| Download failed -> Confused | notification text/category from `NotificationListenerService` for failure strings (start with "failed"/"error" match; make it a small parser with a test) |
| Music starts -> Groove (at most once per 10 min, `grooveInterval = 600`) | `mediaIsPlaying` false -> true transition on the Music notification |
| Flashlight on -> Perk, off -> Yawn | existing torch callback in the service (`onTorchModeChanged`) |
| Screen recording start -> Ready | `IslandMode.ScreenRecording` notification appearing |
| Unlock -> WakeUp | register `Intent.ACTION_USER_PRESENT` in the service (the service already tracks keyguard state) |
| AI agents / mic mute / event countdown / screenshot | Vorssaint-specific. Skip, or map to your own equivalents later |

Create `MascotEvents.kt` as a thin facade (`object MascotEvents { fun post(moment) }` backed by a `MutableSharedFlow`) so receivers and the service call one line without needing a controller reference. Unit-test each mapping with the existing receiver/notification test style (`SystemEventReceiverTest`, `NotificationPriorityTest`).

### 6.3 Layout inside the pill
Change `IslandOverlayView` / `IslandCollapsedContent` minimally:

1. **Resting width.** When the companion is enabled and `mode == Empty`, the pill must be wide enough to give the companion a wing. Add to the `width` transition target: `if (isHiding) 0 else settings.width + companionWingDp` where `companionWingDp` = about `size + 12dp` (Swift: `wingRoom = 10`). Keep `animatedXOffset` so the camera stays centred (add the wing on both sides, as the notch does, or shift the pill by half a wing and keep the camera under the cut-out; the symmetric option is simpler and matches an actual hardware cutout).
2. **Idle hiding.** Define `val isIdleHiding = settings.hideWhenIdle && notifications.isEmpty() && !settings.companionEnabled`. If both are on, the companion's own "hide in the island when idle" option (default on in Vorssaint, `hideDelay = 30s`) governs: after 30s idle it tucks behind the camera and the pill returns to its normal width. Port `mascotTucked`/`hideDelay`.
3. **Draw layer.** In the pill `Box`, inside the clipped region and above `IslandCollapsedContent`, add:
   ```kotlin
   if (settings.companionEnabled && !expanded) {
       MascotLayer(controller = mascot, look = settings.companionLook, modifier = Modifier.fillMaxSize())
   }
   ```
   `MascotLayer` builds the `MascotTrack` from the pill's measured size and the cutout, keeps a `MascotPose`, and calls `pose.play(path(for: visit.kind, track))` in a `LaunchedEffect(visit?.id)`.
4. **Step aside.** During a visit over an active mode, the left/right slot content should fade out where the companion passes (`takesOnlyItsWing` rules). In `IslandCollapsedContent`, multiply each slot's alpha by `1 - stepAside` where `stepAside` comes from the controller (start by hiding only the wing the companion is in, as in `NotchView.wingStepsAside`).
5. **Touch region.** No change: the companion lives inside the pill, which is already the touchable region. If you widen the pill for the wing, update both `setupTouchableRegion` and `collapsedParams` so they use the same effective width (add a shared `effectivePillWidthDp(settings, notifications, companionActive)` helper and use it in all three places instead of `settings.width`; this is the main refactor risk, as the width math is duplicated today).
6. **Notch mode / split mode.** Rest only when `notifications.isEmpty()`. When `hasCompanion` (split secondary circle) appears, the buddy treats it like any other active mode: it hides/tucks.
7. **Expanded state.** Hidden, as in Vorssaint. Optionally show a small buddy in the expanded empty state (the "135dp" shortcuts card) in a later phase.

### 6.4 Pointer interaction (optional, phase 5)
Vorssaint's eyes follow the mouse. On Android use the pill's existing gesture detector: on `awaitFirstDown` over the pill, set gaze toward the touch point for 600ms; a long press that already triggers haptics (`HOLD_GESTURE_THRESHOLD_MS`) can also trigger `Love`. Do not add a second `pointerInput` on the companion; reuse the pill's one.

## 7. Settings and persistence

Follow the four-step pattern already used in `SmartIslandSettings`:

1. **`SmartIslandSettings`**: add fields + defaults + JSON backup keys (`toJson` and restore, with clamping/validation like the others):
   - `companionEnabled: Boolean = false`
   - `companionStyle: String = "minimal"`, `companionShape: String = "ball"`, `companionPalette: String = "pearl"`
   - `companionSide: String = "left"`
   - `companionVisitFrequency: String = "normal"`
   - `companionVisits: Boolean = true`, `companionReacts: Boolean = true`
   - `companionHidesWhenIdle: Boolean = true`
2. **`SmartIslandSettingsRepository`**: add `Keys.*`, map in the settings flow, add `setCompanionEnabled(...)` etc. using `editSafely`.
3. **`SmartIslandSettingsTest`**: round-trip + invalid-enum-falls-back-to-default tests.
4. **UI**: `MascotSettingsSection` in `ui/sections/`, added to `SmartIslandHomeScreen` next to `CustomizationsSection`. Contents: enable switch, style/shape/palette pickers (colour swatches row), side, visit frequency, "react to events", "hide when idle", and a live preview.
   - Preview = a `MascotCanvas` in a scaled pill mock using `MascotTrack.scaled(by: 2.5f)`. Buttons that play each `MascotMoment` (visit, charging, download done, ...) call the same `controller.post` path.
5. Strings go through `strings.xml`. If you localise, add every locale, as the Swift project requires.

## 8. Testing

Pure-JVM unit tests (no Robolectric), in `app/src/test/.../companion/`:
- `MascotReactionGateTest`: spacing, repeat interval, patience.
- `MascotGeometryTest`: every `MascotEye` returns exactly 24 points; resample is closed; bounds sane for each shape; `figureOffset` finite.
- `MascotMotionTest`: for every `Kind` on a hidden-camera track and on a capsule track: `path.x.first/last` positions, `keyTimes` monotonic from 0..1, `lift >= 0`, never above `track.ceiling`, mirrored path equals the mirror of the left one, duration matches the table.
- `MascotControllerTest` (with `kotlinx-coroutines-test`, `TestScope` virtual time): visit scheduling honours frequency, is cancelled on disable, skipped under battery saver / expanded; reaction queue patience; welcome hello; farewell.
- `MascotEventMappingTest`: power/battery/timer/download/music mappings.
- `SmartIslandSettingsTest`: new fields.

Instrumented (`IslandOverlaySmokeTest`): add a smoke test that the pill still renders with the companion on and that `Empty` mode shows the wing.

Manual checklist: punch-hole centre, top-centre waterdrop, no cutout, left-corner camera (`xOffset` large), notch mode on/off, landscape, lock screen (`showOnLockScreen`), Reduce Motion (Settings animator scale 0), Battery Saver, 120 Hz device, Samsung/Xiaomi (the project already has `OemDeviceRules`).

## 9. Phases (each one shippable)

1. **Static companion** (1-2 days): model, geometry, `MascotCanvas`, settings keys, preview in settings, resting companion in the pill on `Empty`, pill widening, shared width helper. No motion except blink.
2. **Moods and reactions**: eye morph, reaction gate, charging/battery/download/music/flashlight/unlock hooks, Linger-style reactions at rest.
3. **Visits**: `MascotMotion` sampler, `Lap`/`Pass`, visit scheduler, `Arrive`/`Farewell` on enable/disable, hide-when-idle tuck.
4. **Cameos over activities**: `Cameo`, `Countdown`/`Retreat` for timers, slot step-aside, `Cross` when changing side.
5. **Polish**: gaze following touch, pat on head, sleepy drift, low-power behaviours, Reduce Motion pass, device matrix, backup/restore, About credits.

## 10. Risks and decisions for you

- **Width duplicated in three places** (`IslandOverlayView`, `setupTouchableRegion`, `collapsedParams`). Introduce one helper before adding the wing, otherwise touch region and window size will drift.
- **Pill is clipped to its shape.** The companion cannot leave the pill. That is fine for a camera-centred pill, and replaces the Swift "wing" with your left/right slot area.
- **No-cutout devices.** Use the capsule path (no camera hiding), already modelled in the Swift track via `hidden == nil`.
- **Hidden API risk** in the touch-region reflection is unchanged by this feature, but a wider resting pill increases the area that intercepts touches. Keep wing width small (about 24-30dp).
- **Scope control:** Command Bar droplet, AI-agent reactions, mic-mute, screenshot and calendar-countdown reactions are Vorssaint-only. Leave them out of v1.
- Decide: pill widens symmetrically (simple, recommended) vs. only on the companion's side (needs `xOffset` compensation).
