package com.example.keywordrecorder.audio

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.vosk.Model
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.zip.ZipInputStream

sealed class ModelState {
    object Idle : ModelState()
    data class Downloading(val progressPercent: Int) : ModelState()
    object Extracting : ModelState()
    object Ready : ModelState()
    data class Error(val message: String) : ModelState()
}

class VoskModelManager(private val context: Context) {
    private val _state = MutableStateFlow<ModelState>(ModelState.Idle)
    val state: StateFlow<ModelState> = _state

    var model: Model? = null
        private set

    private val mutex = Mutex()
    private val modelDir get() = File(context.filesDir, MODEL_DIR_NAME)

    suspend fun ensureModel() = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (model != null) return@withLock
            if (!modelDir.exists()) {
                downloadAndExtract()
                if (_state.value is ModelState.Error) return@withLock
            }
            runCatching {
                _state.value = ModelState.Extracting
                model = Model(modelDir.absolutePath)
                _state.value = ModelState.Ready
            }.onFailure {
                _state.value = ModelState.Error(it.message ?: "Failed to load model")
            }
        }
    }

    private fun downloadAndExtract() {
        val zipFile = File(context.filesDir, "vosk-model.zip")
        try {
            val url = URL(MODEL_URL)
            val connection = url.openConnection()
            connection.connect()
            val totalBytes = connection.contentLength.toLong()

            _state.value = ModelState.Downloading(0)
            url.openStream().use { input ->
                FileOutputStream(zipFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead = 0L
                    var len: Int
                    while (input.read(buffer).also { len = it } != -1) {
                        output.write(buffer, 0, len)
                        bytesRead += len
                        if (totalBytes > 0) {
                            _state.value = ModelState.Downloading(((bytesRead * 100) / totalBytes).toInt())
                        }
                    }
                }
            }

            _state.value = ModelState.Extracting
            ZipInputStream(zipFile.inputStream()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val file = File(context.filesDir, entry.name)
                    if (entry.isDirectory) {
                        file.mkdirs()
                    } else {
                        file.parentFile?.mkdirs()
                        FileOutputStream(file).use { zip.copyTo(it) }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        } catch (e: Exception) {
            modelDir.deleteRecursively()
            _state.value = ModelState.Error(e.message ?: "Download failed")
        } finally {
            zipFile.delete()
        }
    }

    companion object {
        private const val MODEL_DIR_NAME = "vosk-model-small-en-us-0.15"
        private const val MODEL_URL =
            "https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip"
    }
}
