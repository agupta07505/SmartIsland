# iPhone 18 Pro Max Dynamic Island → Android 16 Feature Parity

Target: Recreate (and where possible improve) every major Dynamic Island behaviour from the iPhone 18 Pro / Pro Max on Android 16 using the clean `com.srcpolygot.dynamicisland` package.

## Hardware / Visual

- [x] Collapsed black pill that fully covers the punch-hole (CameraCutoutDetector improved)
- [ ] Pure black / status-bar matching collapsed state by default
- [ ] Fluid spring expand / collapse (IslandAnimationSpecs already prepared)
- [ ] Support for up to **3 simultaneous activities** (ActiveIslandState model added)

## Live Activities & Modes (iPhone 18 Pro Max set)

| Feature | Status | Android 16 mapping |
|---------|--------|--------------------|
| Music / Media controls + album art | Partial (original) | MediaSession + Live Updates |
| Incoming / Ongoing calls | Partial | Telecom + NotificationListener |
| Timer & Stopwatch | Partial | Clock notifications + Live Updates |
| Navigation (turn-by-turn) | Partial | Google Maps Live Updates |
| Charging / Battery | Partial | Battery broadcasts |
| Live Activity (delivery, ride, sports, flight) | To implement | Android 16 Live Updates / ProgressStyle |
| Screen Recording | Partial | MediaProjection indicator |
| Voice Memo / Recording | To implement | MediaRecorder status |
| Flashlight | Partial | CameraManager torch |
| Camera / Mic privacy indicators | To implement | AppOps / Privacy indicators |
| Hotspot | Partial | Tethering callbacks |
| Bluetooth device + battery | Partial | BluetoothBattery |
| AirDrop / Nearby Share | To implement | Nearby Share status |
| Low Power Mode | To implement | PowerManager |
| Workout | To implement | Health Connect / fitness apps |

## Interaction Model (Apple style)

- Tap → expand primary activity
- Long-press → expanded controls / Clear All
- Swipe left/right → switch between the 3 activities
- Swipe up → dismiss
- Companion bubbles for 2nd & 3rd activity (compact)

## Android 16 Specific Advantages

- Native **Live Updates** (progress-centric notifications) give us a first-class system API for ongoing activities
- Status bar chips + lock screen prominence
- Better OEM support on modern devices (including future Funtouch / OriginOS)

## Implementation Order

1. Finish multi-activity (3-slot) state machine + UI
2. Wire Android 16 Live Updates into the NotificationListener
3. Implement remaining mode cards one by one (small files)
4. Privacy indicators + system events
5. Polish animations & iQOO Z9x battery rules

All new code goes into the clean package. Old SmartIsland code remains as reference until fully migrated.
