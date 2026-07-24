# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-07-24

### 🚀 Features & Enhancements
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
