package com.example.keywordrecorder.ui.recordings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.keywordrecorder.data.DailySummaryEntity
import com.example.keywordrecorder.data.RecordingEntity
import com.example.keywordrecorder.data.TranscriptionStatus
import com.example.keywordrecorder.ui.home.WaveformBars
import com.example.keywordrecorder.ui.theme.*
import com.example.keywordrecorder.util.TimeUtils

@Composable
fun RecordingsScreen(
    onOpenDetail: (Long) -> Unit,
    vm: RecordingsViewModel = viewModel()
) {
    val recordings by vm.recordings.collectAsStateWithLifecycle()
    val summaries by vm.summaries.collectAsStateWithLifecycle()

    Surface(modifier = Modifier.fillMaxSize(), color = EchoBg) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "All Recordings",
                        style = MaterialTheme.typography.headlineLarge,
                        color = EchoTextPrimary
                    )
                    Text(
                        "${recordings.size} notes",
                        style = MaterialTheme.typography.bodyMedium,
                        color = EchoTextSecondary
                    )
                }
            }

            if (summaries.isNotEmpty()) {
                item {
                    Text(
                        "Daily Summaries",
                        style = MaterialTheme.typography.titleMedium,
                        color = EchoTextSecondary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp, start = 4.dp)
                    )
                }
                items(summaries, key = { it.id }) { summary ->
                    SummaryCard(summary)
                }
            }

            if (recordings.isNotEmpty()) {
                item {
                    Text(
                        "Notes",
                        style = MaterialTheme.typography.titleMedium,
                        color = EchoTextSecondary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp, start = 4.dp)
                    )
                }
                items(recordings, key = { it.id }) { rec ->
                    RecordingListCard(rec, onClick = { onOpenDetail(rec.id) })
                }
            }

            if (recordings.isEmpty() && summaries.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 64.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("🎙", style = MaterialTheme.typography.displayLarge)
                        Text("No recordings yet", style = MaterialTheme.typography.headlineSmall, color = EchoTextPrimary)
                        Text(
                            "Say your wake keyword to start recording",
                            style = MaterialTheme.typography.bodyMedium,
                            color = EchoTextSecondary
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
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = EchoSurface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    TimeUtils.formatDate(summary.dateEpochMillis),
                    style = MaterialTheme.typography.titleLarge,
                    color = EchoTextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "${summary.recordingCount} recordings",
                    style = MaterialTheme.typography.labelMedium,
                    color = EchoAccent
                )
            }
            Text(
                summary.summaryText,
                style = MaterialTheme.typography.bodyMedium,
                color = EchoTextSecondary,
                maxLines = 4
            )
        }
    }
}

@Composable
private fun RecordingListCard(recording: RecordingEntity, onClick: () -> Unit) {
    val (statusColor) = when (recording.transcriptionStatus) {
        TranscriptionStatus.PENDING    -> listOf(EchoAmber)
        TranscriptionStatus.PROCESSING -> listOf(EchoAccent)
        TranscriptionStatus.COMPLETED  -> listOf(EchoGreen)
        TranscriptionStatus.FAILED     -> listOf(EchoRed)
        TranscriptionStatus.SKIPPED    -> listOf(EchoTextTertiary)
    }
    val snippet = recording.transcriptText?.take(70)?.let { "\"$it…\"" } ?: "No transcript"
    val duration = TimeUtils.formatDuration(recording.durationMillis)
    val time = TimeUtils.formatEpoch(recording.createdAtEpochMillis)

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = EchoSurface),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Waveform thumbnail
            WaveformBars(
                modifier = Modifier.width(52.dp).height(36.dp),
                seed = recording.id,
                barCount = 16,
                color = statusColor
            )

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(time, style = MaterialTheme.typography.titleSmall, color = EchoTextPrimary)
                    Text(duration, style = MaterialTheme.typography.bodySmall, color = EchoTextSecondary)
                }
                Text(
                    snippet,
                    style = MaterialTheme.typography.bodySmall,
                    color = EchoTextSecondary,
                    maxLines = 1
                )
            }
        }
    }
}
