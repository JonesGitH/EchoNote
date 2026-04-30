package com.example.keywordrecorder.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Transcribe
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.keywordrecorder.R
import com.example.keywordrecorder.data.TranscriptionStatus
import com.example.keywordrecorder.util.TimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingDetailScreen(
    id: Long,
    onBack: () -> Unit,
    viewModel: RecordingDetailViewModel = viewModel(),
) {
    val recording by viewModel.recording.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(id) { viewModel.load(id) }
    LaunchedEffect(Unit) {
        viewModel.event.collect { message -> snackbarHostState.showSnackbar(message) }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_dialog_title)) },
            text = { Text(stringResource(R.string.delete_dialog_message)) },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.delete(); onBack() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text(stringResource(R.string.delete_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.delete_cancel)) }
            },
        )
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text(stringResource(R.string.detail_title), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.detail_cd_back))
                    }
                },
            )

            Box(modifier = Modifier.fillMaxSize()) {
                val rec = recording
                if (rec == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = MaterialTheme.shapes.large,
                        ) {
                            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                InfoRow(stringResource(R.string.detail_label_date), TimeUtils.formatEpoch(rec.createdAtEpochMillis))
                                InfoRow(stringResource(R.string.detail_label_duration), TimeUtils.formatDuration(rec.durationMillis))
                                InfoRow(stringResource(R.string.detail_label_file), rec.fileName)
                                InfoRow(stringResource(R.string.detail_label_status), friendlyStatus(rec.transcriptionStatus))
                            }
                        }

                        Text(stringResource(R.string.detail_section_transcript), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = MaterialTheme.shapes.large,
                        ) {
                            Text(
                                text = rec.transcriptText ?: transcriptPlaceholder(rec.transcriptionStatus),
                                modifier = Modifier.padding(20.dp),
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (rec.transcriptText != null)
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                else
                                    MaterialTheme.colorScheme.outline,
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(onClick = viewModel::play, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = stringResource(R.string.detail_cd_play), modifier = Modifier.padding(end = 6.dp))
                                Text(stringResource(R.string.detail_play))
                            }
                            Button(onClick = viewModel::transcribeNow, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Rounded.Transcribe, contentDescription = stringResource(R.string.detail_cd_transcribe), modifier = Modifier.padding(end = 6.dp))
                                Text(stringResource(R.string.detail_transcribe))
                            }
                        }

                        OutlinedButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        ) {
                            Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.detail_cd_delete), modifier = Modifier.padding(end = 6.dp))
                            Text(stringResource(R.string.detail_delete))
                        }
                    }
                }

                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }
}

private fun friendlyStatus(status: TranscriptionStatus) = when (status) {
    TranscriptionStatus.PENDING -> "Pending"
    TranscriptionStatus.PROCESSING -> "Processing"
    TranscriptionStatus.COMPLETED -> "Done"
    TranscriptionStatus.FAILED -> "Failed"
    TranscriptionStatus.SKIPPED -> "Skipped"
}

private fun transcriptPlaceholder(status: TranscriptionStatus) = when (status) {
    TranscriptionStatus.PENDING -> "Tap Transcribe to generate a transcript."
    TranscriptionStatus.PROCESSING -> "Transcription in progress…"
    TranscriptionStatus.FAILED -> "Transcription failed. Tap Transcribe to retry."
    TranscriptionStatus.SKIPPED -> "This recording was skipped."
    TranscriptionStatus.COMPLETED -> "No transcript available."
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
