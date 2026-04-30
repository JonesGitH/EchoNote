# EchoNote — Developer Handoff

## What It Is

EchoNote is an Android app that listens in the background for a configurable wake keyword, then automatically records everything you say until you go silent. Recordings are transcribed on-device using Vosk (no internet required after the first model download). At 10 PM each night, all that day's transcriptions are combined into a single daily summary and the individual recordings are deleted.

---

## Tech Stack

| Layer | Library / Version |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM, AndroidViewModel, StateFlow |
| Database | Room 2.6.1 (SQLite) |
| Background jobs | WorkManager 2.10.0 |
| Settings persistence | DataStore Preferences 1.1.2 |
| Speech recognition | Vosk Android 0.3.47 |
| Navigation | Navigation Compose 2.8.7 |
| AGP | 8.7.3 |
| compileSdk / targetSdk | 35 |
| minSdk | 26 (Android 8.0) |
| Java / JVM target | 17 |

---

## Project Location

```
E:\Code\EchoNote\
```

Open in Android Studio via **File → Open → E:\Code\EchoNote**.

---

## Package Structure

```
com.example.keywordrecorder
├── KeywordRecorderApp.kt           Application class — wires all dependencies
├── audio/
│   ├── VoskModelManager.kt         Downloads & loads Vosk model; exposes StateFlow<ModelState>
│   ├── VoskWakeWordDetector.kt     AudioRecord loop feeding Vosk; suspends until keyword heard
│   ├── FakeWakeWordDetector.kt     Test stub — triggers after 5–15s random delay
│   ├── PorcupineWakeWordDetector.kt  TODO stub (Porcupine not integrated)
│   ├── AndroidAudioRecorder.kt     MediaRecorder wrapper; records to .m4a, exposes getMaxAmplitude()
│   └── SilenceDetector.kt          RMS helper (currently unused — service uses getMaxAmplitude() directly)
├── data/
│   ├── AppDatabase.kt              Room DB v2; entities: RecordingEntity, DailySummaryEntity
│   ├── RecordingEntity.kt          Audio recording row
│   ├── DailySummaryEntity.kt       Daily rollup row
│   ├── RecordingDao.kt             Queries: observe, getPending, getCompletedSince, softDelete
│   ├── DailySummaryDao.kt          Queries: observeAll, insert
│   ├── RoomConverters.kt           TranscriptionStatus ↔ String
│   ├── SettingsDataStore.kt        DataStore wrapper; AppSettings data class with all defaults
│   └── TranscriptionStatus.kt      Enum: PENDING, PROCESSING, COMPLETED, FAILED, SKIPPED
├── domain/
│   ├── WakeWordDetector.kt         Interface: start(), stop(), awaitWakeWord()
│   ├── AudioRecorder.kt            Interface + RecordingSession / RecordingResult data classes
│   ├── TranscriptionEngine.kt      Interface: transcribe(filePath) → TranscriptionResult
│   ├── RecordingRepository.kt      Wraps RecordingDao
│   ├── DailySummaryRepository.kt   Wraps DailySummaryDao
│   └── TranscriptionRepository.kt  Orchestrates transcription + result persistence
├── notification/
│   └── ListeningNotification.kt    Foreground notification builder (channel, actions)
├── service/
│   ├── KeywordListeningService.kt  Core foreground service — detection → recording → save loop
│   ├── ListenerStateBus.kt         Singleton StateFlow<ListenerState> for service ↔ UI
│   └── BootReceiver.kt             BOOT_COMPLETED → restarts service after reboot
├── transcription/
│   ├── VoskTranscriptionEngine.kt  MediaExtractor/MediaCodec → PCM → Vosk Recognizer
│   ├── FakeTranscriptionEngine.kt  Test stub — returns placeholder text
│   ├── CloudTranscriptionEngine.kt TODO stub (throws UnsupportedOperationException)
│   └── LocalTranscriptionEngine.kt TODO stub (throws UnsupportedOperationException)
├── ui/
│   ├── MainActivity.kt             Entry point; requests RECORD_AUDIO + POST_NOTIFICATIONS
│   ├── navigation/AppNavGraph.kt   Bottom nav; routes: home, recordings, detail/{id}, settings
│   ├── home/                       Pulsing mic button, listener state display, model download progress
│   ├── recordings/                 List of daily summaries + individual recordings
│   ├── detail/                     Full transcript, play, re-transcribe, delete
│   ├── settings/                   Wake keyword, transcription mode, daily time picker
│   └── theme/                      Material 3 dynamic color + indigo fallback
├── util/
│   ├── TimeUtils.kt                formatEpoch(), formatDuration()
│   ├── FileUtils.kt                deleteIfExists()
│   └── PermissionUtils.kt          hasRecordAudio()
└── worker/
    ├── ScheduledTranscriptionWorker.kt  Transcribes pending recordings; reschedules daily
    ├── DailySummaryWorker.kt            10 PM rollup — combine, delete originals, reschedule
    └── TranscriptionScheduler.kt        WorkManager scheduling helpers
```

---

## Database Schema

**File:** `keyword_recorder.db` (Room v2)

### `recordings`
| Column | Type | Notes |
|---|---|---|
| id | INTEGER PK | autoGenerate |
| filePath | TEXT | absolute path to .m4a |
| fileName | TEXT | |
| createdAtEpochMillis | INTEGER | |
| durationMillis | INTEGER | |
| transcriptionStatus | TEXT | PENDING / PROCESSING / COMPLETED / FAILED / SKIPPED |
| transcriptText | TEXT | nullable |
| transcribedAtEpochMillis | INTEGER | nullable |
| retryCount | INTEGER | default 0 |
| lastErrorMessage | TEXT | nullable |
| deleted | INTEGER | 0 = active, 1 = soft deleted |

### `daily_summaries`
| Column | Type | Notes |
|---|---|---|
| id | INTEGER PK | autoGenerate |
| dateEpochMillis | INTEGER | start of that day (midnight local) |
| summaryText | TEXT | formatted `[H:MM AM] transcript` lines |
| recordingCount | INTEGER | how many recordings were combined |
| createdAtEpochMillis | INTEGER | when the rollup ran |

**Migration:** `MIGRATION_1_2` creates the `daily_summaries` table. Registered in `KeywordRecorderApp` via `.addMigrations(AppDatabase.MIGRATION_1_2)`.

---

## Full App Flow

### 1. App launch
`KeywordRecorderApp.onCreate` builds the database, wires repositories, starts `KeywordListeningService`, and schedules both WorkManager jobs.

### 2. Background listening loop (`KeywordListeningService`)
1. Load settings from DataStore
2. Create `VoskWakeWordDetector` with configured keyword
3. `detector.start()` → `VoskModelManager.ensureModel()`:
   - First run: downloads `vosk-model-small-en-us-0.15.zip` (~40 MB) from alphacephei.com
   - Extracts to `filesDir/vosk-model-small-en-us-0.15/`
   - Loads `org.vosk.Model` into memory
   - Progress broadcast via `ModelState.Downloading(%)` → `Extracting` → `Ready`
4. Emit `LISTENING` state
5. **Loop forever:**
   - `detector.awaitWakeWord()` — opens `AudioRecord` at 16kHz mono, feeds 4096-byte chunks to grammar-constrained Vosk `Recognizer(model, 16000f, """["keyword","[unk]"]""")`, checks both `finalResult` and `partialResult` for keyword match (case-insensitive)
   - On match: emit `WAKE_WORD_DETECTED`, reload settings, emit `RECORDING`
   - `recorder.startRecording()` — opens `MediaRecorder` → MPEG4/AAC at 16kHz mono → `recordings/recording_[ts].m4a`
   - Poll `recorder.getMaxAmplitude()` every 200 ms:
     - Amplitude < 500 for ≥ `silenceTimeoutSeconds` (default 2s) → stop
     - Elapsed > `maxRecordingSeconds` (default 30s) → stop
   - `finally` block with `NonCancellable`: stop recorder, insert `RecordingEntity` (PENDING) to DB
   - If `transcriptionMode == IMMEDIATE`: enqueue `ScheduledTranscriptionWorker` for this ID
   - Emit `LISTENING`, repeat

> **Important:** The `NonCancellable` wrapper means recordings are always saved even if the user taps Stop mid-recording.

### 3. Immediate transcription (`ScheduledTranscriptionWorker`)
- WorkManager runs the job without delay
- `VoskTranscriptionEngine.transcribe(filePath)`:
  - `MediaExtractor` + `MediaCodec` decode the M4A → raw PCM
  - PCM fed to `Vosk Recognizer` (no grammar — full vocab)
  - Returns `finalResult` text, falls back to `"[No speech detected]"` if blank
- Updates recording: `COMPLETED` + transcript text, or `FAILED` + error message + retry count

### 4. 10 PM daily rollup (`DailySummaryWorker`)
- Fetches all `COMPLETED` recordings since midnight local time
- Formats each as `[H:MM AM] transcript text`
- Inserts one `DailySummaryEntity` with all lines joined by `\n\n`
- Soft-deletes recordings (sets `deleted = 1`) and deletes their audio files from disk
- Re-schedules itself for the next 10 PM

### 5. Boot recovery (`BootReceiver`)
- Listens for `BOOT_COMPLETED`
- Sends `ACTION_START` to `KeywordListeningService` — app resumes listening without user interaction

---

## WorkManager Jobs

| Job | Unique ID | Schedule | Worker |
|---|---|---|---|
| Daily transcription batch | `scheduled_batch_transcription` | Daily at configured hour:minute (default 21:00) | `ScheduledTranscriptionWorker` |
| Daily summary rollup | `daily_summary` | Daily at 22:00 (hardcoded) | `DailySummaryWorker` |
| Immediate transcription | `immediate_transcription_recording_$id` | No delay, per recording | `ScheduledTranscriptionWorker` |
| Manual transcription | `manual_transcription_recording_$id` | No delay, on demand | `ScheduledTranscriptionWorker` |

All jobs use `ExistingWorkPolicy.KEEP` (immediate) or `REPLACE` (scheduled) and exponential backoff with 30s initial delay.

---

## Settings & Defaults

| Setting | Default | User-configurable |
|---|---|---|
| wakeKeyword | `"keyword"` | Yes — Settings screen |
| transcriptionMode | `IMMEDIATE` | Yes — OFF / IMMEDIATE / DAILY chips |
| dailyTranscriptionHour | `21` | Yes — only shown when mode = DAILY |
| dailyTranscriptionMinute | `0` | Yes — only shown when mode = DAILY |
| maxRecordingSeconds | `30` | Displayed only (no UI input) |
| silenceTimeoutSeconds | `2` | Not exposed in UI |
| retryFailed | `true` | Not exposed in UI |
| maxRetryCount | `3` | Not exposed in UI |
| deleteAudioAfterTranscription | `false` | Not exposed in UI |
| onlyWifi | `false` | Not exposed in UI |
| onlyCharging | `false` | Not exposed in UI |

---

## Foreground Service & Notification

- **Type:** `foregroundServiceType="microphone"` (required for background mic on Android 10+)
- **Channel:** `keyword_listening`, importance LOW (no sound/vibration)
- **Notification:** "EchoNote — Listening for wake keyword" with a "Stop Listening" action button
- **Always visible** in the status bar while active — Android mandates this; it cannot be hidden

---

## Known TODOs / Incomplete Stubs

| File | What's missing |
|---|---|
| `PorcupineWakeWordDetector.kt` | All three methods are empty stubs — Porcupine SDK not integrated |
| `CloudTranscriptionEngine.kt` | Throws `UnsupportedOperationException` — no cloud endpoint wired |
| `LocalTranscriptionEngine.kt` | Throws `UnsupportedOperationException` — Whisper.cpp not integrated |
| `SilenceDetector.kt` | Class exists but is never used; service uses `getMaxAmplitude()` directly |
| Settings UI | Several settings have no UI control: silenceTimeout, retryFailed, onlyWifi, onlyCharging, deleteAudioAfterTranscription |
| `RecordingDetailScreen` | `play()` creates a MediaPlayer but never releases it — potential audio leak |

---

## Permissions

```xml
RECORD_AUDIO                    — microphone access
INTERNET                        — Vosk model download (one time)
POST_NOTIFICATIONS              — foreground service notification (Android 13+)
FOREGROUND_SERVICE              — run foreground service
FOREGROUND_SERVICE_MICROPHONE   — microphone foreground service type
WAKE_LOCK                       — keep CPU awake while listening
RECEIVE_BOOT_COMPLETED          — auto-restart after reboot
```

---

## Building & Installing

1. Open `E:\Code\EchoNote` in Android Studio
2. Wait for Gradle sync to complete
3. **Build → Build Bundle(s) / APK(s) → Build APK(s)**
4. APK output: `app\build\outputs\apk\debug\app-debug.apk`
5. Transfer to phone and install (enable "Install unknown apps" for your file manager)

On first launch the app will download the ~40 MB Vosk model — requires internet once. All subsequent operation is fully offline.
