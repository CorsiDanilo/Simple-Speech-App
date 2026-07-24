package com.anomalyzed.simplespeechkeyboard.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

enum class AppLanguage(val code: String, val displayName: String) {
    ENGLISH("en", "English"),
    ITALIAN("it", "Italiano"),
    SYSTEM("system", "System Default");

    companion object {
        fun fromCode(code: String): AppLanguage =
            values().find { it.code == code } ?: ENGLISH
    }
}

class Strings(val lang: AppLanguage) {
    val isIt = lang == AppLanguage.ITALIAN

    // Title
    val appTitle = "Simple Speech App"

    // Sections
    val serviceStatus = if (isIt) "Stato Servizi" else "Service Status"
    val transcriptionEngine = if (isIt) "Motore di Trascrizione" else "Transcription Engine"
    val cloudSettings = if (isIt) "Configurazione Cloud (Google)" else "Cloud Settings (Google)"
    val localModelManagerSection = if (isIt) "Gestione Modelli Local" else "Local Model Manager"
    val transcriptionLanguage = if (isIt) "Lingua di Trascrizione" else "Transcription Language"
    val appLanguageSection = if (isIt) "Lingua dell'App" else "App Language"
    val appUpdatesSection = if (isIt) "Aggiornamenti" else "App Updates"
    val aboutSection = if (isIt) "Informazioni" else "About"

    // Service Status items
    val accessibilityInactiveTitle = if (isIt) "Servizio Accessibilità inattivo" else "Accessibility Service Inactive"
    val accessibilityInactiveSub = if (isIt) "Tocca per attivarlo nelle impostazioni di Android." else "Tap to enable in Android settings."
    val accessibilityActiveTitle = if (isIt) "Servizio Accessibilità attivo" else "Accessibility Service Active"
    val accessibilityActiveSub = if (isIt) "Il microfono flottante comparirà quando apri la tastiera." else "Floating mic overlay will appear when keyboard opens."
    val micDeniedTitle = if (isIt) "Permesso microfono negato" else "Microphone Permission Denied"
    val micDeniedSub = if (isIt) "Tocca per richiederlo." else "Tap to grant permission."
    val micGrantedTitle = if (isIt) "Permesso microfono concesso" else "Microphone Permission Granted"

    // Engines
    val engineCloud = "Cloud (Google Gemini)"
    val engineWhisper = "Local Model (Whisper.cpp)"
    val engineGemma = "Gemma / LiteRT (Local)"
    val engineAiCore = "AICore (On-Device)"

    // Cloud config
    val apiKeyLabel = if (isIt) "Chiave API Google" else "Google API Key"
    val apiKeyHelp = if (isIt) "Ottieni una chiave API gratuita su ai.google.dev per attivare Gemini." else "Get a free API key at ai.google.dev to enable Gemini transcription."
    val modelNameLabel = if (isIt) "Modello Gemini" else "Gemini Model"

    // Local model manager item
    val modelManagerTitle = if (isIt) "Gestore Modelli Whisper" else "Whisper Model Manager"
    val activeModelPrefix = if (isIt) "Modello attivo" else "Active model"
    val noModelSelected = if (isIt) "Nessun modello selezionato" else "No model selected"

    // Languages
    val langItalian = if (isIt) "Italiano" else "Italian"
    val langEnglish = "English"
    val langAuto = if (isIt) "Rilevamento Automatico (Auto)" else "Auto Detect (Auto)"

    // Updates & About
    val checkUpdatesTitle = if (isIt) "Cerca aggiornamenti" else "Check for Updates"
    val checkUpdatesSub = if (isIt) "Controlla le release su GitHub" else "Check releases on GitHub"
    val viewChangelogTitle = if (isIt) "Visualizza Changelog" else "View Changelog"
    val viewChangelogSub = if (isIt) "Cronologia delle versioni" else "Version history"
    val versionTitle = if (isIt) "Versione" else "Version"

    // Model Manager Screen
    val modelManagerScreenTitle = if (isIt) "Gestione Modelli Whisper" else "Whisper Model Manager"
    val storageAvailable = if (isIt) "Spazio Disponibile" else "Available Storage"
    val ramTotal = if (isIt) "RAM Totale" else "Total RAM"
    val searchPlaceholder = if (isIt) "Cerca modello..." else "Search model..."
    val downloadedOnlyChip = if (isIt) "Solo scaricati" else "Downloaded only"
    val smallSizeChip = "< 300 MB"
    val downloadedModelsHeader = if (isIt) "MODELLI SCARICATI" else "DOWNLOADED MODELS"
    val availableModelsHeader = if (isIt) "DISPONIBILI PER IL DOWNLOAD" else "AVAILABLE FOR DOWNLOAD"
    val tapToSelect = if (isIt) "Tocca per selezionare" else "Tap to select"
    val activeBadge = if (isIt) "Attivo" else "Active"
    val downloadBtn = if (isIt) "Scarica" else "Download"
    val cancelBtn = if (isIt) "Annulla" else "Cancel"
    val deleteBtn = if (isIt) "Elimina" else "Delete"
    val ramWarning = if (isIt) "⚠️ Richiede almeno" else "⚠️ Requires at least"
}

val LocalAppStrings = staticCompositionLocalOf { Strings(AppLanguage.ENGLISH) }
