package com.example.keywordrecorder.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.keywordrecorder.data.TranscriptionMode
import com.example.keywordrecorder.ui.theme.*
import com.example.keywordrecorder.util.TimeUtils
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(vm: SettingsViewModel = viewModel()) {
    val settings by vm.settings.collectAsState()
    var keywordInput by remember(settings.wakeKeyword) { mutableStateOf(settings.wakeKeyword) }

    Surface(modifier = Modifier.fillMaxSize(), color = TermBg) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "SETTINGS",
                    color = TermCyan,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text("EchoNote v1.0", color = TermTextDim, style = MaterialTheme.typography.bodySmall)
            }
            TermDivider()

            // Wake keyword
            TermPanel(title = "WAKE KEYWORD") {
                Text(
                    "Trigger word that starts recording",
                    color = TermTextDim,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(">", color = TermCyan, style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(
                        value = keywordInput,
                        onValueChange = { keywordInput = it },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TermCyan,
                            unfocusedBorderColor = TermBorder,
                            focusedTextColor = TermTextNormal,
                            unfocusedTextColor = TermTextNormal,
                            cursorColor = TermCyan,
                            focusedContainerColor = TermSurface,
                            unfocusedContainerColor = TermSurface
                        ),
                        textStyle = MaterialTheme.typography.bodyMedium
                    )
                    TermButton(
                        label = "SAVE",
                        color = TermGreen,
                        onClick = { vm.updateKeyword(keywordInput) }
                    )
                }
            }

            // Transcription mode
            TermPanel(title = "TRANSCRIPTION MODE") {
                Text(
                    "When to run transcription on recordings",
                    color = TermTextDim,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TranscriptionMode.entries.forEach { mode ->
                        val selected = settings.transcriptionMode == mode
                        val color = if (selected) TermCyan else TermTextDim
                        TermButton(
                            label = mode.name,
                            color = color,
                            onClick = { vm.updateMode(mode) },
                            enabled = true
                        )
                    }
                }
            }

            // Daily time picker (only when DAILY mode)
            if (settings.transcriptionMode == TranscriptionMode.DAILY) {
                DailyTimePanel(
                    hour = settings.dailyTranscriptionHour,
                    minute = settings.dailyTranscriptionMinute,
                    onSave = { h, m -> vm.updateDailyTime(h, m) }
                )
            }

            // Max recording duration slider
            TermPanel(title = "MAX RECORDING DURATION") {
                Text(
                    "Hard cutoff for each recording (30s – 5m)",
                    color = TermTextDim,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(12.dp))

                val currentSeconds = settings.maxRecordingSeconds
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "MAX_DURATION",
                        color = MaterialTheme.colorScheme.outlineVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "= ${TimeUtils.formatSeconds(currentSeconds)}",
                        color = TermCyan,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                // Colored progress bar showing position
                TermProgressBar(
                    progress = (currentSeconds - 30f) / (300f - 30f),
                    barWidth = 24,
                    color = TermPurpleBar
                )
                Spacer(modifier = Modifier.height(8.dp))

                Slider(
                    value = currentSeconds.toFloat(),
                    onValueChange = { vm.updateMaxRecordingSeconds(it.roundToInt()) },
                    valueRange = 30f..300f,
                    steps = 269,
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = TermCyan,
                        activeTrackColor = TermPurpleBar,
                        inactiveTrackColor = TermBorder
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("30s", color = TermTextDim, style = MaterialTheme.typography.labelSmall)
                    Text("1m", color = TermTextDim, style = MaterialTheme.typography.labelSmall)
                    Text("2m", color = TermTextDim, style = MaterialTheme.typography.labelSmall)
                    Text("3m", color = TermTextDim, style = MaterialTheme.typography.labelSmall)
                    Text("4m", color = TermTextDim, style = MaterialTheme.typography.labelSmall)
                    Text("5m", color = TermTextDim, style = MaterialTheme.typography.labelSmall)
                }
            }

            // Key hints
            TermPanel(title = "HINTS") {
                TermKeyHint("SAVE", "tap Save after changing keyword")
                Spacer(modifier = Modifier.height(4.dp))
                TermKeyHint("DAILY", "shows time picker when DAILY mode active")
                Spacer(modifier = Modifier.height(4.dp))
                TermKeyHint("SLIDER", "drag to set max recording length")
            }
        }
    }
}

@Composable
private fun DailyTimePanel(hour: Int, minute: Int, onSave: (Int, Int) -> Unit) {
    var hourInput by remember(hour) { mutableIntStateOf(hour) }
    var minuteInput by remember(minute) { mutableIntStateOf(minute) }

    TermPanel(title = "DAILY TRANSCRIPTION TIME") {
        Text(
            "Batch transcription runs at HH:MM daily",
            color = TermTextDim,
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(">", color = TermCyan, style = MaterialTheme.typography.bodyMedium)
            OutlinedTextField(
                value = hourInput.toString().padStart(2, '0'),
                onValueChange = { v -> v.toIntOrNull()?.coerceIn(0, 23)?.let { hourInput = it } },
                singleLine = true,
                modifier = Modifier.width(72.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TermCyan,
                    unfocusedBorderColor = TermBorder,
                    focusedTextColor = TermTextNormal,
                    unfocusedTextColor = TermTextNormal,
                    cursorColor = TermCyan,
                    focusedContainerColor = TermSurface,
                    unfocusedContainerColor = TermSurface
                ),
                textStyle = MaterialTheme.typography.bodyMedium,
                label = { Text("HH", color = TermTextDim, style = MaterialTheme.typography.labelSmall) }
            )
            Text(":", color = TermCyan, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = minuteInput.toString().padStart(2, '0'),
                onValueChange = { v -> v.toIntOrNull()?.coerceIn(0, 59)?.let { minuteInput = it } },
                singleLine = true,
                modifier = Modifier.width(72.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TermCyan,
                    unfocusedBorderColor = TermBorder,
                    focusedTextColor = TermTextNormal,
                    unfocusedTextColor = TermTextNormal,
                    cursorColor = TermCyan,
                    focusedContainerColor = TermSurface,
                    unfocusedContainerColor = TermSurface
                ),
                textStyle = MaterialTheme.typography.bodyMedium,
                label = { Text("MM", color = TermTextDim, style = MaterialTheme.typography.labelSmall) }
            )
            Spacer(modifier = Modifier.weight(1f))
            TermButton(label = "SAVE", color = TermGreen, onClick = { onSave(hourInput, minuteInput) })
        }
    }
}
