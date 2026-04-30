package com.example.keywordrecorder.ui.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.keywordrecorder.audio.ModelState
import com.example.keywordrecorder.service.ListenerState

@Composable
fun HomeScreen(vm: HomeViewModel = viewModel()) {
    val listenerState by vm.listenerState.collectAsState()
    val modelState by vm.modelState.collectAsState()

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (val ms = modelState) {
                is ModelState.Downloading -> {
                    Text("Downloading model…", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { ms.progress / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("${ms.progress}%", style = MaterialTheme.typography.bodySmall)
                }
                is ModelState.Extracting -> {
                    Text("Extracting model…", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                is ModelState.Error -> {
                    Text("Model error: ${ms.message}", color = MaterialTheme.colorScheme.error)
                }
                else -> {
                    PulsingMic(listenerState)
                    Spacer(modifier = Modifier.height(24.dp))
                    ListenerStatusText(listenerState)
                }
            }
        }
    }
}

@Composable
private fun PulsingMic(state: ListenerState) {
    val isPulsing = state == ListenerState.LISTENING || state == ListenerState.RECORDING
    val scale by if (isPulsing) {
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = EaseInOut),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )
    } else {
        remember { mutableFloatStateOf(1f) }
    }

    val color = when (state) {
        ListenerState.RECORDING -> MaterialTheme.colorScheme.error
        ListenerState.WAKE_WORD_DETECTED -> MaterialTheme.colorScheme.tertiary
        ListenerState.LISTENING -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline
    }

    Box(
        modifier = Modifier
            .size(120.dp)
            .scale(scale)
            .background(color = color, shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text("🎙", fontSize = 48.sp)
    }
}

@Composable
private fun ListenerStatusText(state: ListenerState) {
    val text = when (state) {
        ListenerState.STOPPED -> "Stopped"
        ListenerState.STARTING -> "Starting…"
        ListenerState.LISTENING -> "Listening for wake word"
        ListenerState.WAKE_WORD_DETECTED -> "Wake word detected!"
        ListenerState.RECORDING -> "Recording…"
        ListenerState.ERROR -> "Error — check settings"
    }
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Medium
    )
}
