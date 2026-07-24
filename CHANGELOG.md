# Changelog

Tutti i cambiamenti notevoli a questo progetto saranno documentati in questo file.

Il formato si basa su [Keep a Changelog](https://keepachangelog.com/it/1.0.0/),
e questo progetto aderisce a [Semantic Versioning](https://semver.org/lang/it/).

## [1.0.0] - 2026-07-24

### 🚀 Nuove Funzionalità
- **Integrazione NDK whisper.cpp**: Aggiunto supporto nativo C++ JNI per l'elaborazione locale e ad alte prestazioni dei modelli GGML Whisper (Tiny, Base, Small).
- **Servizio di Accessibilità con Floating Overlay**:
  - Trascrizione in tempo reale con bolla/overlay fluttuante (`OverlayManager`).
  - Inserimento automatico del testo trascritto nel campo di testo attivo (`TranscriptionAccessibilityService`).
  - Rilevamento automatico dello stato del microfono e pulsante per avvio/stop registrazione.
- **Gestione Modelli Integrata**:
  - `ModelRepository` per il download, la verifica ed la gestione dei file `.bin` GGML di Whisper.
  - Schermata di selezione modello nelle impostazioni dell'applicazione.
- **Interfaccia Utente Modernizzata**:
  - Schermate Jetpack Compose per onboarding, impostazioni e configurazione del servizio di accessibilità.
  - Supporto stringhe localizzate e personalizzazione interfaccia (`AppStrings`).

### 🛠️ Miglioramenti e Refactoring
- **Rimozione Motori Obsoleto**: Eliminati i vecchi moduli `AICoreEngine`, `CloudEngine` e `LiteRTEngine` a favore di `WhisperCppEngine`.
- **Ottimizzazione AudioRecorder**: Registrazione audio PCM 16-bit a 16kHz ottimizzata per l'input nativo di Whisper.
- **Persistenza Preferenze**: `AppPreferences` aggiornato per salvare le preferenze sul modello selezionato, lingua e stato overlay.

### 🔧 Build & Infrastruttura
- Aggiunto sottomodulo git per `third_party/whisper.cpp`.
- Configurazione CMake `CMakeLists.txt` per compilazione cross-platform ARM64-v8a / ARMEabi-v7a in NDK.
