# Island motion: spring presets and the kinetic glow

How the island's size animates, and the glow that reacts to how fast it moves. Code: `ui/IslandMotion.kt`
(presets and glow maths, pure), `ui/IslandOverlayView.kt` (applies them), `ui/sections/MotionSection.kt` (the
picker on the Layout tab). Tests: `IslandMotionTest`.

## Spring presets

The island's width, height, corner radius and offset animate with a spring-mass-damper of unit mass:
`force = -k * (x - target) - d * v`, with stiffness `k` and damping `d`. Compose's `spring()` takes the same
stiffness and a damping *ratio*, `d / (2 * sqrt(k))`, so a preset's `d` converts exactly. A unit test integrates
the reference formula and checks Compose traces the same path for every preset.

| Preset | Stiffness k | Damping d | Ratio | Feel |
|---|---|---|---|---|
| Bouncy | 150 | 12 | 0.49 | highly elastic, overshoots |
| Snappy | 300 | 22 | 0.64 | fast and aggressive |
| Critical (smooth) | 180 | 27.5 | 1.02 | no overshoot |
| Loose | 60 | 5 | 0.32 | slow, floating, overshoots a lot |
| Standard | n/a | n/a | n/a | the original tuning: a light spring to open (ratio 0.86, k 224), critically damped to close (k 305) |

- Standard is the default and keeps the island looking as it always has. A physical preset uses one spring,
  the same going out and coming back.
- The choice is stored as `springPreset` (a preset id) and is part of settings backup. An unknown id falls back
  to Standard.
- With system animations off (Reduce Motion) the island snaps and the presets have no effect.

## Kinetic glow

A faint light-blue glow that flares while the island moves and fades as it settles.

- **Speed** is read off the animated size on Compose's frame clock, once per frame, in dp per second: the larger
  of the width speed and the height speed.
- **Threshold:** nothing is drawn at or below 5 dp/s.
- **Blur radius** (dp) = `min(20, speed * 0.08)`, so it reaches its 20 dp cap at 250 dp/s. Most expansions are
  far faster than that, so the glow is usually at full strength for the middle of the move and falls away as
  the spring settles.
- It is drawn as contiguous bands outside the island's rounded rectangle, behind it, so it looks the same on
  every Android version. Colour `#3186FF`.
- **Cost:** while the island is still the sampling coroutine is suspended; it runs per frame only during a move
  and stops after the size has held still for 6 frames.
- Switch it off with "Kinetic glow" in Layout > Motion (`kineticGlow` setting). It is also off under Reduce
  Motion.
- **Limitation:** the collapsed overlay window is only a little larger than the pill, so on a collapsed island
  the halo is clipped to that window. It shows fully whenever the window is full-screen (while expanded or
  moving to or from it).

## Debugging

Debug builds log `IslandMotion: preset=... peakSpeed=... glow=...` as the island moves, and
`adb shell settings put global debug_island_glow_dp 18` holds the glow at a fixed radius (restart the island
service or change preset to apply; `settings delete global debug_island_glow_dp` clears it). Neither exists in
release builds.
