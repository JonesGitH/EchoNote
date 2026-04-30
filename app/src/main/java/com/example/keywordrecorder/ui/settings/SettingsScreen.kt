package com.example.keywordrecorder.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.keywordrecorder.R
import com.example.keywordrecorder.data.TranscriptionMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = viewModel()) {
    val settings by viewModel.settings.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val keywordState = remember(settings.wakeKeyword) { mutableStateOf(settings.wakeKeyword) }
    val hourState = remember(settings.dailyTranscriptionHour) { mutableStateOf(settings.dailyTranscriptionHour.toString()) }
    val minuteState = remember(settings.dailyTranscriptionMinute) { mutableStateOf(settings.dailyTranscriptionMinute.toString()) }
    var silenceTimeout by remember(settings.silenceTimeoutSeconds) { mutableStateOf(settings.silenceTimeoutSeconds) }

    val keywordError by remember { derivedStateOf { keywordState.value.isBlank() } }
    val hourError by remember { derivedStateOf { hourState.value.toIntOrNull()?.let { it !in 0..23 } ?: true } }
    val minuteError by remember { derivedStateOf { minuteState.value.toIntOrNull()?.let { it !in 0..59 } ?: true } }

    var keywordSaved by remember { mutableStateOf(false) }
    var scheduleSaved by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(title = { Text(stringResource(R.string.settings_title), fontWeight = FontWeight.SemiBold) })

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    // — Detection —
                    SettingsSection(
                        title = stringResource(R.string.settings_section_detection),
                        icon = { Icon(Icons.Rounded.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    ) {
                        OutlinedTextField(
                            value = keywordState.value,
                            onValueChange = { keywordState.value = it; keywordSaved = false },
                            label = { Text(stringResource(R.string.keyword_label)) },
                            isError = keywordError,
                            supportingText = {
                                if (keywordError) {
                                    Text(stringResource(R.string.keyword_error))
                                } else {
                                    Text(stringResource(R.string.keyword_hint), color = MaterialTheme.colorScheme.outline)
                                }
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Button(
                            enabled = !keywordError && !keywordSaved,
                            onClick = {
                                viewModel.saveWakeKeyword(keywordState.value.trim())
                                keywordSaved = true
                                scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.snack_keyword_saved)) }
                                scope.launch { delay(2500); keywordSaved = false }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            if (keywordSaved) {
                                Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                                Text(stringResource(R.string.keyword_saved))
                            } else {
                                Text(stringResource(R.string.keyword_save))
                            }
                        }
                    }

                    // — Transcription —
                    SettingsSection(
                        title = stringResource(R.string.settings_section_transcription),
                        icon = { Icon(Icons.Rounded.AccessTime, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    ) {
                        Text(
                            stringResource(R.string.transcription_when_label),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TranscriptionMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = settings.transcriptionMode == mode,
                                    onClick = { viewModel.saveTranscriptionMode(mode) },
                                    label = {
                                        Text(
                                            when (mode) {
                                                TranscriptionMode.IMMEDIATE -> stringResource(R.string.transcription_immediate)
                                                TranscriptionMode.DAILY -> stringResource(R.string.transcription_daily)
                                                TranscriptionMode.OFF -> stringResource(R.string.transcription_off)
                                            },
                                            style = MaterialTheme.typography.labelMedium,
                                        )
                                    },
                                )
                            }
                        }
                        Text(
                            text = when (settings.transcriptionMode) {
                                TranscriptionMode.IMMEDIATE -> stringResource(R.string.transcription_desc_immediate)
                                TranscriptionMode.DAILY -> stringResource(R.string.transcription_desc_daily)
                                TranscriptionMode.OFF -> stringResource(R.string.transcription_desc_off)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                        if (settings.transcriptionMode == TranscriptionMode.DAILY) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedTextField(
                                    value = hourState.value,
                                    onValueChange = { hourState.value = it; scheduleSaved = false },
                                    label = { Text(stringResource(R.string.schedule_hour_label)) },
                                    isError = hourError,
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                )
                                OutlinedTextField(
                                    value = minuteState.value,
                                    onValueChange = { minuteState.value = it; scheduleSaved = false },
                                    label = { Text(stringResource(R.string.schedule_minute_label)) },
                                    isError = minuteError,
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            Button(
                                enabled = !hourError && !minuteError && !scheduleSaved,
                                onClick = {
                                    viewModel.saveDailyTime(hourState.value.toInt(), minuteState.value.toInt())
                                    scheduleSaved = true
                                    scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.snack_schedule_saved)) }
                                    scope.launch { delay(2500); scheduleSaved = false }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                if (scheduleSaved) {
                                    Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                                    Text(stringResource(R.string.schedule_saved))
                                } else {
                                    Text(stringResource(R.string.schedule_save))
                                }
                            }
                        }
                        SwitchRow(
                            label = stringResource(R.string.retry_failed_label),
                            description = stringResource(R.string.retry_failed_desc),
                            checked = settings.retryFailed,
                            onCheckedChange = { viewModel.saveRetryFailed(it) },
                        )
                    }

                    // — Recording —
                    SettingsSection(
                        title = stringResource(R.string.settings_section_recording),
                        icon = { Icon(Icons.Rounded.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    ) {
                        Text(
                            stringResource(R.string.max_recording_duration, settings.maxRecordingSeconds),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                stringResource(R.string.silence_timeout_label, silenceTimeout),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                stringResource(R.string.silence_timeout_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                            )
                            Slider(
                                value = silenceTimeout.toFloat(),
                                onValueChange = { silenceTimeout = it.roundToInt() },
                                valueRange = 1f..10f,
                                steps = 8,
                                onValueChangeFinished = { viewModel.saveSilenceTimeout(silenceTimeout) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }

                    // — Storage & Network —
                    SettingsSection(
                        title = stringResource(R.string.settings_section_storage),
                        icon = { Icon(Icons.Rounded.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    ) {
                        SwitchRow(
                            label = stringResource(R.string.delete_audio_label),
                            description = stringResource(R.string.delete_audio_desc),
                            checked = settings.deleteAudioAfterTranscription,
                            onCheckedChange = { viewModel.saveDeleteAudioAfterTranscription(it) },
                        )
                        SwitchRow(
                            label = stringResource(R.string.only_wifi_label),
                            checked = settings.onlyWifi,
                            onCheckedChange = { viewModel.saveOnlyWifi(it) },
                        )
                        SwitchRow(
                            label = stringResource(R.string.only_charging_label),
                            checked = settings.onlyCharging,
                            onCheckedChange = { viewModel.saveOnlyCharging(it) },
                        )
                    }
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    description: String? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (description != null) {
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsSection(
    title: String,
    icon: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            icon()
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        }
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = MaterialTheme.shapes.large,
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                content()
            }
        }
    }
}
