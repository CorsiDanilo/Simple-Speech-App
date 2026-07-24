# Simple Speech Keyboard (Whisper Android Accessibility App)

A privacy-first, 100% offline real-time speech-to-text (STT) Android application powered natively by **whisper.cpp** and integrated seamlessly via an Android Accessibility Service.

---

## 🌟 Key Features

- 🔒 **100% Offline & Privacy-First**: All audio processing happens strictly on-device. No audio data or transcripts ever leave your phone.
- ⚡ **Native C++ Engine (`whisper.cpp`)**: High-performance JNI integration for fast, low-latency, and battery-efficient GGML model execution.
- 🎯 **System-Wide Accessibility Overlay**: Works across any Android app! A floating, draggable microphone bubble lets you dictate text in real time directly into any active text input field.
- 🎨 **Microphone Customization & Live Preview**: Full customization for floating microphone button size (40-80dp), opacity (30-100%), and accent color hue (0-360°) with a real-time interactive preview box in Settings.
- 💬 **Live Text Preview**: Real-time preview chip displayed directly inside the floating overlay bubble as you speak.
- 🎙️ **Voice Activity Detection (VAD) & Silence Chunking**: Smart audio energy monitoring (RMS) breaks continuous speech into natural phrases upon silence pauses (>500ms), eliminating CPU bottlenecks.
- 🧠 **Context Awareness (`initial_prompt`)**: Passes existing text from the input field to Whisper NDK to maintain grammar, capitalization, punctuation, and vocabulary consistency.
- 🧹 **Automatic Text Cleaning**: Post-processing filter removes Whisper hallucinations (*"Subtitles by..."*), vocal hesitation fillers (*"ehm"*, *"uhm"*), and fixes spacing/punctuation.
- 📦 **Smart Model Manager**: Download, verify, and switch between Whisper models (Tiny, Base, Small) in GGML format directly within the app.
- 🎨 **Modern Jetpack Compose UI**: Beautiful, intuitive interface built with Jetpack Compose and Material 3 design guidelines.

---

## 📐 Project Architecture

```text
Simple-Speech-App/
├── app/
│   ├── src/main/
│   │   ├── cpp/                 # C++ JNI Bindings & CMake Build
│   │   │   ├── whisper/         # JNI bridge (jni.cpp) & CMakeLists.txt
│   │   ├── java/com/anomalyzed/simplespeechkeyboard/
│   │   │   ├── data/            # Model Repository, Preferences & Metadata
│   │   │   ├── engine/          # Audio Recorder (PCM 16kHz VAD), PostProcessor & Whisper Engine
│   │   │   ├── ui/              # Jetpack Compose UI Screens & Theme
│   │   │   ├── whisper/         # WhisperContext & Native JNI Wrapper
│   │   │   ├── OverlayManager.kt# Floating Bubble UI & Live Text Preview Chip
│   │   │   └── TranscriptionAccessibilityService.kt # Accessibility Service & Text Injection
│   │   └── res/                 # Layouts, Strings, & Accessibility Service Config
└── third_party/
    └── whisper.cpp              # Native C++ Whisper Submodule
```

---

## 🛠️ Build Requirements

- **Android Studio**: Ladybug / Jellyfish or newer
- **Android SDK**: Target API 35 (Min SDK: 26 - Android 8.0)
- **Android NDK**: Version 25+ with **CMake** 3.22+
- **Gradle**: 8.x +

---

## 🚀 Building & Running

1. **Clone the repository (with submodules)**:
   ```bash
   git clone --recursive https://github.com/CorsiDanilo/Simple-Speech-App.git
   cd Simple-Speech-App
   ```

2. **Build Debug APK**:
   ```bash
   ./gradlew assembleDebug
   ```
   The Debug APK will be generated at: `app/build/outputs/apk/debug/app-debug.apk`.

3. **Build Release APK**:
   ```bash
   ./gradlew assembleRelease
   ```
   The Release APK will be generated at: `app/build/outputs/apk/release/app-release-unsigned.apk`.

---

## 📱 How to Use

1. Launch the app on your Android device.
2. Download a supported model (e.g., `Whisper Tiny` or `Whisper Base`) in the **Model Manager** screen.
3. Enable the **Accessibility Service** for *Simple Speech Keyboard* in Android System Settings.
4. Open any app (e.g., WhatsApp, Notes, Telegram, Messages), tap on a text field, and tap the floating microphone overlay to start voice dictation!

---

## 📄 License

This project is licensed under the MIT License.
