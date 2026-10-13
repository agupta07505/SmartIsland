# File Split & Clean Package Progress

**Target:** Every Kotlin file ≤ ~300 lines  
**New package:** `com.srcpolygot.dynamicisland`  
**Goal:** Full iPhone 18 Pro Max Dynamic Island feature parity on Android 16

## Clean package (current)

```
com.srcpolygot.dynamicisland/
├─ domain/
│   ├─ model/
│   │   ├─ IslandMode.kt              ← full parity modes
│   │   ├─ IslandNotification.kt
│   │   └─ ActiveIslandState.kt       ← 3-activity support
│   └─ detector/
│       └─ CameraCutoutDetector.kt
└─ ui/
    └─ overlay/
        ├─ IslandOverlay.kt             ← main orchestrator
        ├─ IslandCollapsedContent.kt    ← pure black pill + 3 dots
        ├─ IslandExpandedContent.kt     ← expanded card stub
        ├─ IslandPositionCalculator.kt
        ├─ IslandAnimationSpecs.kt
        ├─ IslandGestureHandlers.kt
        └─ CompactNotificationShape.kt
```

## Recently added

- Collapsed black pill that covers the punch-hole
- Support for 3 simultaneous activity indicators
- Expanded content stub
- Main IslandOverlay composable

## Still needed

- Full OverlayService + NotificationListener in the new package
- Mode-specific expanded cards (Music, Call, Timer, Navigation…)
- Android 16 Live Updates integration
- Privacy indicators (camera / mic)
- Funtouch OS battery helpers
