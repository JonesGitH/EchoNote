package com.example.keywordrecorder.ui.recordings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import kotlinx.coroutines.launch

@Composable
fun RecordingsScreen(
    onOpenDetail: (Long) -> Unit,
    vm: RecordingsViewModel = viewModel()
) {
    val recordings by vm.recordings.collectAsStateWithLifecycle()
    val summaries by vm.summaries.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showDeleteAllDialog by remember { mutableStateOf(false) }

    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            containerColor = EchoSurface,
            shape = RoundedCornerShape(16.dp),
            title = { Text("Delete all recordings?", color = EchoTextPrimary, style = MaterialTheme.typography.headlineSmall) },
            text = { Text("This will permanently remove all audio files and transcripts.", color = EchoTextSecondary, style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                Button(
                    onClick = { showDeleteAllDialog = false; vm.deleteAllRecordings() },
                    colors = ButtonDefaults.buttonColors(containerColor = EchoRed)
                ) { Text("Delete All", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text("Cancel", color = EchoTextSecondary)
                }
            }
        )
    }

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
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "${recordings.size} notes",
                            style = MaterialTheme.typography.bodyMedium,
                            color = EchoTextSecondary
                        )
                        if (recordings.isNotEmpty()) {
                            IconButton(onClick = { showDeleteAllDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete all recordings",
                                    tint = EchoTextTertiary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
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
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value == SwipeToDismissBoxValue.EndToStart) {
                                val filePath = rec.filePath
                                vm.softDeleteRecording(rec.id)
                                scope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = "Recording deleted",
                                        actionLabel = "Undo",
                                        duration = SnackbarDuration.Short
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        vm.restoreRecording(rec.id)
                                    } else {
                                        vm.deleteRecordingFile(filePath)
                                    }
                                }
                                true
                            } else false
                        }
                    )
                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = false,
                        backgroundContent = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(EchoRed.copy(alpha = 0.85f))
                                    .padding(end = 20.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    ) {
                        RecordingListCard(rec, onClick = { onOpenDetail(rec.id) })
                    }
                }
            }

            if (recordings.isEmpty() && summaries.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 64.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = EchoAccent,
                            modifier = Modifier.size(48.dp)
                        )
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
    val snippet = recording.transcriptText?.let { t ->
        if (t.length > 70) "\"${t.take(70)}…\"" else "\"$t\""
    } ?: "No transcript"
    val duration = TimeUtils.formatDuration(recording.durationMillis)
    val time = TimeUtils.formatEpoch(recording.createdAtEpochMillis)

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = EchoSurface),
        modifier = Modifier.fillMaxWidth()
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
