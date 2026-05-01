package com.example.keywordrecorder.ui.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.keywordrecorder.audio.ModelState
import com.example.keywordrecorder.data.RecordingEntity
import com.example.keywordrecorder.data.TranscriptionStatus
import com.example.keywordrecorder.service.ListenerState
import com.example.keywordrecorder.ui.recordings.RecordingsViewModel
import com.example.keywordrecorder.ui.theme.*
import com.example.keywordrecorder.util.TimeUtils
import java.util.Calendar
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun HomeScreen(
    homeVm: HomeViewModel = viewModel(),
    recordingsVm: RecordingsViewModel = viewModel()
) {
    val listenerState by homeVm.listenerState.collectAsStateWithLifecycle()
    val modelState by homeVm.modelState.collectAsStateWithLifecycle()
    val recordings by recordingsVm.recordings.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("All", "Recent", "Starred")

    val filteredRecordings = when (selectedTab) {
        1 -> recordings.filter { rec ->
            val cal = Calendar.getInstance()
            val recCal = Calendar.getInstance().apply { timeInMillis = rec.createdAtEpochMillis }
            cal.get(Calendar.DAY_OF_YEAR) == recCal.get(Calendar.DAY_OF_YEAR) &&
                cal.get(Calendar.YEAR) == recCal.get(Calendar.YEAR)
        }
        2 -> emptyList()
        else -> recordings
    }

    Surface(modifier = Modifier.fillMaxSize(), color = EchoBg) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                EchoWordmark()
                IconButton(onClick = {}) {
                    Text("⋯", color = EchoTextSecondary, fontSize = 20.sp)
                }
            }

            // Model status (only shown when not ready)
            when (val ms = modelState) {
                is ModelState.Downloading -> ModelStatusBanner(
                    label = "Downloading voice model… ${ms.progress}%",
                    color = EchoAmber
                )
                is ModelState.Extracting -> ModelStatusBanner(
                    label = "Preparing voice model…",
                    color = EchoAmber
                )
                is ModelState.Error -> ModelStatusBanner(
                    label = "Model error: ${ms.message}",
                    color = EchoRed
                )
                else -> Unit
            }

            // Filter tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tabs.forEachIndexed { idx, label ->
                    FilterTab(
                        label = label,
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Recording list
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (filteredRecordings.isEmpty()) {
                    item {
                        EmptyState(tab = tabs[selectedTab])
                    }
                } else {
                    items(filteredRecordings, key = { it.id }) { rec ->
                        RecordingCard(recording = rec)
                    }
                }
                item { Spacer(modifier = Modifier.height(8.dp)) }
            }

            // Bottom recording control panel
            RecordingPanel(state = listenerState)
        }
    }
}

@Composable
private fun EchoWordmark() {
    val wordmark = buildAnnotatedString {
        withStyle(SpanStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 26.sp, color = EchoTextPrimary)) {
            append("Echo")
        }
        withStyle(SpanStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 26.sp, color = EchoAccent)) {
            append(".")
        }
        withStyle(SpanStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 26.sp, color = EchoTextPrimary)) {
            append("Notes")
        }
    }
    Text(text = wordmark)
}

@Composable
private fun ModelStatusBanner(label: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(modifier = Modifier.size(6.dp).background(color, CircleShape))
        Text(label, color = color, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun FilterTab(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) EchoAccentDim else Color.Transparent
    val textColor = if (selected) EchoAccent else EchoTextSecondary
    val weight = if (selected) FontWeight.SemiBold else FontWeight.Normal

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 7.dp)
    ) {
        Text(label, color = textColor, style = MaterialTheme.typography.labelLarge, fontWeight = weight)
    }
}

@Composable
private fun RecordingCard(recording: RecordingEntity) {
    val title = buildCardTitle(recording)
    val timeLabel = buildTimeLabel(recording.createdAtEpochMillis)
    val snippet = recording.transcriptText?.take(80)?.let { "\"$it…\"" } ?: "Tap to transcribe"
    val duration = TimeUtils.formatDuration(recording.durationMillis)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = EchoSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = EchoTextPrimary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = timeLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = EchoTextSecondary
                )
            }

            Text(
                text = snippet,
                style = MaterialTheme.typography.bodyMedium,
                color = EchoTextSecondary,
                maxLines = 2
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                WaveformBars(
                    modifier = Modifier
                        .weight(1f)
                        .height(24.dp)
                        .padding(end = 12.dp),
                    seed = recording.id,
                    color = EchoWaveActive.copy(alpha = 0.7f)
                )

                val (statusColor, statusLabel) = when (recording.transcriptionStatus) {
                    TranscriptionStatus.COMPLETED  -> EchoGreen to null
                    TranscriptionStatus.PENDING    -> EchoAmber to "pending"
                    TranscriptionStatus.PROCESSING -> EchoAccent to "processing"
                    TranscriptionStatus.FAILED     -> EchoRed to "failed"
                    TranscriptionStatus.SKIPPED    -> EchoTextTertiary to null
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (statusLabel != null) {
                        Text(statusLabel, color = statusColor, style = MaterialTheme.typography.labelSmall)
                    }
                    Text(duration, color = EchoTextSecondary, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun EmptyState(tab: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("🎙", fontSize = 40.sp)
        Text(
            text = if (tab == "Starred") "No starred notes yet" else "No recordings yet",
            style = MaterialTheme.typography.headlineSmall,
            color = EchoTextPrimary
        )
        Text(
            text = if (tab == "Starred") "Tap ★ on any note to star it"
                   else "Tap the record button or say your wake keyword",
            style = MaterialTheme.typography.bodyMedium,
            color = EchoTextSecondary
        )
    }
}

@Composable
private fun RecordingPanel(state: ListenerState) {
    val isRecording = state == ListenerState.RECORDING
    val isListening = state == ListenerState.LISTENING || state == ListenerState.WAKE_WORD_DETECTED

    val pulseAnim = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulseAnim.animateFloat(
        initialValue = 1f,
        targetValue = if (isRecording) 1.12f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Surface(
        color = EchoSurface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Waveform display
            WaveformBars(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                seed = System.currentTimeMillis() / 500,
                animated = isRecording,
                barCount = 48,
                color = if (isRecording) EchoAccent else EchoWaveInactive
            )

            // Controls row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Stop / pause
                IconButton(
                    onClick = {},
                    modifier = Modifier
                        .size(48.dp)
                        .background(EchoSurfaceHigh, CircleShape)
                ) {
                    Text(
                        if (isRecording) "⏸" else "⏸",
                        fontSize = 20.sp,
                        color = if (isRecording) EchoTextPrimary else EchoTextTertiary
                    )
                }

                // Big record button
                Box(
                    modifier = Modifier
                        .size((64 * pulseScale).dp)
                        .background(EchoAccent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(Color.White, if (isRecording) RoundedCornerShape(4.dp) else CircleShape)
                    )
                }

                // Recordings list / transcript toggle
                IconButton(
                    onClick = {},
                    modifier = Modifier
                        .size(48.dp)
                        .background(EchoSurfaceHigh, CircleShape)
                ) {
                    Text("≡", fontSize = 20.sp, color = EchoTextSecondary)
                }
            }

            // Status label
            Text(
                text = when (state) {
                    ListenerState.STOPPED           -> "00:00"
                    ListenerState.STARTING          -> "Starting…"
                    ListenerState.LISTENING         -> "Listening…"
                    ListenerState.WAKE_WORD_DETECTED -> "Wake word!"
                    ListenerState.RECORDING         -> "Recording"
                    ListenerState.ERROR             -> "Error"
                },
                style = MaterialTheme.typography.labelLarge,
                color = if (isRecording) EchoAccent else EchoTextTertiary
            )
        }
    }
}

@Composable
fun WaveformBars(
    modifier: Modifier = Modifier,
    seed: Long = 0L,
    animated: Boolean = false,
    barCount: Int = 32,
    color: Color = EchoWaveActive
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (animated) (2 * PI).toFloat() else 0f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)),
        label = "phase"
    )

    Canvas(modifier = modifier) {
        val rng = java.util.Random(seed)
        val baseHeights = FloatArray(barCount) { i ->
            val envelope = sin(PI * i.toDouble() / barCount).toFloat()
            val noise = rng.nextFloat() * 0.45f
            (envelope * 0.55f + noise + 0.1f).coerceIn(0.08f, 1f)
        }

        val gap = size.width / (barCount * 2f - 1f)
        val barW = gap

        for (i in 0 until barCount) {
            val animMod = if (animated) {
                val wave = sin(phase + i * 0.4f).toFloat()
                1f + wave * 0.3f
            } else 1f

            val h = (baseHeights[i] * animMod).coerceIn(0.05f, 1f) * size.height
            val x = i * (barW + gap)
            val y = (size.height - h) / 2f

            drawRoundRect(
                color = color,
                topLeft = Offset(x, y),
                size = Size(barW, h),
                cornerRadius = CornerRadius(barW / 2f)
            )
        }
    }
}

private fun buildCardTitle(rec: RecordingEntity): String {
    val date = TimeUtils.formatDate(rec.createdAtEpochMillis)
    return "Note — $date"
}

private fun buildTimeLabel(epochMillis: Long): String {
    val now = System.currentTimeMillis()
    val diffMs = now - epochMillis
    val diffDays = (diffMs / 86_400_000L).toInt()
    return when {
        diffDays == 0 -> TimeUtils.formatEpoch(epochMillis)
        diffDays == 1 -> "Yesterday"
        diffDays < 7  -> "$diffDays days ago"
        else          -> TimeUtils.formatDate(epochMillis)
    }
}
