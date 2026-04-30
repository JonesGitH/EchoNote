package com.example.keywordrecorder.ui.recordings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.keywordrecorder.data.DailySummaryEntity
import com.example.keywordrecorder.data.RecordingEntity
import com.example.keywordrecorder.util.TimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingsScreen(
    onRecordingClick: (Long) -> Unit,
    vm: RecordingsViewModel = viewModel(),
) {
    val summaries by vm.summaries.collectAsStateWithLifecycle()
    val recordings by vm.recordings.collectAsStateWithLifecycle()

    Surface(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = { TopAppBar(title = { Text("Recordings") }) }
        ) { padding ->
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (summaries.isNotEmpty()) {
                    item {
                        Text("Daily Summaries", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                    }
                    items(summaries, key = { it.id }) { summary ->
                        SummaryCard(summary)
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }

                if (recordings.isNotEmpty()) {
                    item { Text("Recordings", style = MaterialTheme.typography.titleMedium) }
                    items(recordings, key = { it.id }) { recording ->
                        RecordingCard(recording, onClick = { onRecordingClick(recording.id) })
                    }
                }

                if (summaries.isEmpty() && recordings.isEmpty()) {
                    item {
                        Text(
                            "No recordings yet. Say your wake word to start.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(summary: DailySummaryEntity) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(TimeUtils.formatDate(summary.dateEpochMillis), style = MaterialTheme.typography.titleMedium)
                Text("${summary.recordingCount} clips", style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                summary.summaryText.take(200) + if (summary.summaryText.length > 200) "…" else "",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 4,
            )
        }
    }
}

@Composable
private fun RecordingCard(recording: RecordingEntity, onClick: () -> Unit) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(TimeUtils.formatEpoch(recording.createdAtEpochMillis), style = MaterialTheme.typography.titleMedium)
                Text(TimeUtils.formatDuration(recording.durationMillis), style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                recording.transcriptionStatus.name,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            recording.transcriptText?.let {
                Spacer(Modifier.height(4.dp))
                Text(it.take(100) + if (it.length > 100) "…" else "", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
