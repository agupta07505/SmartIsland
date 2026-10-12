# File Split & Clean Package Progress

**Target:** Every Kotlin file ≤ ~300 lines  
**New package:** `com.srcpolygot.dynamicisland`

## Clean package (current)

```
com.srcpolygot.dynamicisland/
├─ domain/
│   ├─ model/
│   │   ├─ IslandMode.kt
│   │   └─ IslandNotification.kt
│   └─ detector/
│       └─ CameraCutoutDetector.kt   ← improved punch-hole coverage
└─ ui/
    └─ overlay/
        ├─ IslandPositionCalculator.kt
        ├─ IslandAnimationSpecs.kt
        ├─ IslandGestureHandlers.kt
        └─ CompactNotificationShape.kt
```

## Still to extract / migrate

- Full OverlayService + NotificationListenerService (split into smaller pieces)
- Collapsed / Expanded content composables
- Settings screens & sections
- All expanded mode cards (Music, Battery, Timer…)

## Next user request (reminder)

Add **all Apple Dynamic Island features from iPhone 18 Pro Max**, adapted for **Android 16**.

See `NEXT_FEATURES.md`.
