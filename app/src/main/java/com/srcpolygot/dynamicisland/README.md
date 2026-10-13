# Clean Dynamic Island Structure

Package: `com.srcpolygot.dynamicisland`

## Recommended clean architecture

```
com.srcpolygot.dynamicisland/
├─ app/
│   └─ DynamicIslandApp.kt
├─ data/
│   ├─ model/
│   │   ├─ IslandMode.kt
│   │   ├─ IslandNotification.kt
│   │   └─ SwipeAction.kt
│   ├─ repository/
│   │   ├─ NotificationRepository.kt
│   │   ├─ SettingsRepository.kt
│   │   └─ HistoryRepository.kt
│   └─ local/
│       ├─ SettingsDataStore.kt
│       └─ HistoryDatabase.kt
├─ domain/
│   ├─ usecase/
│   └─ detector/
│       └─ CameraCutoutDetector.kt
├─ service/
│   ├─ OverlayService.kt
│   ├─ NotificationListenerService.kt
│   └─ SystemEventReceiver.kt
├─ ui/
│   ├─ overlay/
│   │   ├─ IslandOverlay.kt
│   │   ├─ IslandCollapsed.kt
│   │   ├─ IslandExpanded.kt
│   │   ├─ IslandPositionCalculator.kt
│   │   ├─ IslandAnimationSpecs.kt
│   │   ├─ IslandGestureHandlers.kt
│   │   └─ CompactNotificationShape.kt
│   ├─ expanded/
│   │   ├─ MusicCard.kt
│   │   ├─ NotificationCard.kt
│   │   ├─ BatteryCard.kt
│   │   └─ ... (one file per mode)
│   ├─ settings/
│   │   ├─ HomeScreen.kt
│   │   └─ sections/
│   └─ theme/
│       └─ Theme.kt
└─ di/
    └─ AppModule.kt
```

## Goals of this structure
- Every file ≤ 300 lines
- Clear separation of overlay, data, domain, services
- Easy to focus on punch-hole hiding + realistic animations
- Ready for iQOO Z9x / Funtouch OS specific tweaks

The old `com.agupta07505.smartisland` package remains for now while we migrate piece by piece.
