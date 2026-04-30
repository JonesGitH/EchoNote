package com.example.keywordrecorder.util

import java.io.File

object FileUtils {
    fun deleteIfExists(filePath: String) {
        val file = File(filePath)
        if (file.exists()) file.delete()
    }
}
