# Realistic Dynamic Island

**Fork of SmartIsland** focused on a realistic Dynamic Island that **hides the punch-hole** (especially on iQOO Z9x + Funtouch OS).

**New package:** `com.srcpolygot.dynamicisland`  
**Application ID:** `com.srcpolygot.dynamicisland`

---

## Goals

- Collapsed black pill fully covers the camera cutout
- Smooth, realistic expand/collapse animations
- All original SmartIsland features kept
- Clean architecture, every file ≤ ~300 lines
- Better Funtouch OS / iQOO battery & autostart handling

---

## Clean Structure (being built)

```
com.srcpolygot.dynamicisland/
├─ domain/
│   ├─ model/IslandMode.kt
│   └─ detector/CameraCutoutDetector.kt   ← improved for punch-hole coverage
├─ ui/
│   └─ overlay/
│       ├─ IslandPositionCalculator.kt
│       ├─ IslandAnimationSpecs.kt
│       ├─ IslandGestureHandlers.kt
│       └─ CompactNotificationShape.kt
└─ ... (more being extracted)
```

Old package `com.agupta07505.smartisland` still exists while we migrate.

---

## Current Progress

- [x] Fork + README
- [x] Package rename started (Gradle + new directory)
- [x] CameraCutoutDetector improved (larger padding)
- [x] Position / Animation / Gesture helpers extracted
- [ ] Full migration of remaining large files
- [ ] Pure black collapsed state by default
- [ ] Funtouch OS battery helpers

---

## Build

```bash
git clone https://github.com/srcpolygot-eng/SmartIsland.git
cd SmartIsland
./gradlew assembleDebug
```

---

## License

GNU GPLv3 (same as upstream SmartIsland by Animesh Gupta)
