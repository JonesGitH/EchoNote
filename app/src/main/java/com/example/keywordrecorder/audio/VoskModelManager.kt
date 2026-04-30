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
    object NotReady : ModelState()
    data class Downloading(val progress: Int) : ModelState()
    object Extracting : ModelState()
    object Ready : ModelState()
    data class Error(val message: String) : ModelState()
}

class VoskModelManager(private val context: Context) {

    private val modelName = "vosk-model-small-en-us-0.15"
    private val modelUrl = "https://alphacephei.com/vosk/models/$modelName.zip"
    private val modelDir get() = File(context.filesDir, modelName)

    private val mutex = Mutex()
    private val _state = MutableStateFlow<ModelState>(ModelState.NotReady)
    val state: StateFlow<ModelState> = _state

    private var model: Model? = null

    suspend fun ensureModel(): Model = mutex.withLock {
        model?.let { return it }
        withContext(Dispatchers.IO) {
            if (!modelDir.exists()) {
                downloadAndExtract()
            }
            val loaded = Model(modelDir.absolutePath)
            model = loaded
            _state.value = ModelState.Ready
            loaded
        }
    }

    private fun downloadAndExtract() {
        val zipFile = File(context.cacheDir, "$modelName.zip")
        _state.value = ModelState.Downloading(0)

        URL(modelUrl).openConnection().apply {
            connect()
            val total = contentLengthLong
            zipFile.outputStream().use { out ->
                getInputStream().use { input ->
                    val buf = ByteArray(8192)
                    var downloaded = 0L
                    var read: Int
                    while (input.read(buf).also { read = it } != -1) {
                        out.write(buf, 0, read)
                        downloaded += read
                        if (total > 0) {
                            _state.value = ModelState.Downloading((downloaded * 100 / total).toInt())
                        }
                    }
                }
            }
        }

        _state.value = ModelState.Extracting
        ZipInputStream(zipFile.inputStream()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val outFile = File(context.filesDir, entry.name)
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { zip.copyTo(it) }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        zipFile.delete()
    }
}
