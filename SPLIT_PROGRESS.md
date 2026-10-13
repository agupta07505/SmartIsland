# File Split & Clean Package Progress

**Target:** Every Kotlin file ≤ ~300 lines  
**New package:** `com.srcpolygot.dynamicisland`  
**Goal:** Full iPhone 18 Pro Max Dynamic Island feature parity on Android 16

## Clean package (current)

```
com.srcpolygot.dynamicisland/
├─ domain/
│   ├─ model/ (IslandMode, IslandNotification, ActiveIslandState)
│   └─ detector/CameraCutoutDetector.kt
├─ service/
│   ├─ DynamicIslandOverlayService.kt
│   └─ DynamicIslandNotificationListener.kt
└─ ui/
    ├─ overlay/
    │   ├─ IslandOverlay.kt
    │   ├─ IslandCollapsedContent.kt
    │   ├─ IslandExpandedContent.kt   ← routes all major modes
    │   └─ helpers…
    └─ expanded/
        ├─ MusicExpanded.kt
        ├─ CallExpanded.kt
        ├─ TimerExpanded.kt
        ├─ BatteryExpanded.kt
        ├─ NavigationExpanded.kt
        ├─ LiveActivityExpanded.kt
        ├─ PrivacyExpanded.kt
        └─ FlashlightExpanded.kt
```

## Just added

- Privacy indicators (Camera / Mic)
- Generic Live Activity card (delivery, ride, sports, flight)
- Flashlight card
- NotificationListenerService skeleton
- Expanded content now dispatches every major mode

All changes are live on `main`.
