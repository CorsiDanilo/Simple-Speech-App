package com.anomalyzed.simplespeechkeyboard

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anomalyzed.simplespeechkeyboard.data.AppPreferences
import com.anomalyzed.simplespeechkeyboard.data.ModelRepository
import com.anomalyzed.simplespeechkeyboard.ui.AppLanguage
import com.anomalyzed.simplespeechkeyboard.ui.LocalAppStrings
import com.anomalyzed.simplespeechkeyboard.ui.Strings
import com.anomalyzed.simplespeechkeyboard.ui.screens.ModelManagerScreen
import com.anomalyzed.simplespeechkeyboard.ui.theme.SimpleSpeechKeyboardTheme
import com.anomalyzed.simplespeechkeyboard.ui.theme.Gold
import com.anomalyzed.simplespeechkeyboard.ui.theme.DarkGray
import com.anomalyzed.simplespeechkeyboard.ui.theme.LightGray

enum class AppScreen {
    Settings,
    ModelManager
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val prefs = remember { AppPreferences(context) }
            var appLanguageCode by remember { mutableStateOf(prefs.appLanguage) }

            val appStrings = remember(appLanguageCode) {
                Strings(AppLanguage.fromCode(appLanguageCode))
            }

            CompositionLocalProvider(LocalAppStrings provides appStrings) {
                SimpleSpeechKeyboardTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        var currentScreen by remember { mutableStateOf(AppScreen.Settings) }
                        when (currentScreen) {
                            AppScreen.Settings -> SettingsScreen(
                                onNavigateToModelManager = { currentScreen = AppScreen.ModelManager },
                                onAppLanguageChanged = { newLang -> appLanguageCode = newLang }
                            )
                            AppScreen.ModelManager -> ModelManagerScreen(
                                onNavigateBack = { currentScreen = AppScreen.Settings }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToModelManager: () -> Unit,
    onAppLanguageChanged: (String) -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val prefs = remember { AppPreferences(context) }
    val modelRepo = remember { ModelRepository(context) }

    var language by remember { mutableStateOf(prefs.language) }
    var currentAppLang by remember { mutableStateOf(prefs.appLanguage) }

    var showChangelogDialog by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }

    val isAccessibilityEnabled = remember { isAccessibilityServiceEnabled(context) }

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }
    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasMicPermission = isGranted
        if (!isGranted) Toast.makeText(context, "Microphone permission is required.", Toast.LENGTH_LONG).show()
    }

    LaunchedEffect(Unit) {
        if (!hasMicPermission) micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    val versionName = BuildConfig.VERSION_NAME

    val activeModelName = remember(prefs.whisperModelPath) {
        val path = prefs.whisperModelPath
        if (path == null) {
            strings.noModelSelected
        } else {
            modelRepo.catalog.find { modelRepo.getModelPath(it) == path }?.displayName ?: "Local Model"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.appTitle, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // ─── Service Status ─────────────────────────────────────────────────
            item {
                SettingSection(strings.serviceStatus) {
                    if (!isAccessibilityEnabled) {
                        SettingRow(
                            icon = Icons.Default.Warning,
                            iconTint = MaterialTheme.colorScheme.error,
                            title = strings.accessibilityInactiveTitle,
                            subtitle = strings.accessibilityInactiveSub,
                            onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
                        )
                    } else {
                        SettingRow(
                            icon = Icons.Default.CheckCircle,
                            iconTint = Gold,
                            title = strings.accessibilityActiveTitle,
                            subtitle = strings.accessibilityActiveSub
                        )
                    }
                    HorizontalDivider(color = Color.DarkGray, thickness = 0.5.dp)
                    if (!hasMicPermission) {
                        SettingRow(
                            icon = Icons.Default.Warning,
                            iconTint = MaterialTheme.colorScheme.error,
                            title = strings.micDeniedTitle,
                            subtitle = strings.micDeniedSub,
                            onClick = { micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }
                        )
                    } else {
                        SettingRow(
                            icon = Icons.Default.Mic,
                            iconTint = Gold,
                            title = strings.micGrantedTitle
                        )
                    }
                }
            }

            // ─── Whisper Model Manager (Local Model) ───────────────────────────
            item {
                SettingSection(strings.localModelManagerSection) {
                    SettingItem(
                        icon = Icons.Default.FolderZip,
                        title = strings.modelManagerTitle,
                        subtitle = "${strings.activeModelPrefix}: $activeModelName",
                        onClick = onNavigateToModelManager
                    )
                }
            }

            // ─── Transcription Language ───────────────────────────────────────
            item {
                SettingSection(strings.transcriptionLanguage) {
                    val languages = listOf("it" to strings.langItalian, "en" to strings.langEnglish, "auto" to strings.langAuto)
                    languages.forEachIndexed { index, (code, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { language = code; prefs.language = code }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Language, null, tint = if (language == code) Gold else Color.Gray, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(16.dp))
                            Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f), color = if (language == code) Gold else LightGray)
                            RadioButton(
                                selected = language == code,
                                onClick = { language = code; prefs.language = code },
                                colors = RadioButtonDefaults.colors(selectedColor = Gold)
                            )
                        }
                        if (index < languages.lastIndex) HorizontalDivider(color = Color.DarkGray, thickness = 0.5.dp)
                    }
                }
            }

            // ─── App Language (English default / Italiano / System Default) ────
            item {
                SettingSection(strings.appLanguageSection) {
                    val appLangs = listOf(
                        "en" to AppLanguage.ENGLISH.displayName,
                        "it" to AppLanguage.ITALIAN.displayName,
                        "system" to AppLanguage.SYSTEM.displayName
                    )
                    appLangs.forEachIndexed { index, (code, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    currentAppLang = code
                                    prefs.appLanguage = code
                                    onAppLanguageChanged(code)
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Translate, null, tint = if (currentAppLang == code) Gold else Color.Gray, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(16.dp))
                            Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f), color = if (currentAppLang == code) Gold else LightGray)
                            RadioButton(
                                selected = currentAppLang == code,
                                onClick = {
                                    currentAppLang = code
                                    prefs.appLanguage = code
                                    onAppLanguageChanged(code)
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = Gold)
                            )
                        }
                        if (index < appLangs.lastIndex) HorizontalDivider(color = Color.DarkGray, thickness = 0.5.dp)
                    }
                }
            }

            // ─── App Updates ──────────────────────────────────────────────────
            item {
                SettingSection(strings.appUpdatesSection) {
                    SettingItem(icon = Icons.Default.SystemUpdate, title = strings.checkUpdatesTitle, subtitle = strings.checkUpdatesSub, onClick = { showUpdateDialog = true })
                    HorizontalDivider(color = Color.DarkGray, thickness = 0.5.dp)
                    SettingItem(icon = Icons.Default.Description, title = strings.viewChangelogTitle, subtitle = strings.viewChangelogSub, onClick = { showChangelogDialog = true })
                }
            }

            // ─── About ────────────────────────────────────────────────────────
            item {
                SettingSection(strings.aboutSection) {
                    SettingItem(icon = Icons.Default.Info, title = strings.versionTitle, subtitle = versionName)
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    // ─── Dialogs ───────────────────────────────────────────────────────────────
    if (showUpdateDialog) {
        AlertDialog(
            onDismissRequest = { showUpdateDialog = false },
            title = { Text(strings.checkUpdatesTitle, fontWeight = FontWeight.Bold) },
            text = { Text("No updates available.\nYou are running the latest version: $versionName") },
            confirmButton = { TextButton(onClick = { showUpdateDialog = false }) { Text("OK", color = Gold) } }
        )
    }
    if (showChangelogDialog) {
        AlertDialog(
            onDismissRequest = { showChangelogDialog = false },
            title = { Text("Changelog", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Version $versionName", fontWeight = FontWeight.Bold, color = Gold)
                    Text(
                        "• SimpleSpeechKeyboard Release.\n" +
                        "• Universal background voice dictation.\n" +
                        "• 100% Offline Local Whisper.cpp engine.\n" +
                        "• Offline Whisper model manager with downloads.\n" +
                        "• Real-time streaming dictation while speaking.\n" +
                        "• App UI language switcher (English default & Italian).",
                        color = LightGray, lineHeight = 20.sp
                    )
                }
            },
            confirmButton = { TextButton(onClick = { showChangelogDialog = false }) { Text("Close", color = Gold) } }
        )
    }
}

@Composable
fun SettingSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(start = 4.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkGray)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) { content() }
        }
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = iconTint, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = LightGray)
            if (subtitle != null) Text(subtitle, fontSize = 12.sp, color = Color.Gray)
        }
    }
}

@Composable
fun SettingItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Color.LightGray, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = LightGray)
            Text(subtitle, fontSize = 12.sp, color = Color.Gray)
        }
        if (onClick != null) {
            Icon(Icons.Default.ChevronRight, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
        }
    }
}

private fun isAccessibilityServiceEnabled(context: android.content.Context): Boolean {
    val serviceName = "${context.packageName}/${TranscriptionAccessibilityService::class.java.canonicalName}"
    val enabledServices = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
    return enabledServices.contains(serviceName)
}
