package com.example.keywordrecorder.ui.detail

import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun RecordingDetailScreen(
    recordingId: Long,
    onBack: () -> Unit,
    vm: RecordingDetailViewModel = viewModel()
) {
    LaunchedEffect(recordingId) { vm.load(recordingId) }
    val recording by vm.recording.collectAsStateWithLifecycle()

    // If the recording is deleted externally while this screen is open, go back.
    val hasLoaded = remember { mutableStateOf(false) }
    LaunchedEffect(recording) {
        if (recording != null) hasLoaded.value = true
        else if (hasLoaded.value) onBack()
    }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var playbackProgress by remember { mutableFloatStateOf(0f) }
    var isPlaying by remember { mutableStateOf(false) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            player?.stop()
            player?.release()
            player = null
        }
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying && player != null) {
            while (isActive && isPlaying) {
                try {
                    val mp = player ?: break
                    val duration = mp.duration.takeIf { it > 0 } ?: break
                    playbackProgress = mp.currentPosition.toFloat() / duration
                    if (!mp.isPlaying) {
                        isPlaying = false
                        playbackProgress = 0f
                    }
                } catch (_: IllegalStateException) {
                    isPlaying = false
                    break
                }
                delay(200)
            }
        }
    }

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
                            .padding(horizontal = 20.dp)
                            .semantics { contentDescription = "Audio waveform" },
                        seed = rec.id,
                        barCount = 60,
                        color = EchoWaveActive
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Scrubber
                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        Slider(
                            value = playbackProgress,
                            onValueChange = { value ->
                                playbackProgress = value
                                player?.let { mp ->
                                    val seekPos = (mp.duration * value).toInt()
                                    mp.seekTo(seekPos)
                                }
                            },
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
                        onSkipBack = {
                            player?.let { mp ->
                                val newPos = (mp.currentPosition - 5000).coerceAtLeast(0)
                                mp.seekTo(newPos)
                                playbackProgress = newPos.toFloat() / mp.duration.coerceAtLeast(1)
                            }
                        },
                        onPlayPause = {
                            val currentPlayer = player
                            if (currentPlayer != null && isPlaying) {
                                currentPlayer.pause()
                                isPlaying = false
                            } else if (currentPlayer != null && !isPlaying) {
                                currentPlayer.start()
                                isPlaying = true
                            } else {
                                try {
                                    val mp = MediaPlayer().apply {
                                        setDataSource(rec.filePath)
                                        prepare()
                                        start()
                                        setOnCompletionListener {
                                            isPlaying = false
                                            playbackProgress = 0f
                                        }
                                    }
                                    player = mp
                                    isPlaying = true
                                } catch (_: Exception) {
                                    isPlaying = false
                                }
                            }
                        },
                        onSkipForward = {
                            player?.let { mp ->
                                val newPos = (mp.currentPosition + 5000).coerceAtMost(mp.duration)
                                mp.seekTo(newPos)
                                playbackProgress = newPos.toFloat() / mp.duration.coerceAtLeast(1)
                            }
                        },
                        onStop = {
                            player?.stop()
                            player?.release()
                            player = null
                            isPlaying = false
                            playbackProgress = 0f
                        }
                    )

                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = EchoBorder, modifier = Modifier.padding(horizontal = 20.dp))
                    Spacer(modifier = Modifier.height(16.dp))

                    // Transcript section
                    TranscriptSection(rec = rec, onRetranscribe = { vm.retranscribe(rec.id) })

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
                        player?.stop()
                        player?.release()
                        player = null
                        isPlaying = false
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
            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = EchoRed)
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
        // Skip back
        IconButton(
            onClick = onSkipBack,
            modifier = Modifier
                .size(48.dp)
                .background(EchoSurfaceHigh, CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.SkipPrevious,
                contentDescription = "Skip back 5 seconds",
                tint = EchoTextSecondary,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Big play button
        Surface(
            onClick = onPlayPause,
            modifier = Modifier
                .size(64.dp)
                .semantics {
                    contentDescription = if (isPlaying) "Pause playback" else "Play recording"
                },
            shape = CircleShape,
            color = EchoAccent
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Skip forward
        IconButton(
            onClick = onSkipForward,
            modifier = Modifier
                .size(48.dp)
                .background(EchoSurfaceHigh, CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.SkipNext,
                contentDescription = "Skip forward 5 seconds",
                tint = EchoTextSecondary,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Stop / close
        IconButton(
            onClick = onStop,
            modifier = Modifier
                .size(48.dp)
                .background(EchoSurfaceHigh, CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Stop playback",
                tint = EchoTextSecondary,
                modifier = Modifier.size(24.dp)
            )
        }
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

private fun buildDetailTitle(rec: RecordingEntity): String {
    return "Note — ${TimeUtils.formatDateShort(rec.createdAtEpochMillis)}"
}

private fun buildMetaLine(rec: RecordingEntity): String {
    val date = TimeUtils.formatDate(rec.createdAtEpochMillis)
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
    val words = text.split(" ")
    var boldCount = 0
    var boldWordsLeft = 0

    words.forEachIndexed { idx, word ->
        if (boldWordsLeft > 0) {
            withStyle(SpanStyle(color = EchoTextPrimary, fontWeight = FontWeight.Bold)) {
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
