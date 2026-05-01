package com.example.keywordrecorder.ui.recordings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.keywordrecorder.data.DailySummaryEntity
import com.example.keywordrecorder.data.RecordingEntity
import com.example.keywordrecorder.data.TranscriptionStatus
import com.example.keywordrecorder.util.TimeUtils

@Composable
fun RecordingsScreen(
) {

                    ) {
                        Text(
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                        )
                    }
                }
                    if (summaries.isNotEmpty()) {
                        item {
                            Text(
                            )
                        }
                        }
                    }
                    if (recordings.isNotEmpty()) {
                        item {
                            Text(
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                    Text(
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                    )
            }
            Text(
            )
    }
}

@Composable
            Row(
            ) {
                Text(
                )
            Text(
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                )
            }
}
