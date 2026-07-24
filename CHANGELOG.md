# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.1.0] - 2026-07-24

### 🚀 Features & Customization
- **Microphone Customization Section**: Added dedicated customization controls in the Settings screen allowing users to adjust the floating microphone overlay button.
- **Button Size Control**: Dynamic button diameter customization ranging from 40dp to 80dp.
- **Transparency / Opacity Control**: Adjustable opacity ranging from 30% to 100%.
- **Accent Color Hue Selector**: Continuous rainbow hue slider allowing custom accent color selection for the floating microphone border.
- **Interactive Live Preview Box**: Real-time simulated preview of the floating microphone button directly inside Settings with state toggles (Idle, Recording, Processing).
- **Reset to Defaults**: Quick action to restore standard dimensions (54dp, 90% opacity, Classic Gold accent).

## [1.0.0] - 2026-07-24

### 🚀 Enhancements & UI Refinements
- **Clean Overlay UI**: Removed text preview chip next to floating microphone button for a cleaner, non-intrusive floating button.
- **Automatic Clipboard Copying**: Automatically copy transcribed text directly to system clipboard as soon as dictation completes.
- **Native whisper.cpp NDK Integration**: Added JNI bindings for local, high-performance execution of Whisper GGML models (Tiny, Base, Small).
- **Android Accessibility Service & Floating Overlay**:
  - Real-time speech-to-text dictation via a draggable floating bubble UI ([`OverlayManager`](file:///c:/Users/danil/Documents/GitHub/Projects/Simple-Speech-App/app/src/main/java/com/anomalyzed/simplespeechkeyboard/OverlayManager.kt)).
  - Automatic live text insertion into active editable fields across any Android application ([`TranscriptionAccessibilityService`](file:///c:/Users/danil/Documents/GitHub/Projects/Simple-Speech-App/app/src/main/java/com/anomalyzed/simplespeechkeyboard/TranscriptionAccessibilityService.kt)).
  - Real-time live text preview chip directly inside the floating overlay bubble.
- **Voice Activity Detection (VAD) & Phrase Chunking**:
  - Real-time RMS audio energy calculation in [`AudioRecorder.kt`](file:///c:/Users/danil/Documents/GitHub/Projects/Simple-Speech-App/app/src/main/java/com/anomalyzed/simplespeechkeyboard/engine/AudioRecorder.kt) for silence and speech detection (>500ms pauses), preventing CPU bottlenecks during long recordings.
- **Context Awareness (`initial_prompt`)**:
  - Passed initial context (existing field text) to Whisper NDK to improve grammar, punctuation, and sentence continuity.
- **Text Post-Processing & Cleanup**:
  - Created [`TextPostProcessor.kt`](file:///c:/Users/danil/Documents/GitHub/Projects/Simple-Speech-App/app/src/main/java/com/anomalyzed/simplespeechkeyboard/engine/TextPostProcessor.kt) to filter out Whisper hallucinations (*"Subtitles by..."*), remove filler words (*"ehm"*, *"uhm"*), and automatically format spacing and capitalization.
- **Integrated Model Manager**:
  - Added [`ModelRepository`](file:///c:/Users/danil/Documents/GitHub/Projects/Simple-Speech-App/app/src/main/java/com/anomalyzed/simplespeechkeyboard/data/ModelRepository.kt) for downloading, verifying, and managing GGML model files.
  - Built Model Manager UI screen in Jetpack Compose.
- **Modern Jetpack Compose UI**:
  - Redesigned Onboarding, Settings, Accessibility Configuration, and Model Selector screens using Material 3.

### 🛠️ Refactoring & Improvements
- **Engine Cleanup**: Removed legacy/obsolete modules (`AICoreEngine`, `CloudEngine`, and `LiteRTEngine`) in favor of unified `WhisperCppEngine`.
- **Configurable CPU Threads**: Added preference for configuring CPU NDK thread count in [`AppPreferences.kt`](file:///c:/Users/danil/Documents/GitHub/Projects/Simple-Speech-App/app/src/main/java/com/anomalyzed/simplespeechkeyboard/data/AppPreferences.kt).

### 🔧 Build & Infrastructure
- Added git submodule for `third_party/whisper.cpp`.
- Configured CMake `CMakeLists.txt` for NDK cross-compilation targeting `arm64-v8a` and `armeabi-v7a`.
