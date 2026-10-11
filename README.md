# Realistic Dynamic Island (SmartIsland Fork)

**Customized fork of [SmartIsland](https://github.com/agupta07505/SmartIsland)** focused on creating a **realistic Dynamic Island** that also **hides the ugly punch-hole / camera cutout**.

Particularly tuned for devices like the **iQOO Z9x** (centered punch-hole + Funtouch OS).

---

## Goals of this fork

- Make the collapsed island a pure black (or status-bar matching) pill that **completely covers the punch-hole** so the camera cutout disappears visually.
- Keep all the excellent features of the original SmartIsland (notifications, media, timers, charging, gestures, etc.).
- Improve cutout detection and positioning for centered hole-punch displays.
- Better battery exemption guidance for Funtouch OS / iQOO / Vivo.
- Clean, realistic animations closer to Apple's Dynamic Island.

---

## Original Project

This is a fork of the excellent open-source project:

**Smart Island** by Animesh Gupta  
https://github.com/agupta07505/SmartIsland

Licensed under GNU GPLv3. All credit for the core architecture, Compose UI, notification handling, and feature set goes to the original author.

---

## Getting Started

1. Clone this repository:
   ```bash
   git clone https://github.com/srcpolygot-eng/SmartIsland.git
   cd SmartIsland
   ```

2. Open in Android Studio (JDK 17 recommended).

3. Build & install:
   ```bash
   ./gradlew assembleDebug
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

4. Grant the required permissions (Overlay + Notification Access) and **allow high background power consumption** on Funtouch OS (critical for the island to stay alive).

---

## Development Roadmap (this fork)

- [x] Fork created under srcpolygot-eng
- [x] README updated for new goals
- [ ] Force collapsed island size/position to cover DisplayCutout bounding rect exactly
- [ ] Default pure-black collapsed state for maximum notch hiding
- [ ] iQOO / Funtouch OS specific battery & autostart helpers
- [ ] Fine-tune spring animations for more realistic expand/collapse
- [ ] Optional "always cover cutout" mode even when idle

---

## License

Same as upstream: **GNU General Public License v3.0**
