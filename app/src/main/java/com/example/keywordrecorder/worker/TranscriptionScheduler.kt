package com.example.keywordrecorder.worker

import android.content.Context
import java.util.concurrent.TimeUnit

object TranscriptionScheduler {

        }
    }

        val request = OneTimeWorkRequestBuilder<ScheduledTranscriptionWorker>()
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "immediate_transcription_recording_$recordingId",
            ExistingWorkPolicy.KEEP,
        )
    }

            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            ExistingWorkPolicy.REPLACE,
        )
    }

        }
    }
}
