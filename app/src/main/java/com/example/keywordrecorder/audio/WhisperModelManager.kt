package com.example.keywordrecorder.audio

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

class WhisperModelManager(private val context: Context) {

    private val modelFileName = "ggml-tiny.en.bin"
    private val modelUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny.en.bin"
    private val modelFile get() = File(context.filesDir, modelFileName)

    private val mutex = Mutex()
    private val _state = MutableStateFlow<ModelState>(ModelState.NotReady)
    val state: StateFlow<ModelState> = _state

    suspend fun ensureModel(): String = mutex.withLock {
        if (modelFile.exists()) {
            _state.value = ModelState.Ready
            return modelFile.absolutePath
        }
        withContext(Dispatchers.IO) {
            val tmp = File(context.cacheDir, modelFileName)
            try {
                _state.value = ModelState.Downloading(0)
                URL(modelUrl).openConnection().apply {
                    connect()
                    val total = contentLengthLong
                    tmp.outputStream().use { out ->
                        getInputStream().use { input ->
                            val buf = ByteArray(8192)
                            var downloaded = 0L
                            var read: Int
                            while (input.read(buf).also { read = it } != -1) {
                                out.write(buf, 0, read)
                                downloaded += read
                                if (total > 0)
                                    _state.value = ModelState.Downloading((downloaded * 100 / total).toInt())
                            }
                        }
                    }
                }
                tmp.renameTo(modelFile)
                _state.value = ModelState.Ready
                modelFile.absolutePath
            } catch (e: Exception) {
                tmp.delete()
                modelFile.delete()
                _state.value = ModelState.Error(e.message ?: "Failed to download Whisper model")
                throw e
            }
        }
    }
}
