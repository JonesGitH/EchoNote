# EchoNote — Claude Instructions

## Project Overview

Android app (Kotlin + Jetpack Compose) that listens in the background for a wake keyword, records speech, transcribes it on-device with Vosk, and rolls up daily transcriptions into a combined summary at 10 PM.

Working directory: `E:\Code\EchoNote`
Open in Android Studio — Gradle sync required before building.

## Build & Install

```
Build → Build Bundle(s) / APK(s) → Build APK(s)
Output: app\build\outputs\apk\debug\app-debug.apk
```

Gradle wrapper: `gradle-8.11.1-bin.zip`
AGP: `8.7.3`, compileSdk/targetSdk: `35`, minSdk: `26`

Do not upgrade Gradle past 8.11.1 without also upgrading AGP — they have a strict compatibility matrix.

## Architecture

- **MVVM** — ViewModels own state, screens are stateless
- **StateFlow** everywhere — no LiveData, no callbacks
- **Room** for persistence, **DataStore Preferences** for settings
- **WorkManager** for all background transcription jobs
- **Foreground service** (`KeywordListeningService`) for continuous mic access

### Key entry points

| File | Role |
|---|---|
| `KeywordRecorderApp.kt` | Application class — wires all singletons, starts service, schedules jobs |
| `KeywordListeningService.kt` | Core loop: detect → record → save → enqueue transcription |
| `ScheduledTranscriptionWorker.kt` | WorkManager worker — transcribes pending recordings |
| `DailySummaryWorker.kt` | 10 PM worker — combines daily transcripts, deletes originals |
| `AppNavGraph.kt` | Navigation graph with bottom bar |

## Database

Room v2, file: `keyword_recorder.db`

- `recordings` — individual audio recordings with transcription status
- `daily_summaries` — daily rollup entries created by `DailySummaryWorker`
- Current migration: `MIGRATION_1_2` (adds `daily_summaries` table)

**When adding a new entity or column:** bump the database version in `AppDatabase.kt`, add a new `Migration` object, and register it in `KeywordRecorderApp` via `.addMigrations(...)`. Never use `fallbackToDestructiveMigration()`.

## Key Interfaces and Their Implementations

```
WakeWordDetector     → VoskWakeWordDetector (production)
                     → FakeWakeWordDetector (test stub, 5–15s random delay)
                     → PorcupineWakeWordDetector (TODO — empty stub)

AudioRecorder        → AndroidAudioRecorder

TranscriptionEngine  → VoskTranscriptionEngine (production)
                     → FakeTranscriptionEngine (test stub)
                     → CloudTranscriptionEngine (TODO — throws UnsupportedOperationException)
                     → LocalTranscriptionEngine (TODO — throws UnsupportedOperationException)
```

The service always instantiates `VoskWakeWordDetector` and `KeywordRecorderApp.transcriptionEngine()` always returns `VoskTranscriptionEngine`.

## Recording Flow (important invariant)

Inside `KeywordListeningService`, the stop-and-save block is wrapped in `withContext(NonCancellable)` inside a `finally`. This guarantees the recording is always persisted even if the user taps Stop mid-recording. Do not remove this — without it recordings silently vanish on cancellation.

```kotlin
try {
    // silence detection loop
} finally {
    withContext(NonCancellable) {
        val result = recorder.stopRecording()
        val id = app.recordingRepository.insertRecording(result)
        // enqueue transcription if IMMEDIATE
    }
}
```

## Silence Detection

The service polls `recorder.getMaxAmplitude()` every 200 ms. Amplitude below `SILENCE_AMPLITUDE_THRESHOLD = 500` for `silenceTimeoutSeconds` (default 2s) stops the recording. This threshold may need tuning per device/environment. `SilenceDetector.kt` exists but is unused — the service does this inline.

## Vosk Model

- Downloaded once on first use from `https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip` (~40 MB)
- Extracted to `context.filesDir/vosk-model-small-en-us-0.15/`
- `VoskModelManager` uses a `Mutex` to prevent concurrent downloads
- Wake word detection uses a grammar-constrained recognizer: `["keyword", "[unk]"]`
- Transcription uses a full-vocabulary recognizer (no grammar)

## WorkManager Jobs

| Unique ID | Worker | When |
|---|---|---|
| `scheduled_batch_transcription` | `ScheduledTranscriptionWorker` | Daily at `dailyTranscriptionHour:dailyTranscriptionMinute` (default 21:00) |
| `daily_summary` | `DailySummaryWorker` | Daily at 22:00 (hardcoded in `TranscriptionScheduler`) |
| `immediate_transcription_recording_$id` | `ScheduledTranscriptionWorker` | Immediately after each recording (when mode = IMMEDIATE) |
| `manual_transcription_recording_$id` | `ScheduledTranscriptionWorker` | On demand from detail screen |

Both scheduled jobs re-enqueue themselves at the end of `doWork()` for the next day.

## Settings

All settings live in `AppSettings` (DataStore). Defaults:

- `wakeKeyword` = `"keyword"`
- `transcriptionMode` = `IMMEDIATE`
- `maxRecordingSeconds` = `30`
- `silenceTimeoutSeconds` = `2`
- `dailyTranscriptionHour` = `21`, `dailyTranscriptionMinute` = `0`

Only three settings have UI controls: `wakeKeyword`, `transcriptionMode`, and daily time (shown only when mode = DAILY). The rest are in the data class but not yet exposed in the UI.

## UI Conventions

- All screens use `Surface(modifier = Modifier.fillMaxSize())` as root
- `TopAppBar` on every screen except Home
- Cards use `MaterialTheme.shapes.large` and `colorScheme.surfaceVariant`
- `ColumnScope` (not `Column`) for `@Composable` content lambdas passed as parameters
- No `onBack` parameter on screens — navigation is handled entirely by `AppNavGraph`
- `ListenerStateBus` (singleton `StateFlow`) is the only channel between the service and UI

## Foreground Service Notes

- Service type: `microphone` — required for background mic on Android 10+
- Notification channel: `keyword_listening`, importance LOW
- `BootReceiver` restarts the service on `BOOT_COMPLETED`
- The service auto-starts from `KeywordRecorderApp.onCreate` — no manual tap needed

## Known Issues / TODOs

- `PorcupineWakeWordDetector` — all stubs, not integrated
- `CloudTranscriptionEngine` / `LocalTranscriptionEngine` — throw `UnsupportedOperationException`
- `SilenceDetector.kt` — dead code, never called
- `RecordingDetailScreen.play()` — creates a `MediaPlayer` without releasing it (audio resource leak)
- Several settings have no UI: `silenceTimeoutSeconds`, `retryFailed`, `onlyWifi`, `onlyCharging`, `deleteAudioAfterTranscription`

## What NOT to Do

- Do not add `fallbackToDestructiveMigration()` to the database builder
- Do not upgrade Gradle past 8.11.1 without also upgrading AGP
- Do not remove the `NonCancellable` wrapper in the service finally block
- Do not use `LiveData` — the project uses `StateFlow` throughout
- Do not add `onBack` parameters to screens — `AppNavGraph` owns navigation
