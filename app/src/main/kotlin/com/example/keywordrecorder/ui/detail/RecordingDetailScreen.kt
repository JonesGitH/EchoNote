package com.example.keywordrecorder.ui.detail

import android.media.MediaPlayer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.keywordrecorder.data.RecordingEntity
import com.example.keywordrecorder.util.TimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingDetailScreen(
    onBack: () -> Unit,
    vm: RecordingDetailViewModel = viewModel(),
) {
    val recording by vm.recording.collectAsStateWithLifecycle()

    Surface(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Recording Detail") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                )
            }
        ) { padding ->
            recording?.let { rec ->
                RecordingDetailContent(
                    recording = rec,
                    modifier = Modifier.padding(padding),
                    onRetranscribe = { vm.retranscribe() },
                    onDelete = {
                        vm.delete()
                        onBack()
                    },
                )
            }
        }
    }
}

@Composable
private fun RecordingDetailContent(
    recording: RecordingEntity,
    modifier: Modifier = Modifier,
    onRetranscribe: () -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    var player by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(recording.filePath) {
        onDispose {
            player?.release()
            player = null
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text(TimeUtils.formatEpoch(recording.createdAtEpochMillis), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(4.dp))
        Text(TimeUtils.formatDuration(recording.durationMillis), style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            recording.transcriptionStatus.name,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )

        Spacer(Modifier.height(16.dp))

        recording.transcriptText?.let { text ->
            Text("Transcript", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(text, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(16.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = {
                    player?.release()
                    val mp = MediaPlayer().apply {
                        setDataSource(recording.filePath)
                        prepare()
                        start()
                        setOnCompletionListener { release(); player = null }
                    }
                    player = mp
                },
                modifier = Modifier.weight(1f),
            ) {
                Text("Play")
            }
            OutlinedButton(onClick = onRetranscribe, modifier = Modifier.weight(1f)) {
                Text("Re-transcribe")
            }
        }

        Spacer(Modifier.height(8.dp))

        OutlinedButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
            Text("Delete", color = MaterialTheme.colorScheme.error)
        }
    }
}
