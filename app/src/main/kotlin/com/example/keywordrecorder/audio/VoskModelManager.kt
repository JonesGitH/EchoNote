package com.example.keywordrecorder.audio

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.vosk.Model
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.zip.ZipInputStream

sealed class ModelState {
    object Idle : ModelState()
    data class Downloading(val percent: Int) : ModelState()
    object Extracting : ModelState()
    object Ready : ModelState()
    data class Error(val message: String) : ModelState()
}

private const val MODEL_NAME = "vosk-model-small-en-us-0.15"
private const val MODEL_URL = "https://alphacephei.com/vosk/models/$MODEL_NAME.zip"

class VoskModelManager(private val context: Context) {
    private val mutex = Mutex()
    private val _state = MutableStateFlow<ModelState>(ModelState.Idle)
    val state: StateFlow<ModelState> = _state

    private var model: Model? = null

    suspend fun ensureModel(): Model = mutex.withLock {
        model?.let { return it }

        val modelDir = File(context.filesDir, MODEL_NAME)
        if (!modelDir.exists()) {
            downloadAndExtract(modelDir)
        }

        _state.value = ModelState.Ready
        val loaded = Model(modelDir.absolutePath)
        model = loaded
        loaded
    }

    private suspend fun downloadAndExtract(modelDir: File) = withContext(Dispatchers.IO) {
        val zipFile = File(context.cacheDir, "$MODEL_NAME.zip")
        try {
            download(zipFile)
            _state.value = ModelState.Extracting
            extract(zipFile, modelDir)
        } finally {
            zipFile.delete()
        }
    }

    private suspend fun download(dest: File) = withContext(Dispatchers.IO) {
        val connection = URL(MODEL_URL).openConnection()
        connection.connect()
        val total = connection.contentLengthLong

        connection.getInputStream().use { input ->
            FileOutputStream(dest).use { output ->
                val buffer = ByteArray(8192)
                var downloaded = 0L
                var bytes: Int
                while (input.read(buffer).also { bytes = it } != -1) {
                    output.write(buffer, 0, bytes)
                    downloaded += bytes
                    if (total > 0) {
                        _state.value = ModelState.Downloading(((downloaded * 100) / total).toInt())
                    }
                }
            }
        }
    }

    private suspend fun extract(zipFile: File, destDir: File) = withContext(Dispatchers.IO) {
        destDir.mkdirs()
        ZipInputStream(zipFile.inputStream()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val entryPath = entry.name.substringAfter("$MODEL_NAME/")
                if (entryPath.isNotEmpty()) {
                    val target = File(destDir, entryPath)
                    if (entry.isDirectory) {
                        target.mkdirs()
                    } else {
                        target.parentFile?.mkdirs()
                        FileOutputStream(target).use { zis.copyTo(it) }
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }

    fun close() {
        model?.close()
        model = null
        _state.value = ModelState.Idle
    }
}
