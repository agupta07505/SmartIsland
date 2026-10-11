# File Split Progress

**Goal:** Every Kotlin file ≤ ~300 lines.

## Completed

- [x] `CameraCutoutDetector.kt` — improved padding for punch-hole coverage (iQOO Z9x)
- [x] `ui/overlay/IslandPositionCalculator.kt` — extracted width/height/offset helpers
- [x] `ui/overlay/CompactNotificationShape.kt` — extracted companion shape logic

## In Progress / Next

### High priority (largest files)

1. `IslandOverlayView.kt` (~66k) → split into:
   - IslandOverlayView.kt (thin orchestrator)
   - IslandPositionCalculator.kt (done)
   - CompactNotificationShape.kt (done)
   - IslandGestureHandlers.kt
   - IslandSecondaryBubble.kt
   - IslandAnimationState.kt

2. `SmartIslandOverlayService.kt` (~46k)
3. `SmartIslandNotificationListenerService.kt` (~53k)
4. `IslandCollapsedContent.kt` (~44k)
5. `SmartIslandHomeScreen.kt` (~61k)
6. `NotificationsAndPrivacySection.kt` (~89k)
7. `NotificationHistorySection.kt` (~78k)
8. `GesturesSection.kt` (~64k)
9. `SmartIslandSettingsRepository.kt` (~37k)

## Package structure being created

```
ui/
  overlay/
    IslandOverlayView.kt
    IslandPositionCalculator.kt
    CompactNotificationShape.kt
    IslandGestureHandlers.kt
    IslandSecondaryBubble.kt
  expanded/          (already exists, keep splitting if needed)
  sections/          (already exists, will split large ones)
service/
  overlay/
    ...
```

Continue extracting pure functions and small composables until every file is under the limit.
