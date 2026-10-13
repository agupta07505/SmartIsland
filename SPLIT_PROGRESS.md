# File Split & Clean Package Progress

**Target:** Every Kotlin file ≤ ~300 lines  
**New package:** `com.srcpolygot.dynamicisland`  
**Goal:** Full iPhone 18 Pro Max Dynamic Island feature parity on Android 16

## Clean package (current)

```
com.srcpolygot.dynamicisland/
├─ domain/
│   ├─ model/
│   │   ├─ IslandMode.kt
│   │   ├─ IslandNotification.kt
│   │   └─ ActiveIslandState.kt
│   └─ detector/
│       └─ CameraCutoutDetector.kt
├─ service/
│   └─ DynamicIslandOverlayService.kt   ← skeleton
└─ ui/
    ├─ overlay/
    │   ├─ IslandOverlay.kt
    │   ├─ IslandCollapsedContent.kt
    │   ├─ IslandExpandedContent.kt       ← now dispatches to cards
    │   ├─ IslandPositionCalculator.kt
    │   ├─ IslandAnimationSpecs.kt
    │   ├─ IslandGestureHandlers.kt
    │   └─ CompactNotificationShape.kt
    └─ expanded/
        ├─ MusicExpanded.kt
        ├─ CallExpanded.kt
        └─ TimerExpanded.kt
```

## Recently completed

- Music expanded card with play/pause/skip
- Call expanded card (incoming + ongoing)
- Timer / Stopwatch expanded card
- OverlayService skeleton
- Expanded content now routes to the correct card

## Next

- Navigation, Battery/Charging, LiveActivity, Privacy cards
- NotificationListenerService in the new package
- Android 16 Live Updates wiring
- Full WindowManager Compose attachment
