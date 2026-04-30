package com.example.keywordrecorder.util

import java.io.File

object FileUtils {
    fun deleteIfExists(path: String) {
        val file = File(path)
        if (file.exists()) file.delete()
    }
}
