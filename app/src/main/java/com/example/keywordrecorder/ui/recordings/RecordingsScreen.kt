package com.example.keywordrecorder.ui.recordings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.keywordrecorder.data.DailySummaryEntity
import com.example.keywordrecorder.data.RecordingEntity
import com.example.keywordrecorder.data.TranscriptionStatus
import com.example.keywordrecorder.ui.theme.*
import com.example.keywordrecorder.util.TimeUtils

@Composable
fun RecordingsScreen(
    onOpenDetail: (Long) -> Unit,
    vm: RecordingsViewModel = viewModel()
) {
    val recordings by vm.recordings.collectAsState()
    val summaries by vm.summaries.collectAsState()

    Surface(modifier = Modifier.fillMaxSize(), color = TermBg) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                // Screen header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "RECORDINGS",
                        color = TermCyan,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${recordings.size} rec  ${summaries.size} sum",
                        color = TermTextDim,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                TermDivider(modifier = Modifier.padding(top = 6.dp))
            }

            if (summaries.isNotEmpty()) {
                item {
                    Text(
                        "─[ DAILY SUMMARIES ]",
                        color = TermPurple,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                items(summaries, key = { it.id }) { summary ->
                    SummaryRow(summary)
                }
            }

            if (recordings.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "─[ INDIVIDUAL RECORDINGS ]",
                        color = TermPurple,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    // Column header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(TermSurfaceAlt)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("TIMESTAMP          ", color = TermTextDim, style = MaterialTheme.typography.labelSmall)
                        Text("DUR    ", color = TermTextDim, style = MaterialTheme.typography.labelSmall)
                        Text("STATUS    ", color = TermTextDim, style = MaterialTheme.typography.labelSmall)
                        Text("TRANSCRIPT", color = TermTextDim, style = MaterialTheme.typography.labelSmall)
                    }
                }
                items(recordings, key = { it.id }) { rec ->
                    RecordingRow(rec, onClick = { onOpenDetail(rec.id) })
                }
            }

            if (summaries.isEmpty() && recordings.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, TermBorder)
                            .background(TermSurface)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("[ NO DATA ]", color = TermTextDim, style = MaterialTheme.typography.titleSmall)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Say your wake keyword to start recording.",
                                color = TermTextDim,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(summary: DailySummaryEntity) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, TermBorder)
            .background(TermSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(TermSurfaceAlt)
                .padding(horizontal = 10.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                TimeUtils.formatDate(summary.dateEpochMillis),
                color = TermCyan,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                "${summary.recordingCount} recordings → Downloads",
                color = TermGreen,
                style = MaterialTheme.typography.labelSmall
            )
        }
        Text(
            summary.summaryText,
            color = TermTextNormal,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            maxLines = 5
        )
    }
}

@Composable
private fun RecordingRow(recording: RecordingEntity, onClick: () -> Unit) {
    val (statusLabel, statusColor) = when (recording.transcriptionStatus) {
        TranscriptionStatus.PENDING    -> "PENDING"    to TermYellow
        TranscriptionStatus.PROCESSING -> "RUNNING"    to TermCyan
        TranscriptionStatus.COMPLETED  -> "DONE"       to TermGreen
        TranscriptionStatus.FAILED     -> "FAILED"     to TermRed
        TranscriptionStatus.SKIPPED    -> "SKIPPED"    to TermTextDim
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, TermBorder)
            .background(TermSurface)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            TimeUtils.formatFull(recording.createdAtEpochMillis),
            color = TermTextNormal,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(0f).widthIn(min = 140.dp)
        )
        Text(
            TimeUtils.formatDuration(recording.durationMillis),
            color = TermTextDim,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(48.dp)
        )
        Row(
            modifier = Modifier.width(80.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("●", color = statusColor, style = MaterialTheme.typography.labelSmall)
            Text(statusLabel, color = statusColor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
        Text(
            recording.transcriptText ?: "—",
            color = TermTextDim,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
    }
}
