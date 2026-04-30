package com.example.keywordrecorder.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.keywordrecorder.R
import com.example.keywordrecorder.audio.ModelState
import com.example.keywordrecorder.service.ListenerState
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(viewModel: HomeViewModel = viewModel()) {
    val listenerState by viewModel.listenerState.collectAsState()
    val modelState by viewModel.modelState.collectAsState()

    val isListening = listenerState == ListenerState.LISTENING
    val isRecording = listenerState == ListenerState.RECORDING
    val isActive = isListening || listenerState == ListenerState.WAKE_WORD_DETECTED || isRecording

    var elapsedSeconds by remember { mutableStateOf(0) }
    LaunchedEffect(isRecording) {
        elapsedSeconds = 0
        if (isRecording) {
            while (true) {
                delay(1000L)
                elapsedSeconds++
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.home_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.home_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
                PulsingMicButton(
                    isListening = isListening,
                    isRecording = isRecording,
                    isLoading = listenerState == ListenerState.LOADING_MODEL,
                    onClick = {
                        if (isActive) viewModel.stopListening() else viewModel.startListening()
                    },
                )

                val statusLabel = when (listenerState) {
                    ListenerState.IDLE -> stringResource(R.string.status_idle)
                    ListenerState.LOADING_MODEL -> stringResource(R.string.status_loading_model)
                    ListenerState.LISTENING -> stringResource(R.string.status_listening)
                    ListenerState.WAKE_WORD_DETECTED -> stringResource(R.string.status_wake_word)
                    ListenerState.RECORDING -> stringResource(R.string.status_recording)
                    ListenerState.STOPPING -> stringResource(R.string.status_stopping)
                    ListenerState.ERROR -> stringResource(R.string.status_error)
                }
                val statusColor = when (listenerState) {
                    ListenerState.RECORDING -> MaterialTheme.colorScheme.error
                    ListenerState.LISTENING, ListenerState.WAKE_WORD_DETECTED -> MaterialTheme.colorScheme.primary
                    ListenerState.ERROR -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor,
                    )
                    if (isRecording) {
                        Text(
                            text = "%d:%02d".format(elapsedSeconds / 60, elapsedSeconds % 60),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            ModelStatusBanner(modelState)
        }
    }
}

@Composable
private fun PulsingMicButton(
    isListening: Boolean,
    isRecording: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseScale",
    )

    val buttonColor by animateColorAsState(
        targetValue = when {
            isRecording -> MaterialTheme.colorScheme.error
            isListening -> MaterialTheme.colorScheme.primary
            isLoading -> MaterialTheme.colorScheme.secondary
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        animationSpec = tween(400),
        label = "buttonColor",
    )

    val cdText = if (isRecording) "Stop recording" else "Start listening"

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
        if (isListening || isRecording) {
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .scale(pulseScale)
                    .background(buttonColor.copy(alpha = 0.15f), CircleShape),
            )
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .scale(pulseScale * 0.92f)
                    .background(buttonColor.copy(alpha = 0.10f), CircleShape),
            )
        }
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(buttonColor)
                .semantics { contentDescription = cdText }
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (isRecording) Icons.Rounded.Stop else Icons.Rounded.Mic,
                contentDescription = null,
                tint = if (isListening || isRecording || isLoading)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(44.dp),
            )
        }
    }
}

@Composable
private fun ModelStatusBanner(modelState: ModelState) {
    when (val ms = modelState) {
        is ModelState.Downloading -> Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                stringResource(R.string.model_downloading, ms.progressPercent),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LinearProgressIndicator(
                progress = { ms.progressPercent / 100f },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        is ModelState.Extracting -> Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                stringResource(R.string.model_extracting),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        is ModelState.Error -> Text(
            "⚠ ${ms.message}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
        else -> Spacer(Modifier.height(56.dp))
    }
}
