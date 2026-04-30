package com.example.keywordrecorder.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.keywordrecorder.data.TranscriptionMode
import com.example.keywordrecorder.util.TimeUtils
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: SettingsViewModel = viewModel()) {
    val settings by vm.settings.collectAsState()

    var keywordInput by remember(settings.wakeKeyword) { mutableStateOf(settings.wakeKeyword) }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(title = { Text("Settings") })

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {

                // Wake keyword
                SettingSection(title = "Wake Keyword") {
                    OutlinedTextField(
                        value = keywordInput,
                        onValueChange = { keywordInput = it },
                        label = { Text("Keyword") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    TextButton(
                        onClick = { vm.updateKeyword(keywordInput) },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Save")
                    }
                }

                HorizontalDivider()

                // Transcription mode
                SettingSection(title = "Transcription Mode") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TranscriptionMode.entries.forEach { mode ->
                            FilterChip(
                                selected = settings.transcriptionMode == mode,
                                onClick = { vm.updateMode(mode) },
                                label = { Text(mode.name) }
                            )
                        }
                    }
                }

                if (settings.transcriptionMode == TranscriptionMode.DAILY) {
                    HorizontalDivider()
                    DailyTimeSection(
                        hour = settings.dailyTranscriptionHour,
                        minute = settings.dailyTranscriptionMinute,
                        onSave = { h, m -> vm.updateDailyTime(h, m) }
                    )
                }

                HorizontalDivider()

                // Max recording duration slider (30s – 5min)
                SettingSection(title = "Max Recording Duration") {
                    val currentSeconds = settings.maxRecordingSeconds
                    Text(
                        text = TimeUtils.formatSeconds(currentSeconds),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Slider(
                        value = currentSeconds.toFloat(),
                        onValueChange = { vm.updateMaxRecordingSeconds(it.roundToInt()) },
                        valueRange = 30f..300f,
                        steps = 269,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("30s", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("5m", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Recording stops after this duration even without silence.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary)
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DailyTimeSection(hour: Int, minute: Int, onSave: (Int, Int) -> Unit) {
    var hourInput by remember(hour) { mutableIntStateOf(hour) }
    var minuteInput by remember(minute) { mutableIntStateOf(minute) }

    SettingSection(title = "Daily Transcription Time") {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = hourInput.toString().padStart(2, '0'),
                onValueChange = { v -> v.toIntOrNull()?.coerceIn(0, 23)?.let { hourInput = it } },
                label = { Text("HH") },
                singleLine = true,
                modifier = Modifier.width(80.dp)
            )
            Text(":", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = minuteInput.toString().padStart(2, '0'),
                onValueChange = { v -> v.toIntOrNull()?.coerceIn(0, 59)?.let { minuteInput = it } },
                label = { Text("MM") },
                singleLine = true,
                modifier = Modifier.width(80.dp)
            )
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = { onSave(hourInput, minuteInput) }) {
                Text("Save")
            }
        }
    }
}
