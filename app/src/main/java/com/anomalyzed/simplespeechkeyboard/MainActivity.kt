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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anomalyzed.simplespeechkeyboard.data.AppPreferences
import com.anomalyzed.simplespeechkeyboard.ui.theme.SimpleSpeechKeyboardTheme
import com.anomalyzed.simplespeechkeyboard.ui.theme.Gold
import com.anomalyzed.simplespeechkeyboard.ui.theme.DarkGray
import com.anomalyzed.simplespeechkeyboard.ui.theme.LightGray

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SimpleSpeechKeyboardTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SettingsScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val prefs = remember { AppPreferences(context) }

    var selectedEngine by remember { mutableStateOf(prefs.selectedEngine) }
    var apiKey by remember { mutableStateOf(prefs.geminiApiKey) }
    var language by remember { mutableStateOf(prefs.language) }

    var isApiKeyVisible by remember { mutableStateOf(false) }
    var showChangelogDialog by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }

    val isAccessibilityEnabled = remember {
        isAccessibilityServiceEnabled(context)
    }

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        hasMicPermission = isGranted
        if (!isGranted) {
            Toast.makeText(context, "Il permesso del microfono è necessario per la trascrizione.", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasMicPermission) {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val versionName = BuildConfig.VERSION_NAME

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Simple Speech App", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // ─── Permessi ──────────────────────────────────────────────────────
            SettingSection("Stato Servizi") {
                if (!isAccessibilityEnabled) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Servizio Accessibilità inattivo",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                "Tocca per attivarlo nelle impostazioni di Android.",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Gold,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(
                                "Servizio Accessibilità attivo",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = LightGray
                            )
                            Text(
                                "Il microfono flottante comparirà quando apri la tastiera.",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }

                HorizontalDivider(color = Color.DarkGray, thickness = 0.5.dp)

                if (!hasMicPermission) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Permesso microfono negato",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                "Tocca per richiederlo. Senza microfono l'app crasherà.",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = null,
                            tint = Gold,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(
                                "Permesso microfono concesso",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = LightGray
                            )
                        }
                    }
                }
            }

            // ─── Motore di Trascrizione ────────────────────────────────────────
            SettingSection("Motore di Trascrizione") {
                val engines = listOf(
                    AppPreferences.ENGINE_CLOUD to Pair("Gemini Cloud (API)", Icons.Default.Cloud),
                    AppPreferences.ENGINE_WHISPER to Pair("Whisper.cpp (Locale)", Icons.Default.PhoneAndroid),
                    AppPreferences.ENGINE_GEMMA to Pair("Gemma / LiteRT (Locale)", Icons.Default.Memory),
                    AppPreferences.ENGINE_AICORE to Pair("Gemini Nano (AICore)", Icons.Default.Settings)
                )

                engines.forEachIndexed { index, (key, data) ->
                    val (label, icon) = data
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedEngine = key
                                prefs.selectedEngine = key
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = if (selectedEngine == key) Gold else Color.Gray,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(16.dp))
                        Text(
                            label,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f),
                            color = if (selectedEngine == key) Gold else LightGray
                        )
                        RadioButton(
                            selected = selectedEngine == key,
                            onClick = {
                                selectedEngine = key
                                prefs.selectedEngine = key
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = Gold)
                        )
                    }
                    if (index < engines.lastIndex) {
                        HorizontalDivider(color = Color.DarkGray, thickness = 0.5.dp)
                    }
                }
            }

            // ─── Gemini API Key ────────────────────────────────────────────────
            if (selectedEngine == AppPreferences.ENGINE_CLOUD) {
                SettingSection("Gemini API Config") {
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = {
                            apiKey = it
                            prefs.geminiApiKey = it
                        },
                        label = { Text("API Key", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Gold,
                            unfocusedBorderColor = Color.Gray,
                            focusedTextColor = LightGray,
                            unfocusedTextColor = LightGray
                        ),
                        visualTransformation = if (isApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isApiKeyVisible = !isApiKeyVisible }) {
                                Icon(
                                    if (isApiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = Color.Gray
                                )
                            }
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Ottieni una chiave API gratuita su ai.google.dev per attivare la trascrizione cloud Gemini.",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }

            // ─── Lingua ───────────────────────────────────────────────────────
            SettingSection("Lingua di Trascrizione") {
                val languages = listOf(
                    "it" to "Italiano",
                    "en" to "English",
                    "auto" to "Rilevamento Automatico (Auto)"
                )
                languages.forEachIndexed { index, (code, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                language = code
                                prefs.language = code
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Language,
                            contentDescription = null,
                            tint = if (language == code) Gold else Color.Gray,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(16.dp))
                        Text(
                            label,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f),
                            color = if (language == code) Gold else LightGray
                        )
                        RadioButton(
                            selected = language == code,
                            onClick = {
                                language = code
                                prefs.language = code
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = Gold)
                        )
                    }
                    if (index < languages.lastIndex) {
                        HorizontalDivider(color = Color.DarkGray, thickness = 0.5.dp)
                    }
                }
            }

            // ─── Aggiornamenti ────────────────────────────────────────────────
            SettingSection("Aggiornamenti") {
                SettingItem(
                    icon = Icons.Default.SystemUpdate,
                    title = "Cerca aggiornamenti",
                    subtitle = "Controlla le release su GitHub",
                    onClick = { showUpdateDialog = true }
                )
                HorizontalDivider(color = Color.DarkGray, thickness = 0.5.dp)
                SettingItem(
                    icon = Icons.Default.Article,
                    title = "Visualizza Changelog",
                    subtitle = "Cronologia delle versioni",
                    onClick = { showChangelogDialog = true }
                )
            }

            // ─── Informazioni ──────────────────────────────────────────────────
            SettingSection("Informazioni") {
                SettingItem(
                    icon = Icons.Default.Info,
                    title = "Versione",
                    subtitle = versionName
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    // ─── Dialogs ─────────────────────────────────────────────────────────────
    if (showUpdateDialog) {
        AlertDialog(
            onDismissRequest = { showUpdateDialog = false },
            title = { Text("Verifica Aggiornamenti", fontWeight = FontWeight.Bold) },
            text = { Text("Nessun aggiornamento disponibile.\nStai già utilizzando l'ultima versione: $versionName") },
            confirmButton = {
                TextButton(onClick = { showUpdateDialog = false }) {
                    Text("OK", color = Gold)
                }
            }
        )
    }

    if (showChangelogDialog) {
        AlertDialog(
            onDismissRequest = { showChangelogDialog = false },
            title = { Text("Changelog", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Versione $versionName",
                        fontWeight = FontWeight.Bold,
                        color = Gold
                    )
                    Text(
                        "• Prima release di SimpleSpeechKeyboard.\n" +
                        "• Supporto completo per la dettatura universale in background.\n" +
                        "• Trascrizione in streaming reale con Whisper.cpp.\n" +
                        "• Trascrizione offline con Gemma (LiteRT-LM) e cloud con Gemini API.\n" +
                        "• Microfono flottante trascinabile sopra la tastiera Android.",
                        color = LightGray,
                        lineHeight = 20.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showChangelogDialog = false }) {
                    Text("Chiudi", color = Gold)
                }
            }
        )
    }
}

@Composable
fun SettingSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray,
            modifier = Modifier.padding(start = 4.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkGray)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                content()
            }
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
        Icon(icon, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = LightGray)
            Text(subtitle, fontSize = 12.sp, color = Color.Gray)
        }
    }
}

private fun isAccessibilityServiceEnabled(context: android.content.Context): Boolean {
    val serviceName = "${context.packageName}/${TranscriptionAccessibilityService::class.java.canonicalName}"
    val enabledServices = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false
    return enabledServices.contains(serviceName)
}
