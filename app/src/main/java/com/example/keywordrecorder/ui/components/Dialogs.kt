package com.example.keywordrecorder.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import com.example.keywordrecorder.ui.theme.EchoRed
import com.example.keywordrecorder.ui.theme.EchoSurface
import com.example.keywordrecorder.ui.theme.EchoTextPrimary
import com.example.keywordrecorder.ui.theme.EchoTextSecondary

@Composable
fun DeleteAllConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete All Recordings?") },
        text = { Text("Are you sure you want to delete all your recordings? This cannot be undone.") },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = EchoRed)
            ) {
                Text("Delete Everything")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        containerColor = EchoSurface,
        titleContentColor = EchoTextPrimary,
        textContentColor = EchoTextSecondary
    )
}
