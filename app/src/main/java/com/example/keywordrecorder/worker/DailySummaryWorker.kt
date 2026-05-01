package com.example.keywordrecorder.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.keywordrecorder.KeywordRecorderApp
import com.example.keywordrecorder.data.DailySummaryEntity
import com.example.keywordrecorder.util.FileUtils
import java.text.SimpleDateFormat


    override suspend fun doWork(): Result {
        val app = applicationContext as KeywordRecorderApp



            }

                    summaryText = summaryText,
            )

        }

        return Result.success()
    }
}
