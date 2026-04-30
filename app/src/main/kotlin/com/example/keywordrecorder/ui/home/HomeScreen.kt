package com.example.keywordrecorder.ui.home

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.keywordrecorder.audio.ModelState
import com.example.keywordrecorder.service.ListenerState

@Composable
fun HomeScreen(vm: HomeViewModel = viewModel()) {
    val listenerState by vm.listenerState.collectAsStateWithLifecycle()
    val modelState by vm.modelState.collectAsStateWithLifecycle()

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            MicButton(listenerState)

            Spacer(Modifier.height(24.dp))

            Text(
                text = listenerState.label(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            if (modelState is ModelState.Downloading) {
                Spacer(Modifier.height(16.dp))
                val percent = (modelState as ModelState.Downloading).percent
                Text("Downloading model… $percent%", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(progress = { percent / 100f })
            } else if (modelState is ModelState.Extracting) {
                Spacer(Modifier.height(16.dp))
                Text("Extracting model…", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator()
            }
        }
    }
}

@Composable
private fun MicButton(state: ListenerState) {
    val active = state == ListenerState.LISTENING || state == ListenerState.RECORDING
    val infinite = rememberInfiniteTransition(label = "mic_pulse")
    val scale by infinite.animateFloat(
        initialValue = 1f,
        targetValue = if (active) 1.15f else 1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "scale",
    )

    Box(contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier
                .size(120.dp)
                .scale(scale),
            shape = CircleShape,
            color = if (active) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 4.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Microphone",
                    modifier = Modifier.size(56.dp),
                    tint = if (active) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun ListenerState.label() = when (this) {
    ListenerState.IDLE -> "Service stopped"
    ListenerState.LISTENING -> "Listening for wake word…"
    ListenerState.WAKE_WORD_DETECTED -> "Wake word detected!"
    ListenerState.RECORDING -> "Recording…"
}
