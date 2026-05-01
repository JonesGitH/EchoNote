package com.example.keywordrecorder.ui.detail

import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.keywordrecorder.data.RecordingEntity
import com.example.keywordrecorder.data.TranscriptionStatus
import com.example.keywordrecorder.ui.home.WaveformBars
import com.example.keywordrecorder.ui.theme.*
import com.example.keywordrecorder.util.TimeUtils
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RecordingDetailScreen(
    recordingId: Long,
    onBack: () -> Unit,
    vm: RecordingDetailViewModel = viewModel()
) {
    LaunchedEffect(recordingId) { vm.load(recordingId) }
    val recording by vm.recording.collectAsStateWithLifecycle()

    var showDeleteDialog by remember { mutableStateOf(false) }
    var playbackProgress by remember { mutableFloatStateOf(0.35f) }
    var isPlaying by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize(), color = EchoBg) {
        when (val rec = recording) {
            null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = EchoAccent, modifier = Modifier.size(32.dp))
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Top bar
                    DetailTopBar(
                        title = buildDetailTitle(rec),
                        onBack = onBack,
                        onDelete = { showDeleteDialog = true }
                    )

                    // Metadata line
                    Text(
                        text = buildMetaLine(rec),
                        style = MaterialTheme.typography.bodySmall,
                        color = EchoTextSecondary,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Waveform
                    WaveformBars(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp)
                            .padding(horizontal = 20.dp),
                        seed = rec.id,
                        barCount = 60,
                        color = EchoWaveActive
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Scrubber
                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        Slider(
                            value = playbackProgress,
                            onValueChange = { playbackProgress = it },
                            modifier = Modifier.fillMaxWidth(),
                            colors = SliderDefaults.colors(
                                thumbColor = EchoAccent,
                                activeTrackColor = EchoAccent,
                                inactiveTrackColor = EchoWaveInactive
                            )
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val totalMs = rec.durationMillis
                            val currentMs = (totalMs * playbackProgress).toLong()
                            Text(formatMs(currentMs), color = EchoTextSecondary, style = MaterialTheme.typography.bodySmall)
                            Text(formatMs(totalMs), color = EchoTextSecondary, style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Playback controls
                    PlaybackControls(
                        isPlaying = isPlaying,
                        onSkipBack = { playbackProgress = (playbackProgress - 0.1f).coerceAtLeast(0f) },
                        onPlayPause = {
                            isPlaying = !isPlaying
                            if (isPlaying) {
                                try {
                                    val player = MediaPlayer()
                                    player.setDataSource(rec.filePath)
                                    player.prepare()
                                    player.start()
                                } catch (_: Exception) {}
                            }
                        },
                        onSkipForward = { playbackProgress = (playbackProgress + 0.1f).coerceAtMost(1f) },
                        onStop = { isPlaying = false; playbackProgress = 0f }
                    )

                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = EchoBorder, modifier = Modifier.padding(horizontal = 20.dp))
                    Spacer(modifier = Modifier.height(16.dp))

                    // Transcript section
                    TranscriptSection(rec = rec, onRetranscribe = { vm.retranscribe(rec.id) })

                    Spacer(modifier = Modifier.height(20.dp))

                    // AI action chips
                    ActionChipRow(modifier = Modifier.padding(horizontal = 20.dp))

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = EchoSurface,
            shape = RoundedCornerShape(16.dp),
            title = {
                Text("Delete recording?", color = EchoTextPrimary, style = MaterialTheme.typography.headlineSmall)
            },
            text = {
                Text(
                    "This will permanently remove the audio file and transcript. This action cannot be undone.",
                    color = EchoTextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        recording?.let { vm.delete(it.id) { onBack() } }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EchoRed)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = EchoTextSecondary)
                }
            }
        )
    }
}

@Composable
private fun DetailTopBar(title: String, onBack: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = EchoTextPrimary)
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = EchoTextPrimary,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.MoreVert, contentDescription = "More", tint = EchoTextSecondary)
        }
    }
}

@Composable
private fun PlaybackControls(
    isPlaying: Boolean,
    onSkipBack: () -> Unit,
    onPlayPause: () -> Unit,
    onSkipForward: () -> Unit,
    onStop: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(8.dp))

        // Skip back
        IconButton(onClick = onSkipBack, modifier = Modifier.size(44.dp)) {
            Text("‹", fontSize = 24.sp, color = EchoTextSecondary)
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Pause / Resume
        IconButton(
            onClick = onPlayPause,
            modifier = Modifier
                .size(44.dp)
                .background(EchoSurfaceHigh, CircleShape)
        ) {
            Text(if (isPlaying) "⏸" else "⏸", fontSize = 18.sp, color = EchoTextPrimary)
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Big play button
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(EchoAccent, CircleShape)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(if (isPlaying) "⏸" else "▶", fontSize = 22.sp, color = Color.White)
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Skip forward
        IconButton(
            onClick = onSkipForward,
            modifier = Modifier
                .size(44.dp)
                .background(EchoSurfaceHigh, CircleShape)
        ) {
            Text("›", fontSize = 24.sp, color = EchoTextSecondary)
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Stop / close
        IconButton(onClick = onStop, modifier = Modifier.size(44.dp)) {
            Text("✕", fontSize = 18.sp, color = EchoTextSecondary)
        }

        Spacer(modifier = Modifier.width(8.dp))
    }
}

@Composable
private fun TranscriptSection(rec: RecordingEntity, onRetranscribe: () -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "TRANSCRIPT",
                style = MaterialTheme.typography.labelMedium,
                color = EchoTextSecondary,
                letterSpacing = 1.5.sp
            )
            val (statusLabel, statusColor) = when (rec.transcriptionStatus) {
                TranscriptionStatus.COMPLETED  -> "Auto-transcribed" to EchoGreen
                TranscriptionStatus.PENDING    -> "Pending" to EchoAmber
                TranscriptionStatus.PROCESSING -> "Processing…" to EchoAccent
                TranscriptionStatus.FAILED     -> "Failed" to EchoRed
                TranscriptionStatus.SKIPPED    -> "Skipped" to EchoTextTertiary
            }
            Text(statusLabel, style = MaterialTheme.typography.labelSmall, color = statusColor)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = EchoSurface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                when {
                    rec.transcriptText.isNullOrBlank() && rec.transcriptionStatus == TranscriptionStatus.FAILED -> {
                        Text(
                            "Transcription failed. ${rec.lastErrorMessage ?: ""}",
                            color = EchoRed,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        TextButton(onClick = onRetranscribe) {
                            Text("Try again", color = EchoAccent)
                        }
                    }
                    rec.transcriptText.isNullOrBlank() -> {
                        Text(
                            "No transcript yet.",
                            color = EchoTextTertiary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (rec.transcriptionStatus == TranscriptionStatus.PENDING) {
                            Spacer(modifier = Modifier.height(12.dp))
                            TextButton(onClick = onRetranscribe) {
                                Text("Transcribe now", color = EchoAccent)
                            }
                        }
                    }
                    else -> {
                        Text(
                            text = buildHighlightedTranscript(rec.transcriptText!!),
                            style = MaterialTheme.typography.bodyLarge,
                            color = EchoTextSecondary,
                            lineHeight = 26.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionChipRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Summarize — filled primary chip
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = EchoAccentDim,
            modifier = Modifier
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text("✦", fontSize = 12.sp, color = EchoAccent)
                Text("Summarize", style = MaterialTheme.typography.labelLarge, color = EchoAccent, fontWeight = FontWeight.SemiBold)
            }
        }

        // Action items
        OutlinedActionChip(label = "Action items")

        // Translate
        OutlinedActionChip(label = "Translate")
    }
}

@Composable
private fun OutlinedActionChip(label: String) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, EchoBorder)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = EchoTextSecondary,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
        )
    }
}

private fun buildDetailTitle(rec: RecordingEntity): String {
    val dateStr = SimpleDateFormat("MMM d", Locale.US).format(Date(rec.createdAtEpochMillis))
    return "Note — $dateStr"
}

private fun buildMetaLine(rec: RecordingEntity): String {
    val date = SimpleDateFormat("MMMM d, yyyy", Locale.US).format(Date(rec.createdAtEpochMillis))
    val duration = TimeUtils.formatDuration(rec.durationMillis)
    val status = when (rec.transcriptionStatus) {
        TranscriptionStatus.COMPLETED -> "Auto-transcribed"
        TranscriptionStatus.PENDING   -> "Pending transcription"
        TranscriptionStatus.PROCESSING -> "Transcribing…"
        TranscriptionStatus.FAILED    -> "Transcription failed"
        TranscriptionStatus.SKIPPED   -> ""
    }
    return if (status.isNotEmpty()) "$date · $duration · $status" else "$date · $duration"
}

private fun formatMs(ms: Long): String {
    val totalSec = ms / 1000
    val m = totalSec / 60
    val s = totalSec % 60
    return "%d:%02d".format(m, s)
}

private fun buildHighlightedTranscript(text: String) = buildAnnotatedString {
    // Bold every ~5th–8th word to simulate key phrase highlighting
    val words = text.split(" ")
    var boldCount = 0
    var inBold = false
    var boldWordsLeft = 0

    words.forEachIndexed { idx, word ->
        if (boldWordsLeft > 0) {
            withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.Bold)) {
                append(word)
            }
            boldWordsLeft--
        } else {
            append(word)
            boldCount++
            if (boldCount >= 5 + (idx % 4)) {
                boldCount = 0
                boldWordsLeft = 2 + (idx % 3)
            }
        }
        if (idx < words.lastIndex) append(" ")
    }
}
