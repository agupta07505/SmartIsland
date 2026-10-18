# How the island decides what to show

This is the rulebook behind the island: which alert owns the pill, what waits behind it, what disappears
and when. The logic lives in one pure class, `alert/AlertEngine.kt`, and is covered by
`AlertEngineTest` (21 tests). If you change behaviour, change the engine and its tests first, then this file.

## 1. Where it sits

```
SmartIslandNotificationListenerService      SystemEventReceiver / demo
                  \                              /
                   SmartIslandNotificationRepository      (every active alert, unfiltered)
                                  |
                            IslandViewModel
        foreground/settings filter -> AlertEngine.evaluate(list, now)
                                  |
                    visibleNotifications  (at most 2 entries, in order)
                    /                                   \
   OverlayIsland -> IslandOverlayView            SmartIslandOverlayService
   (draws the pill, bubble, card)                (sizes and positions the window)
```

- The repository keeps everything; the engine only decides what the island *shows*. Nothing the engine hides
  is cancelled: it stays in the notification shade and can come back.
- **The window and the UI must read the same list.** `SmartIslandOverlayService` uses
  `viewModel.visibleNotifications` for the split state, the idle hide, the blur and the pill width. It once used
  the raw list, so after an alert expired the window still believed there were two states and the pill sat
  about 20dp off the camera centre. Never read `viewModel.notifications` for layout.
- `IslandViewModel` re-runs the engine whenever the alert list, the foreground app or the settings change, on a
  touch, and at the next expiry the engine reports (`scheduleAlertExpiry`).

## 2. Tiers

`AlertClassifier.classify` maps every alert to one tier. Highest first:

| Tier | What | Lifetime |
|---|---|---|
| **Critical** | alarms (`category == "alarm"`), a ringing timer, battery at **5% or less** and not charging | until the user acts |
| **Persistent** (Tier 1) | incoming call, navigation, screen recording, timer, stopwatch | while active |
| **Ephemeral** (Tier 3) | messages and other alerts, battery, Bluetooth, flashlight | 7s (messages) or 5s (the rest) |
| **Background** (Tier 2) | music, live activity, hotspot, transfer | while active |
| **Passive** | promos, likes, app updates | never shown |

Ephemeral sits *above* Background on purpose: a message may hijack a music pill for its few seconds and then
hand it back. Persistent sits above Ephemeral: a call or a timer is never displaced by a message.

How a plain notification (mode `Notification`) is classified, first match wins:

1. key starts with `demo_` (the in-app preview) -> Ephemeral, so previews always show;
2. category `alarm` -> Critical; category `call` -> Persistent;
3. has a quick-reply action (a person is waiting, even from a "social" app) -> Ephemeral;
4. category `promo`, `recommendation` or `social`, or the Play Store package -> **Passive**;
5. anything else, including no category at all -> Ephemeral. Hiding every app that never sets a category would
   lose real messages, so only explicit passive signals bypass the island.

Battery is Critical only when the percentage in its text is 5 or less and its title does not say "charging";
otherwise it is an Ephemeral interruption.

## 3. Two slots, and the order

The island shows **at most two states**: the first entry is the **main pill**, the second the **companion bubble**
(`AlertEngine.MAX_ACTIVE = 2`). A third is hidden, stays alive in the shade, and takes a slot when one frees up.

Order inside `evaluate`:

1. Critical, most recently started first;
2. Persistent: an incoming call first, then the most recently started;
3. Ephemeral, newest first (message groups and single interruptions together);
4. Background, newest first.

"Started" is when the engine first saw the alert's key, not the notification's own timestamp (a timer's
timestamp is its end time, which says nothing about when it began).

Consequences, each covered by a test:

- A message that arrives during a call waits behind it as the companion bubble.
- A message takes a music pill for 7s; music steps aside to the bubble, then returns.
- Music + timer: the timer (Tier 1) is the main pill and music the bubble. Add a call: call main, timer bubble,
  music hidden.
- When the main pill ends, the bubble and any hidden alert move up.

## 4. Timing

All timing is in the engine and takes `nowMs` as an argument, so it is testable without a clock.

| Alert | Timeout | Notes |
|---|---|---|
| Message / group | **7s** (`communicationTimeoutMs`) | the group's clock is the newest member's first-seen time, so a new message restarts it |
| Battery, Bluetooth, flashlight | **5s** (`transientTimeoutMs`) | restarts when the alert's **title** changes (Charging -> Low Battery); a percentage tick does not |
| Critical, Persistent, Background | none | leave when the source removes them |

- **A touch restarts every ephemeral clock** (`touch(now)`). The view model calls it on each interaction with
  the island, when the card collapses, and every second while the card is expanded or being typed into, so a
  reply in progress is never pulled out from under the user.
- Once an ephemeral alert has expired it stays hidden even though its notification is still in the shade. It is
  forgotten when the notification leaves the list, so the same key posted again is a fresh alert.
- The separate **auto-collapse** of the expanded card (5s, 10s for music) is unrelated to this and lives in
  `IslandViewModel`.

## 5. Grouping

Ephemeral message alerts from the same app fold into one entry. The group key is
`packageName + "|" + groupId` (`Notification.getGroup()`; null for most apps, so one app is one group).

The folded entry is the newest message plus `groupCount`, `memberKeys` (every key it stands for) and up to three
`groupIcons`. It never resizes the island or spawns another one.

- Pill: stacked, overlapping badges on the left and **"N New Messages from App"** on the right. Without icon
  bitmaps the badges are lettered circles.
- A new message in the group plays a spring pulse on the pill (1.05x and back) instead of redrawing it.
- Dismissing, replying to, or marking a group as read acts on **every** `memberKeys` entry.
- Group-summary notifications are already skipped by the listener, so they are not counted.

## 6. Collisions at arrival

When an alert arrives with auto-expand on, `IslandViewModel` will not auto-expand it if it is Ephemeral and the
current main pill is Critical or Persistent. The message waits as the bubble instead of stealing the island. When
the main pill changes (a higher tier arrives, or the old one ends) the user's swap selection resets to the new
main pill; otherwise a swap the user made stands.

Tapping the companion bubble selects it and expands it. Tapping the main pill expands the main alert.

## 7. What the pill and card look like

- **Single message pill:** app icon in a circle on the left; bold sender then gray preview on the right, scrolling
  sideways (marquee) with a faded right edge, and a green dot. Up to **280dp** wide.
- **Medium card (message):** pure black, about 340dp wide and 130-185dp tall, 32dp corners. Header (app icon,
  sender, "Now"), up to three lines of body, **Reply** (icon + text, opens an inline field and the keyboard) and
  an icon-only **Mark as read**. Tapping the body opens the app.
- **Music:** pill is cover + waveform only; the card has cover, track/artist, waveform, scrubber, then Repeat,
  Previous, Play/Pause, Next, End. See `MusicExpanded.kt`.
- **Idle dashboard** (large state): see `IdleDashboard.kt`.
- Content in the pill **crossfades with a spring** when the alert changes (`CollapsedSlot` in
  `IslandCollapsedContent.kt`). Updates to the same alert, or more messages from the same app, refresh in place.

### Pill width and the camera hole

The pill is centred on the camera hole and grows **symmetrically**, so the hole stays in the middle and each
side needs the same room. `PillLayout.contentWingDp(active)` says how much room the active alert's right-hand
content needs beside the hole:

- music: `MUSIC_WING_DP` (40dp), just the waveform;
- message: 26dp plus about 6.2dp per character of "sender  preview", capped at 42 characters;
- the pill never exceeds `MAX_ALERT_PILL_WIDTH_DP` (280dp); past that the text scrolls and fades.

The view, the window params and the touch region all call the same function so touches never drift from what is
drawn.

### Anchoring

The main pill is always positioned from the screen centre plus the user's X offset; the companion bubble is
placed beside it and never moves it. The bubble's side is the user's `circlePosition` setting (default right;
a phone may be set to left). Only the first two entries are ever drawn, so the third-pill code path is dormant.

## 8. Files

| File | Role |
|---|---|
| `alert/AlertEngine.kt` | `AlertTier`, `AlertClassifier`, `AlertEngine` (all rules above) |
| `ui/IslandViewModel.kt` | runs the engine, schedules expiry, collision rule, selection reset, touch |
| `ui/PillLayout.kt` | pill width, content wing, 280dp cap |
| `ui/AlertPill.kt` | message text, stacked badges, fading edge |
| `ui/IslandCollapsedContent.kt` | pill slots, crossfade, group pulse |
| `ui/expanded/NotificationExpanded.kt` | the message card, Reply, Mark as read |
| `service/SmartIslandOverlayService.kt` | window size and position, from `visibleNotifications` |
| `model/IslandNotification.kt` | `groupId`, `groupCount`, `memberKeys`, `groupIcons` |
| `src/test/.../alert/AlertEngineTest.kt` | the rules as executable examples |

## 9. Testing it

Unit tests: `./gradlew testDebugUnitTest`.

On a device or emulator, **debug builds only** include `DebugAlertReceiver`, which posts synthetic alerts so the
rules can be driven from adb (the app filters notifications posted by `adb shell cmd notification`, and a real
messaging app is awkward to script):

```bash
R=com.niteeshbharadwaj.smartisland.personal/com.agupta07505.smartisland.debug.DebugAlertReceiver
adb shell "am broadcast -n $R --es kind music --es pkg com.spotify.music --es app Spotify --es title Starlight --es text Muse"
adb shell "am broadcast -n $R --es kind message --es pkg com.whatsapp --es app WhatsApp --es title Sam --es text 'Lunch at 1?'"
adb shell "am broadcast -n $R --es kind clear"
```

`kind` is one of `message promo call alarm nav music timer stopwatch record live battery bluetooth flash clear`.
Optional: `--es key`, `--es group`, `--ez expand true`, `--ez reply true`. Quote values containing spaces with
single quotes inside the double-quoted shell string, as above.

A debug APK cannot update a release-signed install (different signature). Re-sign it with the release key first:
`apksigner sign` with the same keystore, then `adb install -r`.

## 10. Not covered

- Real notifications from third-party apps were checked through the synthetic path only; the listener's
  extraction of reply actions already existed and is unchanged.
- The swipe-up-to-dismiss gesture and the drag that morphs the card back into the pill are the app's existing
  gestures; this engine does not change them.
