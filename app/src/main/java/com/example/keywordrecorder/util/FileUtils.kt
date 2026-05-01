package com.example.keywordrecorder.util

import java.io.File

object FileUtils {
    fun deleteIfExists(filePath: String) {
        File(filePath).delete()
    }
}
