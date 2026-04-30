package com.example.keywordrecorder.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.keywordrecorder.audio.ModelState
import com.example.keywordrecorder.service.ListenerState
import com.example.keywordrecorder.ui.theme.*

@Composable
fun HomeScreen(vm: HomeViewModel = viewModel()) {
    val listenerState by vm.listenerState.collectAsState()
    val modelState by vm.modelState.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = TermBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // App header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ECHONOTE",
                    color = TermCyan,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "v1.0",
                    color = TermTextDim,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            TermDivider()

            when (val ms = modelState) {
                is ModelState.Downloading -> {
                    TermPanel(title = "MODEL DOWNLOAD") {
                        TermPrompt("STATUS", "DOWNLOADING", valueColor = TermYellow)
                        Spacer(modifier = Modifier.height(12.dp))
                        TermProgressBar(
                            progress = ms.progress / 100f,
                            barWidth = 24,
                            color = TermPurpleBar
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "vosk-model-small-en-us-0.15.zip",
                            color = TermTextDim,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                is ModelState.Extracting -> {
                    TermPanel(title = "MODEL SETUP") {
                        TermPrompt("STATUS", "EXTRACTING...", valueColor = TermYellow)
                        Spacer(modifier = Modifier.height(12.dp))
                        TermProgressBar(progress = 1f, barWidth = 24, color = TermYellow)
                    }
                }
                is ModelState.Error -> {
                    TermPanel(title = "MODEL ERROR") {
                        TermStatusDot(label = "ERROR", color = TermRed)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(ms.message, color = TermPink, style = MaterialTheme.typography.bodySmall)
                    }
                }
                else -> {
                    StatusPanel(listenerState)
                }
            }
        }
    }
}

@Composable
private fun StatusPanel(state: ListenerState) {
    val (statusLabel, statusColor) = when (state) {
        ListenerState.STOPPED           -> "STOPPED"           to TermTextDim
        ListenerState.STARTING          -> "STARTING..."       to TermYellow
        ListenerState.LISTENING         -> "LISTENING"         to TermGreen
        ListenerState.WAKE_WORD_DETECTED-> "WAKE WORD DETECTED"to TermCyan
        ListenerState.RECORDING         -> "RECORDING"         to TermRed
        ListenerState.ERROR             -> "ERROR"             to TermRed
    }

    TermPanel(title = "LISTENER STATUS") {
        TermStatusDot(label = statusLabel, color = statusColor)
    }

    Spacer(modifier = Modifier.height(4.dp))

    TermPanel(title = "SYSTEM INFO") {
        TermPrompt(
            label = "MODEL",
            value = "READY",
            valueColor = TermGreen
        )
        Spacer(modifier = Modifier.height(6.dp))
        TermPrompt(
            label = "SERVICE",
            value = if (state == ListenerState.STOPPED || state == ListenerState.ERROR) "INACTIVE" else "ACTIVE",
            valueColor = if (state == ListenerState.STOPPED || state == ListenerState.ERROR) TermRed else TermGreen
        )
    }

    if (state == ListenerState.RECORDING) {
        Spacer(modifier = Modifier.height(4.dp))
        TermPanel(title = "RECORDING") {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("●", color = TermRed, fontSize = 10.sp)
                Text("AUDIO CAPTURE IN PROGRESS", color = TermRed, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("Recording stops on silence or max duration", color = TermTextDim, style = MaterialTheme.typography.bodySmall)
        }
    }

    Spacer(modifier = Modifier.height(4.dp))
    TermPanel(title = "HINTS") {
        TermKeyHint("SAY", "wake keyword to start recording")
        Spacer(modifier = Modifier.height(4.dp))
        TermKeyHint("TAB", "switch to RECORDINGS or SETTINGS")
    }
}
