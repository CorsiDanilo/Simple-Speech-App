# Floating Microphone Customization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement full customization for size, opacity (transparency), and accent color of the floating microphone overlay button, complete with an interactive live preview box in the app's Settings screen.

**Architecture:** Extend `AppPreferences` with SharedPreferences keys for size, opacity, and hue. Update `OverlayManager` to dynamically size, alpha-blend, and HSV-tint the floating button and support live re-styling. Add a new Material 3 `SettingSection` in `MainActivity.kt` with live preview and custom Compose controls.

**Tech Stack:** Android, Kotlin, Jetpack Compose, Material 3, SharedPreferences, Android WindowManager/GradientDrawable.

## Global Constraints
- `overlaySizeDp`: default `54`, range `40..80`
- `overlayOpacityPercent`: default `90`, range `30..100`
- `overlayColorHue`: default `45.0f` (Gold `#D4AF37`), range `0.0f..360.0f`
- Note: Git commits are skipped as per explicit user directive ("NON COMMITTARE").

---

### Task 1: Add Localized Strings for Customization UI

**Files:**
- Modify: `app/src/main/java/com/anomalyzed/simplespeechkeyboard/ui/AppStrings.kt`

**Interfaces:**
- Produces: `Strings.micCustomizationSection`, `Strings.micSizeTitle`, `Strings.micOpacityTitle`, `Strings.micColorTitle`, `Strings.micResetDefaults`, `Strings.micLivePreview`

- [ ] **Step 1: Update `AppStrings.kt` with EN and IT strings**

Add the following properties to `Strings` class in `AppStrings.kt`:
```kotlin
val micCustomizationSection: String = if (isIt) "Personalizzazione Microfono" else "Microphone Customization"
val micSizeTitle: String = if (isIt) "Dimensione Pulsante" else "Button Size"
val micOpacityTitle: String = if (isIt) "Trasparenza / Opacità" else "Transparency / Opacity"
val micColorTitle: String = if (isIt) "Colore Accento" else "Accent Color"
val micResetDefaults: String = if (isIt) "Ripristina Predefiniti" else "Reset to Defaults"
val micLivePreview: String = if (isIt) "Anteprima Live" else "Live Preview"
```

- [ ] **Step 2: Verify code compilation**

Run `./gradlew assembleDebug` to verify string properties compile without error.

---

### Task 2: Add Customization Preferences to `AppPreferences.kt`

**Files:**
- Modify: `app/src/main/java/com/anomalyzed/simplespeechkeyboard/data/AppPreferences.kt`

**Interfaces:**
- Consumes: SharedPreferences API
- Produces: `overlaySizeDp`, `overlayOpacityPercent`, `overlayColorHue`, `resetOverlayCustomizations()`

- [ ] **Step 1: Add keys and properties in `AppPreferences.kt`**

```kotlin
var overlaySizeDp: Int
    get() = prefs.getInt(KEY_OVERLAY_SIZE, 54)
    set(value) = prefs.edit().putInt(KEY_OVERLAY_SIZE, value).apply()

var overlayOpacityPercent: Int
    get() = prefs.getInt(KEY_OVERLAY_OPACITY, 90)
    set(value) = prefs.edit().putInt(KEY_OVERLAY_OPACITY, value).apply()

var overlayColorHue: Float
    get() = prefs.getFloat(KEY_OVERLAY_HUE, 45.0f)
    set(value) = prefs.edit().putFloat(KEY_OVERLAY_HUE, value).apply()

fun resetOverlayCustomizations() {
    prefs.edit()
        .putInt(KEY_OVERLAY_SIZE, 54)
        .putInt(KEY_OVERLAY_OPACITY, 90)
        .putFloat(KEY_OVERLAY_HUE, 45.0f)
        .apply()
}
```

Add constants in `companion object`:
```kotlin
private const val KEY_OVERLAY_SIZE = "overlay_size_dp"
private const val KEY_OVERLAY_OPACITY = "overlay_opacity_percent"
private const val KEY_OVERLAY_HUE = "overlay_color_hue"
```

- [ ] **Step 2: Verify compilation**

Run `./gradlew assembleDebug` to confirm build success.

---

### Task 3: Update `OverlayManager.kt` for Dynamic Customization

**Files:**
- Modify: `app/src/main/java/com/anomalyzed/simplespeechkeyboard/OverlayManager.kt`

**Interfaces:**
- Consumes: `AppPreferences` overlay properties (`overlaySizeDp`, `overlayOpacityPercent`, `overlayColorHue`)
- Produces: Dynamic overlay dimensions, alpha blending, HSV tinting, and `updateCustomization(...)` method.

- [ ] **Step 1: Update sizing, color, and alpha logic in `OverlayManager.kt`**

In `OverlayManager.kt`:
1. Retrieve dynamic button size `val buttonSizeDp get() = prefs.overlaySizeDp`.
2. Compute dynamic HSV color:
```kotlin
fun getAccentColor(hue: Float): Int {
    return Color.HSVToColor(floatArrayOf(hue, 0.85f, 0.95f))
}
```
3. Apply `btn.alpha = prefs.overlayOpacityPercent / 100f` in `updateAppearance`.
4. Set stroke color of `GradientDrawable` in `IDLE` state using `getAccentColor(prefs.overlayColorHue)`.
5. Add `updateCustomization(sizeDp: Int, opacityPercent: Int, hue: Float)`:
```kotlin
fun updateCustomization(sizeDp: Int, opacityPercent: Int, hue: Float) {
    micButton?.let { btn ->
        val sizePx = sizeDp.dp
        val lp = btn.layoutParams
        lp.width = sizePx
        lp.height = sizePx
        btn.layoutParams = lp
        val pad = (sizePx / 4.5f).toInt()
        btn.setPadding(pad, pad, pad, pad)
    }
    overlayView?.let { updateAppearance(currentState) }
}
```

- [ ] **Step 2: Verify compilation**

Run `./gradlew assembleDebug` to ensure `OverlayManager.kt` compiles cleanly.

---

### Task 4: Add Customization UI Section with Live Preview in `MainActivity.kt`

**Files:**
- Modify: `app/src/main/java/com/anomalyzed/simplespeechkeyboard/MainActivity.kt`

**Interfaces:**
- Consumes: `AppStrings`, `AppPreferences`, `OverlayManager`
- Produces: "Microphone Customization" section in `SettingsScreen` with live interactive preview box, Size slider, Opacity slider, Rainbow Hue slider, and Reset button.

- [ ] **Step 1: Implement `MicrophoneCustomizationSection` Composable in `MainActivity.kt`**

Add composable `MicrophoneCustomizationSection` containing:
1. Live Preview container showing simulated floating button with live state toggles (Idle, Recording, Processing).
2. Size Slider (`40.0f` to `80.0f`).
3. Opacity Slider (`30.0f` to `100.0f`).
4. Hue Slider (`0.0f` to `360.0f`) with a multi-color gradient background brush.
5. "Reset to Defaults" button.

- [ ] **Step 2: Add `MicrophoneCustomizationSection` to `SettingsScreen` LazyColumn**

Insert `item { MicrophoneCustomizationSection(...) }` right after the Service Status section in `SettingsScreen`.

- [ ] **Step 3: Run full debug build and verify**

Run `./gradlew assembleDebug` and confirm `BUILD SUCCESSFUL`.
