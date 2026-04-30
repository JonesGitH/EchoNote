package com.example.keywordrecorder.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.keywordrecorder.data.AppSettings
import com.example.keywordrecorder.data.TranscriptionMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: SettingsViewModel = viewModel()) {
    val settings by vm.settings.collectAsStateWithLifecycle()

    Surface(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = { TopAppBar(title = { Text("Settings") }) }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(padding)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                KeywordSection(settings, onSave = { vm.updateKeyword(it) })
                ModeSection(settings, onModeChange = { vm.updateMode(it) })
                if (settings.transcriptionMode == TranscriptionMode.DAILY) {
                    DailyTimeSection(settings, onSave = { h, m -> vm.updateDailyTime(h, m) })
                }
            }
        }
    }
}

@Composable
private fun KeywordSection(settings: AppSettings, onSave: (String) -> Unit) {
    var draft by rememberSaveable(settings.wakeKeyword) { mutableStateOf(settings.wakeKeyword) }

    Column {
        Text("Wake keyword", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onSave(draft) }),
            label = { Text("Keyword") },
        )
    }
}

@Composable
private fun ModeSection(settings: AppSettings, onModeChange: (TranscriptionMode) -> Unit) {
    Column {
        Text("Transcription mode", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TranscriptionMode.entries.forEach { mode ->
                FilterChip(
                    selected = settings.transcriptionMode == mode,
                    onClick = { onModeChange(mode) },
                    label = { Text(mode.name) },
                )
            }
        }
    }
}

@Composable
private fun DailyTimeSection(settings: AppSettings, onSave: (Int, Int) -> Unit) {
    var hourDraft by rememberSaveable(settings.dailyTranscriptionHour) {
        mutableStateOf(settings.dailyTranscriptionHour.toString())
    }
    var minuteDraft by rememberSaveable(settings.dailyTranscriptionMinute) {
        mutableStateOf(settings.dailyTranscriptionMinute.toString().padStart(2, '0'))
    }

    Column {
        Text("Daily transcription time", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = hourDraft,
                onValueChange = { hourDraft = it },
                label = { Text("Hour (0–23)") },
                singleLine = true,
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            )
            OutlinedTextField(
                value = minuteDraft,
                onValueChange = { minuteDraft = it },
                label = { Text("Minute") },
                singleLine = true,
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    val h = hourDraft.toIntOrNull()?.coerceIn(0, 23) ?: return@KeyboardActions
                    val m = minuteDraft.toIntOrNull()?.coerceIn(0, 59) ?: return@KeyboardActions
                    onSave(h, m)
                }),
            )
        }
    }
}
