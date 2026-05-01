package com.example.keywordrecorder.transcription

import com.example.keywordrecorder.audio.VoskModelManager
import com.example.keywordrecorder.domain.TranscriptionEngine
import com.example.keywordrecorder.domain.TranscriptionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.vosk.Recognizer

class VoskTranscriptionEngine(private val modelManager: VoskModelManager) : TranscriptionEngine {


        extractor.setDataSource(filePath)
        var trackIndex = -1
        for (i in 0 until extractor.trackCount) {
            }
        extractor.selectTrack(trackIndex)
        codec.configure(format, null, null, 0)
        codec.start()
        var inputDone = false
                if (!inputDone) {
                        if (sampleSize < 0) {
                            inputDone = true
                        } else {
                            extractor.advance()
                        }
                    }
                }
                }
            }
            codec.stop()
            codec.release()
            extractor.release()
    }
}
