package com.example.keywordrecorder.ui.detail

import android.media.MediaPlayer
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
import com.example.keywordrecorder.ui.theme.*
import com.example.keywordrecorder.util.TimeUtils

@Composable
fun RecordingDetailScreen(
    recordingId: Long,
    vm: RecordingDetailViewModel = viewModel()
) {
    LaunchedEffect(recordingId) { vm.load(recordingId) }
    val recording by vm.recording.collectAsState()

    var showDeleteDialog by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize(), color = TermBg) {
        when (val rec = recording) {
            null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("LOADING...", color = TermTextDim, style = MaterialTheme.typography.bodyMedium)
                }
            }
            else -> {
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
                            "RECORDING DETAIL",
                            color = TermCyan,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "#${rec.id}",
                            color = TermTextDim,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    TermDivider()

                    // Metadata panel
                    TermPanel(title = "METADATA") {
                        TermPrompt("DATE", TimeUtils.formatFull(rec.createdAtEpochMillis))
                        Spacer(modifier = Modifier.height(5.dp))
                        TermPrompt("DURATION", TimeUtils.formatDuration(rec.durationMillis))
                        Spacer(modifier = Modifier.height(5.dp))
                        val (statusLabel, statusColor) = when (rec.transcriptionStatus.name) {
                            "COMPLETED" -> "COMPLETED" to TermGreen
                            "FAILED"    -> "FAILED"    to TermRed
                            "PENDING"   -> "PENDING"   to TermYellow
                            "PROCESSING"-> "PROCESSING"to TermCyan
                            else        -> rec.transcriptionStatus.name to TermTextDim
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(">", color = TermCyan, style = MaterialTheme.typography.bodyMedium)
                            Text("STATUS          ", color = MaterialTheme.colorScheme.outlineVariant, style = MaterialTheme.typography.bodyMedium)
                            Text("●", color = statusColor, style = MaterialTheme.typography.bodyMedium)
                            Text(statusLabel, color = statusColor, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                        rec.lastErrorMessage?.let { err ->
                            Spacer(modifier = Modifier.height(5.dp))
                            TermPrompt("ERROR", err, valueColor = TermPink)
                        }
                    }

                    // Transcript panel
                    TermPanel(title = "TRANSCRIPT") {
                        if (rec.transcriptText.isNullOrBlank()) {
                            Text(
                                "[ no transcript ]",
                                color = TermTextDim,
                                style = MaterialTheme.typography.bodySmall
                            )
                        } else {
                            Text(
                                rec.transcriptText,
                                color = TermTextNormal,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    // Actions panel
                    TermPanel(title = "ACTIONS") {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TermButton(
                                label = "PLAY",
                                color = TermCyan,
                                onClick = {
                                    // Note: MediaPlayer not released — known issue
                                    val player = MediaPlayer()
                                    player.setDataSource(rec.filePath)
                                    player.prepare()
                                    player.start()
                                }
                            )
                            TermButton(
                                label = "RETRANSCRIBE",
                                color = TermYellow,
                                onClick = { vm.retranscribe(rec.id) }
                            )
                            TermButton(
                                label = "DELETE",
                                color = TermRed,
                                onClick = { showDeleteDialog = true }
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            TermKeyHint("PLAY", "play audio")
                            TermKeyHint("RTX", "re-run transcription")
                            TermKeyHint("DEL", "delete permanently")
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = TermSurface,
            title = {
                Text("[ CONFIRM DELETE ]", color = TermRed, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "This will permanently remove the audio file and transcript. This action cannot be undone.",
                    color = TermTextNormal,
                    style = MaterialTheme.typography.bodySmall
                )
            },
            confirmButton = {
                TermButton(
                    label = "CONFIRM DELETE",
                    color = TermRed,
                    onClick = {
                        showDeleteDialog = false
                        recording?.let { vm.delete(it.id) {} }
                    }
                )
            },
            dismissButton = {
                TermButton(
                    label = "CANCEL",
                    color = TermTextDim,
                    onClick = { showDeleteDialog = false }
                )
            }
        )
    }
}
