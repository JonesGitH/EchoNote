package com.example.keywordrecorder.ui.recordings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.keywordrecorder.data.DailySummaryEntity
import com.example.keywordrecorder.data.RecordingEntity
import com.example.keywordrecorder.data.TranscriptionStatus
import com.example.keywordrecorder.util.TimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingsScreen(
    onOpenDetail: (Long) -> Unit,
    vm: RecordingsViewModel = viewModel()
) {
    val recordings by vm.recordings.collectAsState()
    val summaries by vm.summaries.collectAsState()

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(title = { Text("Recordings") })
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (summaries.isNotEmpty()) {
                    item {
                        Text(
                            "Daily Summaries",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    items(summaries) { summary ->
                        SummaryCard(summary)
                    }
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }
                if (recordings.isNotEmpty()) {
                    item {
                        Text(
                            "Individual Recordings",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    items(recordings) { recording ->
                        RecordingCard(recording, onClick = { onOpenDetail(recording.id) })
                    }
                }
                if (summaries.isEmpty() && recordings.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp)) {
                            Text(
                                "No recordings yet.\nSay your wake keyword to start.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(summary: DailySummaryEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                TimeUtils.formatDate(summary.dateEpochMillis),
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "${summary.recordingCount} recording${if (summary.recordingCount != 1) "s" else ""} combined",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                summary.summaryText,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 4
            )
        }
    }
}

@Composable
private fun RecordingCard(recording: RecordingEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(TimeUtils.formatFull(recording.createdAtEpochMillis),
                    style = MaterialTheme.typography.titleSmall)
                StatusChip(recording.transcriptionStatus)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                TimeUtils.formatDuration(recording.durationMillis),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            recording.transcriptText?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(it, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
            }
        }
    }
}

@Composable
private fun StatusChip(status: TranscriptionStatus) {
    val (label, color) = when (status) {
        TranscriptionStatus.PENDING -> "Pending" to MaterialTheme.colorScheme.outline
        TranscriptionStatus.PROCESSING -> "Processing" to MaterialTheme.colorScheme.tertiary
        TranscriptionStatus.COMPLETED -> "Done" to MaterialTheme.colorScheme.primary
        TranscriptionStatus.FAILED -> "Failed" to MaterialTheme.colorScheme.error
        TranscriptionStatus.SKIPPED -> "Skipped" to MaterialTheme.colorScheme.outline
    }
    Surface(
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.12f)
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}
