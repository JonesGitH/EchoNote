package com.example.keywordrecorder.ui

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import com.example.keywordrecorder.ui.navigation.AppNavGraph
import com.example.keywordrecorder.ui.theme.KeywordRecorderTheme

class MainActivity : ComponentActivity() {
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* permissions handled at service level */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.POST_NOTIFICATIONS,
            )
        )

        setContent {
            KeywordRecorderTheme {
                AppNavGraph()
            }
        }
    }
}
