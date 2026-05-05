package com.example.keywordrecorder.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.keywordrecorder.data.TranscriptionMode
import com.example.keywordrecorder.ui.theme.*
import com.example.keywordrecorder.util.TimeUtils
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(vm: SettingsViewModel = viewModel()) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    var keywordInput by remember(settings.wakeKeyword) { mutableStateOf(settings.wakeKeyword) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        containerColor = EchoBg,
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = EchoSurface,
                    contentColor = EchoTextPrimary,
                    actionColor = EchoAccent
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            // Header
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
                Text(
                    "Settings",
                    style = MaterialTheme.typography.displayMedium,
                    color = EchoTextPrimary
                )
                Text(
                    "EchoNote v1.0",
                    style = MaterialTheme.typography.bodyMedium,
                    color = EchoTextSecondary
                )
            }

            SettingsSection(title = "Wake Keyword") {
                Text(
                    "The trigger phrase that starts a recording",
                    style = MaterialTheme.typography.bodyMedium,
                    color = EchoTextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = keywordInput,
                        onValueChange = { keywordInput = it },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EchoAccent,
                            unfocusedBorderColor = EchoBorder,
                            focusedTextColor = EchoTextPrimary,
                            unfocusedTextColor = EchoTextPrimary,
                            cursorColor = EchoAccent,
                            focusedContainerColor = EchoSurface,
                            unfocusedContainerColor = EchoSurface
                        ),
                        textStyle = MaterialTheme.typography.bodyMedium
                    )
                    Button(
                        onClick = {
                            vm.updateKeyword(keywordInput)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Wake keyword saved")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EchoAccent),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Save", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            SettingsSection(title = "Transcription Mode") {
                Text(
                    "When to process recordings into text",
                    style = MaterialTheme.typography.bodyMedium,
                    color = EchoTextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TranscriptionMode.entries.forEach { mode ->
                        val selected = settings.transcriptionMode == mode
                        if (selected) {
                            Button(
                                onClick = { vm.updateMode(mode) },
                                colors = ButtonDefaults.buttonColors(containerColor = EchoAccent),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(mode.name, color = Color.White, style = MaterialTheme.typography.labelLarge)
                            }
                        } else {
                            OutlinedButton(
                                onClick = { vm.updateMode(mode) },
                                shape = RoundedCornerShape(10.dp),
                                border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = EchoTextSecondary)
                            ) {
                                Text(mode.name, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = when (settings.transcriptionMode) {
                        TranscriptionMode.OFF       -> "Recordings are saved but never transcribed automatically."
                        TranscriptionMode.IMMEDIATE -> "Each recording is transcribed as soon as it is captured."
                        TranscriptionMode.DAILY     -> "All recordings are transcribed together once per day at the scheduled time."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = EchoTextTertiary
                )
            }

            if (settings.transcriptionMode == TranscriptionMode.DAILY) {
                DailyTimeSection(
                    hour = settings.dailyTranscriptionHour,
                    minute = settings.dailyTranscriptionMinute,
                    onSave = { h, m -> vm.updateDailyTime(h, m) }
                )
            }

            SettingsSection(title = "Max Recording Duration") {
                Text(
                    "Hard cutoff per recording — from 30 seconds to 5 minutes",
                    style = MaterialTheme.typography.bodyMedium,
                    color = EchoTextSecondary
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Maximum", style = MaterialTheme.typography.bodyMedium, color = EchoTextSecondary)
                    Text(
                        TimeUtils.formatSeconds(settings.maxRecordingSeconds),
                        style = MaterialTheme.typography.headlineSmall,
                        color = EchoAccent,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Slider(
                    value = settings.maxRecordingSeconds.toFloat(),
                    onValueChange = { vm.updateMaxRecordingSeconds(it.roundToInt()) },
                    valueRange = 30f..300f,
                    steps = 26,
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = EchoAccent,
                        activeTrackColor = EchoAccent,
                        inactiveTrackColor = EchoBorder
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("30s", "1m", "2m", "3m", "4m", "5m").forEach { label ->
                        Text(label, style = MaterialTheme.typography.labelSmall, color = EchoTextTertiary)
                    }
                }
            }

            SettingsSection(title = "Silence Timeout") {
                Text(
                    "Stop recording after this many seconds of continuous silence",
                    style = MaterialTheme.typography.bodyMedium,
                    color = EchoTextSecondary
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Silence cutoff", style = MaterialTheme.typography.bodyMedium, color = EchoTextSecondary)
                    Text(
                        "${settings.silenceTimeoutSeconds}s",
                        style = MaterialTheme.typography.headlineSmall,
                        color = EchoAccent,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Slider(
                    value = settings.silenceTimeoutSeconds.toFloat(),
                    onValueChange = { vm.updateSilenceTimeoutSeconds(it.roundToInt()) },
                    valueRange = 1f..10f,
                    steps = 8,
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = EchoAccent,
                        activeTrackColor = EchoAccent,
                        inactiveTrackColor = EchoBorder
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("1s", "2s", "4s", "6s", "8s", "10s").forEach { label ->
                        Text(label, style = MaterialTheme.typography.labelSmall, color = EchoTextTertiary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = EchoSurface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                color = EchoTextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DailyTimeSection(hour: Int, minute: Int, onSave: (Int, Int) -> Unit) {
    var showPicker by remember { mutableStateOf(false) }
    val timeState = rememberTimePickerState(
        initialHour = hour,
        initialMinute = minute,
        is24Hour = true
    )

    if (showPicker) {
        AlertDialog(
            onDismissRequest = { showPicker = false },
            containerColor = EchoSurface,
            shape = RoundedCornerShape(16.dp),
            title = {
                Text("Transcription Time", color = EchoTextPrimary, style = MaterialTheme.typography.titleLarge)
            },
            text = {
                TimePicker(
                    state = timeState,
                    colors = TimePickerDefaults.colors(
                        clockDialColor = EchoBg,
                        clockDialSelectedContentColor = EchoBg,
                        clockDialUnselectedContentColor = EchoTextPrimary,
                        selectorColor = EchoAccent,
                        containerColor = EchoSurface,
                        periodSelectorBorderColor = EchoBorder,
                        timeSelectorSelectedContainerColor = EchoAccentDim,
                        timeSelectorUnselectedContainerColor = EchoBg,
                        timeSelectorSelectedContentColor = EchoAccent,
                        timeSelectorUnselectedContentColor = EchoTextSecondary
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onSave(timeState.hour, timeState.minute)
                    showPicker = false
                }) {
                    Text("OK", color = EchoAccent, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text("Cancel", color = EchoTextSecondary)
                }
            }
        )
    }

    SettingsSection(title = "Daily Transcription Time") {
        Text(
            "Batch transcription runs at this time each day",
            style = MaterialTheme.typography.bodyMedium,
            color = EchoTextSecondary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "%02d:%02d".format(hour, minute),
                style = MaterialTheme.typography.displaySmall,
                color = EchoAccent,
                fontWeight = FontWeight.SemiBold
            )
            Button(
                onClick = { showPicker = true },
                colors = ButtonDefaults.buttonColors(containerColor = EchoAccent),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Change", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
