package com.anomalyzed.simplespeechkeyboard.ui.screens

import android.app.ActivityManager
import android.content.Context
import android.os.Environment
import android.os.StatFs
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anomalyzed.simplespeechkeyboard.data.AppPreferences
import com.anomalyzed.simplespeechkeyboard.data.ModelInfo
import com.anomalyzed.simplespeechkeyboard.data.ModelRepository
import com.anomalyzed.simplespeechkeyboard.ui.LocalAppStrings
import com.anomalyzed.simplespeechkeyboard.ui.theme.DarkGray
import com.anomalyzed.simplespeechkeyboard.ui.theme.Gold
import com.anomalyzed.simplespeechkeyboard.ui.theme.LightGray
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ModelManagerScreen(
    onNavigateBack: () -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val prefs = remember { AppPreferences(context) }
    val modelRepo = remember { ModelRepository(context) }
    val coroutineScope = rememberCoroutineScope()

    var selectedModelPath by remember { mutableStateOf(prefs.whisperModelPath) }
    var searchQuery by remember { mutableStateOf("") }
    var downloadedOnly by remember { mutableStateOf(false) }
    var smallSizeOnly by remember { mutableStateOf(false) }

    val downloadProgress = remember { mutableStateMapOf<String, Float>() }
    val downloadJobs = remember { mutableStateMapOf<String, Job>() }

    var downloadedIds by remember {
        mutableStateOf(
            modelRepo.catalog.filter { modelRepo.isDownloaded(it) }.map { it.id }.toSet()
        )
    }

    fun refreshDownloaded() {
        downloadedIds = modelRepo.catalog.filter { modelRepo.isDownloaded(it) }.map { it.id }.toSet()
    }

    val availableStorage = remember { getAvailableStorageFormatted() }
    val deviceRamMb = remember { getDeviceRamMb(context) }

    val visibleModels = remember(
        searchQuery, downloadedOnly, smallSizeOnly, downloadedIds, selectedModelPath
    ) {
        val query = searchQuery.trim().lowercase()
        modelRepo.catalog
            .filter { model ->
                query.isBlank() ||
                        model.displayName.lowercase().contains(query) ||
                        model.description.lowercase().contains(query) ||
                        model.fileName.lowercase().contains(query)
            }
            .filter { !downloadedOnly || it.id in downloadedIds }
            .filter { !smallSizeOnly || it.sizeBytes <= 300L * 1024L * 1024L }
    }

    val downloadedModels = remember(visibleModels, downloadedIds) {
        visibleModels.filter { it.id in downloadedIds }
    }
    val availableModels = remember(visibleModels, downloadedIds) {
        visibleModels.filter { it.id !in downloadedIds }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.modelManagerScreenTitle, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
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
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // ── Storage Info ──
            item {
                StorageInfoCard(availableStorage = availableStorage, deviceRamMb = deviceRamMb)
            }

            // ── Search & Filter ──
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text(strings.searchPlaceholder) },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, null)
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Gold,
                            unfocusedBorderColor = Color.Gray,
                            focusedTextColor = LightGray,
                            unfocusedTextColor = LightGray
                        )
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = downloadedOnly,
                            onClick = { downloadedOnly = !downloadedOnly },
                            label = { Text(strings.downloadedOnlyChip) },
                            leadingIcon = if (downloadedOnly) {
                                { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Gold.copy(alpha = 0.18f),
                                selectedLabelColor = Gold,
                                selectedLeadingIconColor = Gold
                            )
                        )
                        FilterChip(
                            selected = smallSizeOnly,
                            onClick = { smallSizeOnly = !smallSizeOnly },
                            label = { Text(strings.smallSizeChip) },
                            leadingIcon = if (smallSizeOnly) {
                                { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Gold.copy(alpha = 0.18f),
                                selectedLabelColor = Gold,
                                selectedLeadingIconColor = Gold
                            )
                        )
                    }
                }
            }

            // ── Downloaded Models ──
            if (downloadedModels.isNotEmpty()) {
                item {
                    Text(
                        "${strings.downloadedModelsHeader} (${downloadedModels.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                }
                items(
                    items = downloadedModels,
                    key = { it.id }
                ) { model ->
                    val modelPath = modelRepo.getModelPath(model)
                    val isSelected = selectedModelPath != null && selectedModelPath == modelPath
                    DownloadedModelCard(
                        model = model,
                        isSelected = isSelected,
                        onSelect = {
                            prefs.whisperModelPath = modelPath
                            prefs.selectedEngine = AppPreferences.ENGINE_WHISPER
                            selectedModelPath = modelPath
                        },
                        onDelete = {
                            modelRepo.deleteModel(model)
                            if (isSelected) {
                                prefs.whisperModelPath = null
                                selectedModelPath = null
                            }
                            refreshDownloaded()
                        }
                    )
                }
            }

            // ── Available Models ──
            if (availableModels.isNotEmpty()) {
                item {
                    Text(
                        "${strings.availableModelsHeader} (${availableModels.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                }
                items(
                    items = availableModels,
                    key = { it.id }
                ) { model ->
                    val progress = downloadProgress[model.id]
                    val isDownloading = progress != null
                    AvailableModelCard(
                        model = model,
                        isDownloading = isDownloading,
                        downloadProgress = progress ?: 0f,
                        deviceRamMb = deviceRamMb,
                        onDownload = {
                            val job = coroutineScope.launch(Dispatchers.IO) {
                                try {
                                    modelRepo.downloadModel(model) { p ->
                                        downloadProgress[model.id] = p
                                    }
                                    downloadProgress.remove(model.id)
                                    downloadJobs.remove(model.id)
                                    refreshDownloaded()
                                } catch (e: CancellationException) {
                                    downloadProgress.remove(model.id)
                                    downloadJobs.remove(model.id)
                                } catch (e: Exception) {
                                    downloadProgress.remove(model.id)
                                    downloadJobs.remove(model.id)
                                    launch(Dispatchers.Main) {
                                        Toast.makeText(
                                            context,
                                            "Download failed: ${e.localizedMessage}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            }
                            downloadJobs[model.id] = job
                            downloadProgress[model.id] = 0f
                        },
                        onCancel = {
                            downloadJobs[model.id]?.cancel()
                            downloadJobs.remove(model.id)
                            downloadProgress.remove(model.id)
                        }
                    )
                }
            }

            if (visibleModels.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No models found.", color = Color.Gray, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun StorageInfoCard(availableStorage: String, deviceRamMb: Int) {
    val strings = LocalAppStrings.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkGray)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.Storage,
                contentDescription = null,
                tint = Gold,
                modifier = Modifier.size(28.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(strings.storageAvailable, fontSize = 12.sp, color = Color.Gray)
                Text(availableStorage, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = LightGray)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(strings.ramTotal, fontSize = 12.sp, color = Color.Gray)
                Text("${deviceRamMb / 1024} GB", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = LightGray)
            }
        }
    }
}

@Composable
private fun DownloadedModelCard(
    model: ModelInfo,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    val strings = LocalAppStrings.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { if (!isSelected) onSelect() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkGray),
        border = if (isSelected) CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(Gold)
        ) else null
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (isSelected) Icons.Default.CheckCircle else Icons.Default.CloudDone,
                    contentDescription = null,
                    tint = if (isSelected) Gold else Color.LightGray,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(model.displayName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = LightGray)
                    Text(model.description, fontSize = 12.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (model.quantization.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        ModelBadge(model.quantization, Color(0xFF81C784))
                    }
                }
                Text(model.formattedSize, fontSize = 13.sp, color = Color.Gray)
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (isSelected) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Bolt, contentDescription = null, tint = Gold, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(strings.activeBadge, fontSize = 12.sp, color = Gold, fontWeight = FontWeight.Bold)
                    }
                } else {
                    TextButton(onClick = onSelect, contentPadding = PaddingValues(0.dp)) {
                        Text(strings.tapToSelect, fontSize = 12.sp, color = Gold)
                    }
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
private fun AvailableModelCard(
    model: ModelInfo,
    isDownloading: Boolean,
    downloadProgress: Float,
    deviceRamMb: Int,
    onDownload: () -> Unit,
    onCancel: () -> Unit
) {
    val strings = LocalAppStrings.current
    val hasEnoughRam = deviceRamMb >= model.minRamMb || model.minRamMb == 0
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkGray)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (isDownloading) Icons.Default.Cloud else Icons.Default.CloudDownload,
                    contentDescription = null,
                    tint = if (isDownloading) Gold else Color.LightGray,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(model.displayName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = LightGray)
                    Text(model.description, fontSize = 12.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (model.quantization.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        ModelBadge(model.quantization, Color(0xFF81C784))
                    }
                }
                Text(model.formattedSize, fontSize = 13.sp, color = Color.Gray)
            }

            if (!hasEnoughRam) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "${strings.ramWarning} ${model.minRamMb / 1024} GB RAM",
                    fontSize = 11.sp,
                    color = Color(0xFFFF9800)
                )
            }

            Spacer(Modifier.height(12.dp))
            if (isDownloading) {
                LinearProgressIndicator(
                    progress = { downloadProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = Gold,
                    trackColor = Color.DarkGray
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${(downloadProgress * 100).toInt()}%", fontSize = 12.sp, color = Color.Gray)
                    TextButton(onClick = onCancel, contentPadding = PaddingValues(0.dp)) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(strings.cancelBtn, fontSize = 12.sp)
                    }
                }
            } else {
                Button(
                    onClick = onDownload,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Gold.copy(alpha = 0.15f),
                        contentColor = Gold
                    )
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("${strings.downloadBtn} (${model.formattedSize})")
                }
            }
        }
    }
}

@Composable
private fun ModelBadge(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.14f),
        contentColor = color
    ) {
        Text(text, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}

private fun getAvailableStorageFormatted(): String {
    return try {
        val stat = StatFs(Environment.getDataDirectory().path)
        val bytesAvailable = stat.availableBytes
        val gb = bytesAvailable / (1024.0 * 1024.0 * 1024.0)
        String.format("%.1f GB", gb)
    } catch (e: Exception) {
        "N/D"
    }
}

private fun getDeviceRamMb(context: Context): Int {
    return try {
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memInfo)
        (memInfo.totalMem / (1024 * 1024)).toInt()
    } catch (e: Exception) {
        2048
    }
}
