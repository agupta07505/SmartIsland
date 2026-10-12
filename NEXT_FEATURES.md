# Next Major Feature Request (Reminder)

**User will ask to add all of Apple’s Dynamic Island features from the iPhone 18 Pro Max, adapted for Android 16.**

## Current known Dynamic Island capabilities (Apple side)

Core behaviors that should be targeted:

- Collapsed pill that expands on interaction / event
- Live Activities (ongoing events: timers, sports, deliveries, rides, workouts…)
- Media controls with album art + progress
- Incoming / ongoing calls
- Charging / battery status
- Timer & Stopwatch
- Navigation turn-by-turn
- Privacy indicators (camera / mic)
- Compact + expanded states with fluid spring animations
- Multiple concurrent activities (split island / companion bubble)
- Quick actions from the expanded card
- Always-on / Ambient style presence near the cutout

## Android 16 considerations

- Use latest NotificationListener + MediaSession APIs
- Leverage Live Updates / ongoing notifications where possible
- WindowManager + Accessibility for reliable overlay on the cutout
- Material 3 Expressive + custom spring physics for fluid feel
- Funtouch OS / iQOO specific battery & autostart handling remains critical

When the user says the word, start implementing full feature parity on top of the clean `com.srcpolygot.dynamicisland` structure.
