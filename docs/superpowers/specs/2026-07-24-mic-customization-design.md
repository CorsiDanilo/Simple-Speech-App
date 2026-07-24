# Design Spec: Floating Microphone Overlay Customization

## Overview
This specification details the design for allowing users to customize the size, transparency (opacity), and accent color of the floating microphone overlay button in Simple Speech App. All customization settings are managed directly within a dedicated section in the app's primary Settings screen, featuring a real-time interactive preview box and persistent storage.

---

## 1. Data Persistence (`AppPreferences.kt`)

New persistent settings stored in `SharedPreferences`:

- `overlaySizeDp: Int`
  - **Key**: `overlay_size_dp`
  - **Default**: `54`
  - **Valid Range**: `40` to `80` (dp)
- `overlayOpacityPercent: Int`
  - **Key**: `overlay_opacity_percent`
  - **Default**: `90`
  - **Valid Range**: `30` to `100` (%)
- `overlayColorHue: Float`
  - **Key**: `overlay_color_hue`
  - **Default**: `45.0f` (Gold `#D4AF37`)
  - **Valid Range**: `0.0f` to `360.0f` (degrees)

Helper method in `AppPreferences`:
- `resetOverlayCustomizations()`: Resets `overlaySizeDp`, `overlayOpacityPercent`, and `overlayColorHue` to default values.

---

## 2. Floating Overlay (`OverlayManager.kt`)

### Dynamic Sizing & Layout Params
- `buttonSizeDp` is retrieved dynamically from `prefs.overlaySizeDp`.
- Icon padding scales proportionally to button size (`buttonSizePx / 4.5`).
- Container layout parameters use the customized pixel dimensions when creating or updating the `micButton`.

### Appearance & Color Tinting
- Accent color is calculated from HSV: `Color.HSVToColor(floatArrayOf(hue, 0.85f, 0.95f))`.
- Button opacity is applied directly: `btn.alpha = (prefs.overlayOpacityPercent / 100f)`.
- `IDLE` state uses a sleek dark background (`#1E1E1E`) with a border stroke matching the calculated accent color.
- `RECORDING` and `PROCESSING` states maintain state feedback colors while honoring transparency and size settings.

### Real-Time Update Callback
- Added `updateCustomization(sizeDp: Int, opacityPercent: Int, colorHue: Float)` to `OverlayManager` to refresh button layout and styling on-the-fly without needing service restart.

---

## 3. Settings UI & Interactive Live Preview (`MainActivity.kt` & `AppStrings.kt`)

### Settings Section: "Microphone Customization" / "Personalizzazione Microfono"
Positioned in `SettingsScreen` in `MainActivity.kt`:

1. **Interactive Live Preview Box**:
   - A Material 3 Card displaying a simulated floating microphone button.
   - Updates instantly as sliders are moved.
   - Includes state preview toggles (Idle, Recording, Processing).
2. **Size Control**:
   - `Slider` ranging from `40.0f` to `80.0f`.
   - Trailing label showing current dp value (e.g., `54 dp`).
3. **Opacity Control**:
   - `Slider` ranging from `30.0f` to `100.0f`.
   - Trailing label showing current percentage (e.g., `90 %`).
4. **Color Hue Control**:
   - Custom continuous color gradient slider ranging from `0.0f` to `360.0f`.
   - Visual color indicator swatch displaying the active color.
5. **Reset Action**:
   - "Reset to Defaults" / "Ripristina Predefiniti" button to restore 54dp, 90%, and Gold hue.

---

## 4. Internationalization (`AppStrings.kt`)

Added localized strings for English and Italian:
- `micCustomizationSection`: "Microphone Customization" / "Personalizzazione Microfono"
- `micSizeTitle`: "Button Size" / "Dimensione Pulsante"
- `micOpacityTitle`: "Transparency / Opacity" / "Trasparenza / Opacità"
- `micColorTitle`: "Accent Color" / "Colore Accento"
- `micResetDefaults`: "Reset to Defaults" / "Ripristina Predefiniti"
- `micPreviewLabel`: "Live Preview" / "Anteprima Live"
