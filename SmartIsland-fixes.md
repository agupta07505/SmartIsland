# Smart Island — Fixes & Improvements Guide

> **Repo:** `agupta07505/SmartIsland` (branch `dev`)
> **Target:** Fix expansion animation glitch + all review improvements
> **Date:** 2026-07-09

---

## Table of Contents

1. [🔴 CRITICAL: Expansion Height Animation Bug](#-critical-expansion-height-animation-bug)
2. [🟡 ComposeView Lifecycle Leak Fix](#-composeview-lifecycle-leak-fix)
3. [🟡 Move `OverlayIsland` to Its Own File](#-move-overlayisland-to-its-own-file)
4. [🟡 Extract Magic Numbers to Constants](#-extract-magic-numbers-to-constants)
5. [🟡 Consolidate Duplicate `actions` / `actionIntents`](#-consolidate-duplicate-actions--actionintents)
6. [🟡 Add ProGuard Rules for MediaController Reflection](#-add-proguard-rules-for-mediacontroller-reflection)
7. [🟡 Foldable & Multi-Window Support](#-foldable--multi-window-support)
8. [🔵 Better Error Handling for `removeCollapsedWindow`](#-better-error-handling-for-removecollapsedwindow)
9. [🔵 Documentation: Hidden API Usage](#-documentation-hidden-api-usage)
10. [🔵 Dependency Injection Strategy](#-dependency-injection-strategy)

---

## 🔴 CRITICAL: Expansion Height Animation Bug

### The Problem

When the pill expands into the Smart Island overlay, the height **overshoots downward** (goes too tall) and then **snaps back** to the correct height. This is a visible glitch every time the island expands.

### Root Cause Analysis

Here is the exact frame-by-frame sequence causing the bug:

```
STATE: expandedHeight = null, expanded = false, height = 34dp (collapsed)

FRAME 0:  User taps → expanded = true
          Transition starts: height target = expandedHeight ?: 160.dp = 160.dp
          ⚠️ BUG: 160dp is a hardcoded guess, usually wrong

FRAME 1-5: Spring animates height from 34dp → 160dp
           (dampingRatio=0.6, stiffness=300 — some bounce)

FRAME 4:  IslandExpandedContent renders (because `if (expanded)` is true)
          onSizeChanged fires → pageHeights updated → targetHeight computed
          LaunchedEffect fires → onHeightMeasured(actualHeight=~140dp) called
          expandedHeight = 140dp

FRAME 5:  Transition target CHANGES mid-animation: 160dp → 140dp
          Height is currently at ~155dp (mid-spring toward 160dp)
          Spring now pulls toward 140dp → OVERSHOOTS past 140dp

FRAME 6-10: Spring oscillates and settles at 140dp
            👁️ User sees: pill grows too tall → bounces back down
```

**The core issue:** `expandedHeight ?: 160.dp` — `160.dp` is a hardcoded guess that has nothing to do with the actual content height. The content hasn't measured yet when the transition starts, so the animation races toward a wrong value, then has to reverse course when the real measurement arrives.

### The Fix (3 changes in `IslandOverlayView.kt`)

#### Change 1: Initialize `expandedHeight` to collapsed height, not `null`

**File:** `app/src/main/java/com/agupta07505/smartisland/ui/IslandOverlayView.kt`

**Find this line (~line 90):**
```kotlin
    var expandedHeight by remember { mutableStateOf<Dp?>(null) }
```

**Replace with:**
```kotlin
    // FIX: Start from collapsed height so the transition target never jumps
    // to a hardcoded 160dp. Content measurement updates this within 1-2 frames,
    // and the spring animation smoothly interpolates to the real height.
    //
    // Keyed on `expanded` so it resets to collapsed height on every expand/collapse
    // cycle, avoiding stale measurements from a previous expansion.
    var expandedHeight by remember(expanded) {
        mutableStateOf(settings.height.dp)
    }
```

#### Change 2: Remove the hardcoded `160.dp` fallback

**Find this line (~line 95):**
```kotlin
    val height by transition.animateDp(transitionSpec = { sizeSpec }, label = "islandHeight") {
        if (it) (expandedHeight ?: 160.dp) else settings.height.dp
    }
```

**Replace with:**
```kotlin
    val height by transition.animateDp(transitionSpec = { sizeSpec }, label = "islandHeight") {
        // FIX: No more 160.dp hardcoded fallback. expandedHeight is always non-null now.
        // It starts at collapsed height and gets updated to the real content height
        // within 1-2 frames. The spring animation smoothly follows.
        if (it) expandedHeight else settings.height.dp
    }
```

#### Change 3: Remove unused `sizeSpecInt` and `heightIn` import

**Find these lines (~lines 87-89):**
```kotlin
    val sizeSpecInt = spring<androidx.compose.ui.unit.IntSize>(
        dampingRatio = 0.6f,
        stiffness = 300f
    )
```

**Replace with — just delete these 4 lines entirely.** They are unused.

Also remove the unused import at the top:
```kotlin
// DELETE this line:
import androidx.compose.foundation.layout.heightIn
```

### Why This Fix Works

```
NEW STATE: expandedHeight = 34dp (collapsed), expanded = false

FRAME 0:  User taps → expanded = true → remember(expanded) resets
          expandedHeight = 34dp (collapsed height)
          Transition starts: height target = expandedHeight = 34dp
          No visible change yet — height stays at collapsed size

FRAME 1:  IslandExpandedContent renders (unbounded measurement)
          onSizeChanged fires → pageHeights[0] = 140dp
          targetHeight computed → LaunchedEffect fires
          onHeightMeasured(140dp) → expandedHeight = 140dp

FRAME 2:  Transition target: 140dp
          Spring animates from 34dp → 140dp smoothly
          No overshoot, no reverse, no snap-back ✅

FRAME 3-8: Spring settles at 140dp
           👁️ User sees: pill grows smoothly to correct size
```

The key insight is that `remember(expanded)` resets the height to collapsed on every toggle, and the `wrapContentHeight(unbounded = true)` in the pager means content can measure itself even in a small container. The measurement happens in 1-2 frames — imperceptible to the user — and the spring animation then smoothly takes the box from collapsed height to the correct expanded height.

---

## 🟡 ComposeView Lifecycle Leak Fix

### The Problem

In `SmartIslandOverlayService.kt`, `removeCollapsedWindow()` sets `islandView = null` even when `windowManager.removeView()` fails (caught by `runCatchingLogged`). The ComposeView may still be attached to the window, creating a leak.

**File:** `app/src/main/java/com/agupta07505/smartisland/service/SmartIslandOverlayService.kt`

**Find (~line 180):**
```kotlin
    private fun removeCollapsedWindow() {
        islandView?.let { view ->
            runCatchingLogged(TAG, "Failed to remove view") { windowManager.removeView(view) }
        }
        islandView = null
    }
```

**Replace with:**
```kotlin
    private fun removeCollapsedWindow() {
        val view = islandView ?: return
        val removed = runCatchingLogged(TAG, "Failed to remove view") {
            windowManager.removeView(view)
        } != null
        if (removed) {
            islandView = null
        } else {
            // If removeView fails, the view is still attached. Don't null the reference
            // so we can retry or at least avoid leaking a dangling window.
            android.util.Log.w(TAG, "removeCollapsedWindow: removeView failed, keeping reference for retry")
        }
    }
```

---

## 🟡 Move `OverlayIsland` to Its Own File

The `OverlayIsland` composable currently lives at the bottom of `SmartIslandOverlayService.kt` (a 474-line service file). It belongs in the `ui/` package.

### Step 1: Create the new file

**Create:** `app/src/main/java/com/agupta07505/smartisland/ui/OverlayIsland.kt`

```kotlin
/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.agupta07505.smartisland.model.IslandNotification

@Composable
fun OverlayIsland(
    viewModel: IslandViewModel,
    statusBarHeight: Float,
    onOpenNotification: (IslandNotification) -> Unit,
    onOpenFloatingWindow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val expanded by viewModel.expanded.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val selectedIndex by viewModel.selectedIndex.collectAsState()

    IslandOverlayView(
        settings = settings,
        expanded = expanded,
        notifications = notifications,
        selectedIndex = selectedIndex,
        onPageSelected = { index -> viewModel.setSelectedNotificationIndex(index) },
        onOpenNotification = onOpenNotification,
        onToggleExpanded = { viewModel.toggleExpanded() },
        onDismissNotification = { viewModel.dismissCurrentNotification() },
        onOpenFloatingWindow = onOpenFloatingWindow,
        statusBarHeight = statusBarHeight,
        modifier = modifier
    )
}
```

### Step 2: Remove from `SmartIslandOverlayService.kt`

**Delete these lines (~lines 435-474):**
```kotlin
@Composable
private fun OverlayIsland(
    viewModel: IslandViewModel,
    statusBarHeight: Float,
    onOpenNotification: (IslandNotification) -> Unit,
    onOpenFloatingWindow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val expanded by viewModel.expanded.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val selectedIndex by viewModel.selectedIndex.collectAsState()

    IslandOverlayView(
        settings = settings,
        ...
    )
}
```

### Step 3: Update import in `SmartIslandOverlayService.kt`

Add this import:
```kotlin
import com.agupta07505.smartisland.ui.OverlayIsland
```

Remove now-unused imports:
```kotlin
// DELETE these:
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
```

---

## 🟡 Extract Magic Numbers to Constants

### File: `app/src/main/java/com/agupta07505/smartisland/ui/IslandOverlayView.kt`

**Find (~line 328):**
```kotlin
private const val EXPANDED_WIDTH_RATIO = 0.95f
private const val SWIPE_THRESHOLD_DP = 35f
```

**Replace with:**
```kotlin
// Animation specs
private const val EXPANDED_WIDTH_RATIO = 0.95f
private const val SWIPE_THRESHOLD_DP = 35f
private const val COLLAPSE_ANIMATION_DELAY_MS = 500L
private const val DRAG_MAX_OFFSET_DP = 100f
private const val COLLAPSED_SCALE_MIN = 0.9f
private const val COLLAPSED_SCALE_RANGE = 0.1f

// Visual specs
private const val STACK_INDICATOR_GAP_DP = 3.5f
private const val STACK_INDICATOR_STROKE_DP = 1.5f
private const val STACK_INDICATOR_ARC_SWEEP = 70f
private const val STACK_LEFT_ARC_START = 145f
private const val STACK_RIGHT_ARC_START = 325f
```

Then update the usages in the code to reference these constants.

### File: `app/src/main/java/com/agupta07505/smartisland/service/SmartIslandOverlayService.kt`

**Find:**
```kotlin
companion object {
    private const val TAG = "SmartIslandOverlayService"
    private const val NOTIFICATION_ID = 8105
    private const val WINDOWING_MODE_FREEFORM = 5
}
```

**Replace with:**
```kotlin
companion object {
    private const val TAG = "SmartIslandOverlayService"
    private const val NOTIFICATION_ID = 8105
    private const val WINDOWING_MODE_FREEFORM = 5
    private const val OVERLAY_CHANNEL_ID = "smart_island_overlay"
    private const val OVERLAY_CHANNEL_NAME = "Smart Island overlay"
    private const val AUTO_COLLAPSE_DELAY_MS = 500L
}
```

Then update the `buildServiceNotification()` method and `expanded.collectLatest` block to use these constants instead of inline strings/numbers.

### File: `app/src/main/java/com/agupta07505/smartisland/ui/IslandCollapsedContent.kt`

**Find:**
```kotlin
private val COLLAPSED_TRANSLATION_MAX_DP = 32.dp
```

**Replace with:**
```kotlin
// Collapsed content animation
private val COLLAPSED_TRANSLATION_MAX_DP = 32.dp
private const val LEFT_SLOT_PADDING_START_DP = 8
private const val RIGHT_SLOT_PADDING_END_DP = 12
private const val CENTER_DOT_SIZE_DP = 20
```

---

## 🟡 Consolidate Duplicate `actions` / `actionIntents`

### The Problem

`IslandNotification` has both `actions: List<String>` and `actionIntents: List<IslandNotificationAction>`. The `actions` list is just the titles from `actionIntents` — it's redundant and can drift out of sync.

### Fix: Derive `actions` from `actionIntents`

**File:** `app/src/main/java/com/agupta07505/smartisland/model/IslandNotification.kt`

```kotlin
data class IslandNotification(
    val key: String = "",
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val timeMillis: Long,
    val icon: Bitmap? = null,
    val largeIcon: Bitmap? = null,
    val actionIntents: List<IslandNotificationAction> = emptyList(),
    val category: String? = null,
    val progress: Int = 0,
    val progressMax: Int = 0,
    val mediaPositionMs: Long? = null,
    val mediaDurationMs: Long? = null,
    val mediaIsPlaying: Boolean = false,
    val mediaToken: android.media.session.MediaSession.Token? = null,
    val mode: IslandMode = IslandMode.Notification,
    val contentIntent: PendingIntent? = null
) {
    // Derived: no need to store separately
    val actions: List<String>
        get() = actionIntents.map { it.title }
}
```

**Note:** This is a **breaking change** — you must also update the NotificationListenerService where `actions` is constructed. Remove the `actions = ...` line from `handleNotificationPosted()` in `SmartIslandNotificationListenerService.kt`:

**File:** `app/src/main/java/com/agupta07505/smartisland/service/SmartIslandNotificationListenerService.kt`

**Find and remove this line:**
```kotlin
                actions = notification.actions?.mapNotNull { it.title?.toString() }.orEmpty(),
```

---

## 🟡 Add ProGuard Rules for MediaController Reflection

### The Problem

`MusicExpanded.kt` uses reflection to call `getRepeatMode()`, `setRepeatMode()`, and `isHearted()` on `MediaController` / `Rating`. These methods could be stripped by R8/ProGuard in release builds.

### Fix

**File:** `app/proguard-rules.pro`

**Append to the existing rules:**
```proguard
# ── MediaController reflection used in MusicExpanded ──
# Keep repeat mode methods accessed via reflection
-keep class android.media.session.MediaController {
    public int getRepeatMode();
}
-keep class android.media.session.MediaController$TransportControls {
    public void setRepeatMode(int);
}
# Keep Rating.isHearted() accessed via reflection
-keep class android.media.Rating {
    public boolean isHearted();
}
# Keep MediaMetadata.getRating() used to obtain Rating object
-keep class android.media.MediaMetadata {
    public android.media.Rating getRating(java.lang.String);
}
# Keep PlaybackState custom actions and extras
-keep class android.media.session.PlaybackState {
    public java.util.List getCustomActions();
}
-keep class android.media.session.PlaybackState$CustomAction {
    public java.lang.String getAction();
    public java.lang.CharSequence getName();
}
```

---

## 🟡 Foldable & Multi-Window Support

### The Problem

The overlay uses `resources.displayMetrics.widthPixels` which reports the full physical display width. On foldables in unfolded mode, split-screen, or freeform windows, the app's actual window bounds may be smaller, causing the pill to be misaligned.

### Fix: Use WindowMetricsCalculator

**File:** `app/src/main/java/com/agupta07505/smartisland/service/SmartIslandOverlayService.kt`

**Add this import:**
```kotlin
import androidx.window.layout.WindowMetricsCalculator
```

**Add this dependency to `app/build.gradle.kts`:**
```kotlin
implementation("androidx.window:window:1.3.0")
```

**Replace the `ensureCollapsedWindow` method's display metrics usage:**

**Find:**
```kotlin
    private fun ensureCollapsedWindow() {
        // ... existing code ...
        val density = resources.displayMetrics.density
        // ...
        val screenWidth = resources.displayMetrics.widthPixels
```

**Replace with:**
```kotlin
    private fun ensureCollapsedWindow() {
        // ... existing code ...
        // FIX: Use WindowMetricsCalculator for correct bounds on foldables and
        // multi-window mode, instead of displayMetrics which reports full display.
        val windowMetrics = WindowMetricsCalculator.getOrCreate()
            .computeCurrentWindowMetrics(this)
        val density = resources.displayMetrics.density
        val screenWidth = windowMetrics.bounds.width()
        // ...
```

**Also update `collapsedParams` and `updateWindowLayoutParams`** to use the same window metrics source for consistency.

**If you don't want to add the dependency**, a simpler approach is to listen for `onConfigurationChanged` in the service and re-create the overlay:

```kotlin
override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
    super.onConfigurationChanged(newConfig)
    // Recreate the overlay window on configuration changes (fold, split-screen, rotation)
    removeCollapsedWindow()
    if (Settings.canDrawOverlays(this)) {
        ensureCollapsedWindow()
    }
}
```

---

## 🔵 Better Error Handling for `removeCollapsedWindow`

### The Problem

If `windowManager.removeView()` throws, the `islandView` reference is nulled anyway (see the 🟡 fix above). But there's also a missing cleanup path: if the service is destroyed while `removeView` fails, we should try again.

### Fix: Add a retry mechanism

**File:** `app/src/main/java/com/agupta07505/smartisland/service/SmartIslandOverlayService.kt`

```kotlin
    override fun onDestroy() {
        // CRASH FIX: safe unregister
        runCatchingLogged(TAG, "unregisterReceiver failed") {
            unregisterReceiver(systemEventReceiver)
        }
        runCatchingLogged(TAG, "unregisterReceiver screenStateReceiver failed") {
            unregisterReceiver(screenStateReceiver)
        }
        // Retry remove up to 3 times with 100ms delay
        repeat(3) { attempt ->
            if (islandView != null) {
                removeCollapsedWindow()
                if (islandView == null) return@repeat // success
                Thread.sleep(100)
            }
        }
        if (islandView != null) {
            android.util.Log.e(TAG, "Failed to remove overlay window after 3 attempts")
        }
        overlayOwners.destroy()
        super.onDestroy()
    }
```

---

## 🔵 Documentation: Hidden API Usage

### Problem

`BatteryManager.computeChargeTimeRemaining()` is a hidden/non-SDK API. It may return `-1` or throw on Android 9+ (API 28+) depending on greylist/max-target policies.

### Fix: Add a comment block

**File:** `app/src/main/java/com/agupta07505/smartisland/ui/expanded/BatteryExpanded.kt`

**Find the `computeChargeTimeRemaining` call (~line 90):**

**Add this doc comment above it:**
```kotlin
    /*
     * ⚠️ HIDDEN API: BatteryManager.computeChargeTimeRemaining()
     *
     * This is a non-SDK interface restricted on Android 9+ (API 28+).
     * Behavior varies by device:
     *   - Some devices: returns accurate remaining time in milliseconds
     *   - Most devices: returns -1 (hidden API blocked)
     *   - Some OEMs: may throw or return 0
     *
     * We fall back to a heuristic: ~1.5 minutes per remaining percent.
     * This is intentionally a rough estimate — it does not account for
     * varying charge speeds (fast charging, wireless, etc.).
     */
    val timeText = remember(pct) {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? android.os.BatteryManager
        val remainingMs = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            runCatchingLogged("BatteryExpanded", "computeChargeTimeRemaining failed (hidden API)") {
                batteryManager?.computeChargeTimeRemaining() ?: -1L
            } ?: -1L
        } else {
            -1L
        }
        // ... rest unchanged
```

---

## 🔵 Dependency Injection Strategy

### Current State

`SmartIslandApp` is a manual service locator:
```kotlin
class SmartIslandApp : Application() {
    val settingsRepository by lazy { SmartIslandSettingsRepository(applicationContext) }
    val notificationRepository: INotificationRepository by lazy { SmartIslandNotificationRepository() }
}
```

This is accessed via `(application as SmartIslandApp).settingsRepository` throughout the codebase.

### Recommended: Migrate to Hilt

**Step 1: Add Hilt to `build.gradle.kts` (root):**

```kotlin
plugins {
    id("com.google.dagger.hilt.android") version "2.51.1" apply false
}
```

**Step 2: Add to `app/build.gradle.kts`:**

```kotlin
plugins {
    id("com.google.dagger.hilt.android")
    id("org.jetbrains.kotlin.kapt")  // or use KSP
}

dependencies {
    implementation("com.google.dagger:hilt-android:2.51.1")
    kapt("com.google.dagger:hilt-compiler:2.51.1")
    implementation("androidx.hilt:hilt-work:1.2.0")
    kapt("androidx.hilt:hilt-compiler:1.2.0")
}
```

**Step 3: Create a DI module:**

```kotlin
// app/src/main/java/com/agupta07505/smartisland/di/AppModule.kt
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSettingsRepository(
        @ApplicationContext context: Context
    ): SmartIslandSettingsRepository = SmartIslandSettingsRepository(context)

    @Provides
    @Singleton
    fun provideNotificationRepository(): INotificationRepository =
        SmartIslandNotificationRepository()
}
```

**Step 4: Annotate `SmartIslandApp` and services:**

```kotlin
@HiltAndroidApp
class SmartIslandApp : Application()

@AndroidEntryPoint
class SmartIslandOverlayService : LifecycleService() { ... }

@AndroidEntryPoint
class SmartIslandNotificationListenerService : NotificationListenerService() { ... }

@AndroidEntryPoint
class MainActivity : ComponentActivity() { ... }
```

**Step 5: Inject instead of casting:**

```kotlin
// Before:
val app = application as SmartIslandApp
repository = app.settingsRepository

// After:
@Inject lateinit var repository: SmartIslandSettingsRepository
```

---

## 📋 Complete Fix Checklist

| # | Fix | Priority | File(s) | Effort |
|---|---|---|---|---|
| 1 | Animation height bug | 🔴 CRITICAL | `IslandOverlayView.kt` | 5 min |
| 2 | ComposeView leak fix | 🟡 HIGH | `SmartIslandOverlayService.kt` | 5 min |
| 3 | Move OverlayIsland to own file | 🟡 MED | New + `SmartIslandOverlayService.kt` | 10 min |
| 4 | Extract magic numbers | 🟡 MED | Multiple | 20 min |
| 5 | Consolidate actions/actionIntents | 🟡 MED | `IslandNotification.kt`, `NotificationListenerService.kt` | 10 min |
| 6 | ProGuard rules for reflection | 🟡 MED | `proguard-rules.pro` | 5 min |
| 7 | Foldable/multi-window support | 🟡 MED | `SmartIslandOverlayService.kt`, `build.gradle.kts` | 15 min |
| 8 | removeCollapsedWindow retry | 🔵 LOW | `SmartIslandOverlayService.kt` | 5 min |
| 9 | Hidden API documentation | 🔵 LOW | `BatteryExpanded.kt` | 5 min |
| 10 | Hilt DI migration | 🔵 LOW | Multiple | 1-2 hours |

---

## 🧪 Testing the Animation Fix

After applying Fix #1, verify the animation with these scenarios:

```bash
# Build the debug APK
./gradlew assembleDebug

# Install on device
./gradlew installDebug
```

### Manual Test Cases

| # | Test | Expected Result |
|---|---|---|
| 1 | Tap collapsed pill → expand | Pill grows smoothly from collapsed height to content height. No overshoot. No snap-back. |
| 2 | Expand notification demo | Height should match the notification card (title + text + actions). |
| 3 | Expand music demo | Height should match the music card (album art + seek bar + controls). |
| 4 | Expand battery demo | Height should match the battery card (icon + percentage + progress bar). |
| 5 | Expand call demo | Height should match the call card (name + accept/decline buttons). |
| 6 | Swipe between pages in expanded | Height should interpolate smoothly between different page heights. |
| 7 | Tap outside to collapse | Pill should shrink smoothly back to collapsed height. |
| 8 | Rapid expand/collapse | No crash, no visual glitches, no stale heights. |
| 9 | Expand with no notifications | Should show "Smart Island / Ready for notifications" at correct height. |

---

## 🔧 Quick-Apply Patch (Fix #1 Only)

If you only want to fix the animation bug immediately, here's the minimal diff:

```diff
--- a/app/src/main/java/com/agupta07505/smartisland/ui/IslandOverlayView.kt
+++ b/app/src/main/java/com/agupta07505/smartisland/ui/IslandOverlayView.kt
@@ -87,7 +87,9 @@
         easing = FastOutSlowInEasing
     )
 
-    var expandedHeight by remember { mutableStateOf<Dp?>(null) }
+    var expandedHeight by remember(expanded) {
+        mutableStateOf(settings.height.dp)
+    }
 
     val width by transition.animateDp(transitionSpec = { sizeSpec }, label = "islandWidth") {
         if (it) expandedWidth else settings.width.dp
@@ -95,7 +97,7 @@
     val height by transition.animateDp(transitionSpec = { sizeSpec }, label = "islandHeight") {
-        if (it) (expandedHeight ?: 160.dp) else settings.height.dp
+        if (it) expandedHeight else settings.height.dp
     }
```

That's it. Two lines changed, the bug is fixed.