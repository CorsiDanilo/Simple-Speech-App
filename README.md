# Simple Speech Keyboard (Whisper Android Accessibility App)

Un'applicazione Android privacy-first per la trascrizione vocale in tempo reale (Speech-to-Text) totalmente offline, integrata tramite Servizio di Accessibilità Android ed alimentata nativamente da **whisper.cpp**.

---

## 🌟 Caratteristiche Principali

- 🔒 **100% Offline & Privacy-First**: Tutti i dati audio vengono elaborati direttamente sul dispositivo senza inviare alcuna informazione a server esterni.
- ⚡ **Motore Nativo C++ (`whisper.cpp`)**: Integrazione JNI ad alte prestazioni per un'esecuzione rapida e a basso consumo dei modelli GGML.
- 🎯 **Servizio di Accessibilità Integrato**: Funziona su qualsiasi applicazione! Un overlay/fluttuante ti permette di dettare testo in tempo reale che viene direttamente inserito nel campo di testo attivo.
- 📦 **Gestione Modelli Intelligente**: Scarica e gestisci i modelli Whisper (Tiny, Base, Small) direttamente dall'app.
- 🎨 **Interfaccia Moderna**: Realizzata interamente con Jetpack Compose e Material 3.

---

## 📐 Architettura del Progetto

```text
Simple-Speech-App/
├── app/
│   ├── src/main/
│   │   ├── cpp/                 # Binding JNI C++ e CMake Build
│   │   │   ├── whisper_jni.cpp  # Interfaccia C++ per Whisper
│   │   │   └── CMakeLists.txt
│   │   ├── java/com/anomalyzed/simplespeechkeyboard/
│   │   │   ├── data/            # Gestione Modelli e Preferenze
│   │   │   ├── engine/          # Recording Audio (PCM 16kHz) & Whisper Engine
│   │   │   ├── ui/              # Schermate Jetpack Compose
│   │   │   ├── OverlayManager.kt# Gestione Finestra Fluttuante
│   │   │   └── TranscriptionAccessibilityService.kt # Servizio di Accessibilità
│   │   └── res/                 # Layout, Stringhe, Config Accessibilità
└── third_party/
    └── whisper.cpp              # Sottomodulo C++ Whisper Nativo
```

---

## 🛠️ Requisiti di Compilazione

- **Android Studio**: Ladybug / Jellyfish o superiore
- **Android SDK**: API Level 35 (Min SDK: 26 - Android 8.0)
- **Android NDK**: versione 25+ e **CMake** 3.22+
- **Gradle**: 8.x +

---

## 🚀 Come Compilare ed Eseguire

1. **Clona il repository (inclusi i sottomoduli)**:
   ```bash
   git clone --recursive https://github.com/CorsiDanilo/Simple-Speech-App.git
   cd Simple-Speech-App
   ```

2. **Compila la versione Debug**:
   ```bash
   ./gradlew assembleDebug
   ```
   L'APK Debug sarà generato in: `app/build/outputs/apk/debug/app-debug.apk`.

3. **Compila la versione Release**:
   ```bash
   ./gradlew assembleRelease
   ```
   L'APK Release sarà generato in: `app/build/outputs/apk/release/app-release-unsigned.apk`.

---

## 📱 Come Usare l'Applicazione

1. Avvia l'app sul dispositivo Android.
2. Scarica uno dei modelli supportati (es. `Whisper Tiny` o `Whisper Base`) nella schermata Impostazioni.
3. Abilita il **Servizio di Accessibilità** per *Simple Speech Keyboard* nelle Impostazioni di Sistema del telefono.
4. Apri qualsiasi app (es. WhatsApp, Note, Telegram), fai tap su un campo di testo e usa il pulsante dell'overlay fluttuante per iniziare la dettatura vocale!

---

## 📄 Licenza

Questo progetto è rilasciato sotto licenza MIT.
