package com.example.keywordrecorder.audio

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.vosk.Model
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.zip.ZipInputStream

sealed class ModelState {
    object Extracting : ModelState()
    object Ready : ModelState()
    data class Error(val message: String) : ModelState()
}

class VoskModelManager(private val context: Context) {


    private val mutex = Mutex()

            if (!modelDir.exists()) {
                downloadAndExtract()
            }
                _state.value = ModelState.Ready
        }
    }

    private fun downloadAndExtract() {
            _state.value = ModelState.Downloading(0)
                        }
                    }
                }
            }

            _state.value = ModelState.Extracting
            ZipInputStream(zipFile.inputStream()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (entry.isDirectory) {
                    } else {
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
            zipFile.delete()
        }
    }
