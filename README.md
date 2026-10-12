# Realistic Dynamic Island

**Goal:** Full feature parity with the **iPhone 18 Pro Max Dynamic Island**, adapted for **Android 16**, while completely hiding the punch-hole on devices like the iQOO Z9x.

**Package:** `com.srcpolygot.dynamicisland`  
**Application ID:** `com.srcpolygot.dynamicisland`

---

## Key Targets (iPhone 18 Pro Max)

- Up to **3 simultaneous Live Activities**
- Smaller, tighter collapsed pill that covers the camera cutout
- All classic modes: Music, Calls, Timer, Navigation, Charging, Privacy indicators, Screen Recording, Voice Memos, Hotspot, etc.
- Fluid spring animations + Apple-style gestures
- Native use of **Android 16 Live Updates** where possible

See `IPHONE_18_FEATURE_PARITY.md` for the complete checklist.

---

## Clean Structure (growing)

```
com.srcpolygot.dynamicisland/
├─ domain/
│   ├─ model/
│   │   ├─ IslandMode.kt          ← expanded for full parity
│   │   ├─ IslandNotification.kt
│   │   └─ ActiveIslandState.kt   ← 3-activity support
│   └─ detector/
│       └─ CameraCutoutDetector.kt
└─ ui/overlay/
    ├─ IslandPositionCalculator.kt
    ├─ IslandAnimationSpecs.kt
    ├─ IslandGestureHandlers.kt
    └─ CompactNotificationShape.kt
```

---

## Build

```bash
git clone https://github.com/srcpolygot-eng/SmartIsland.git
cd SmartIsland
./gradlew assembleDebug
```

---

## License

GNU GPLv3 (based on original SmartIsland by Animesh Gupta)
