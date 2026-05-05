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
AGP: `8.7.3`, compileSdk/targetSdk: `35`, minSdk: `29`

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
| `ScheduledTranscriptionWorker.kt` | WorkManager worker — transcribes pending/failed recordings |
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

## Service Error Recovery

`KeywordListeningService` catches per-cycle errors inside the `while(isActive)` loop rather than outside it. On failure it retries with back-off (2 s after the first error, 5 s after the second) before permanently emitting `ERROR` and breaking after 3 consecutive failures. The `consecutiveErrors` counter resets to 0 at the end of every successful detection+recording cycle.

The wake keyword is re-read from DataStore and a fresh `VoskWakeWordDetector` is created at the top of each loop iteration, so keyword changes made in Settings take effect on the next cycle without restarting the service.

## Silence Detection

The service polls `recorder.getMaxAmplitude()` every 200 ms. When amplitude stays below the effective silence threshold for `silenceTimeoutSeconds` (configurable via Settings, default 2 s), the recording stops.

**Adaptive noise floor calibration:** At the start of each recording the service samples ambient noise for the first 600 ms, then computes:

```
effectiveThreshold = (noiseFloor × 3.0).coerceIn(MIN = 1500, MAX = 8000)
```

This adapts to the user's environment — a noisy room raises the threshold so background noise cannot prevent silence detection. The `MAX_SILENCE_THRESHOLD = 8000` cap prevents the threshold from being set impossibly high if the user speaks during the calibration window. Only the timeout duration is user-configurable; the multiplier and bounds are constants in `KeywordListeningService`.

`SilenceDetector.kt` exists but is unused — detection is inline in the service.

## Vosk Model

- Downloaded once on first use from `https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip` (~40 MB)
- Extracted to `context.filesDir/vosk-model-small-en-us-0.15/`
- `VoskModelManager` uses a `Mutex` to prevent concurrent downloads
- Calling `ensureModel()` after an error state retries the download (the manager deletes the partial dir and starts fresh)
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

Batch mode (`ScheduledTranscriptionWorker` with no `recording_id`) uses `getRetryable(maxRetryCount)` when `retryFailed = true` (the default), so eligible `FAILED` recordings are automatically retried in addition to `PENDING` ones. `retryCount` is incremented atomically each time a recording fails.

## Settings

All settings live in `AppSettings` (DataStore). Defaults:

- `wakeKeyword` = `"keyword"`
- `transcriptionMode` = `IMMEDIATE` (options: `OFF`, `IMMEDIATE`, `DAILY`)
- `maxRecordingSeconds` = `30`
- `silenceTimeoutSeconds` = `2`
- `dailyTranscriptionHour` = `21`, `dailyTranscriptionMinute` = `0`
- `retryFailed` = `true`, `maxRetryCount` = `3`
- `deleteAudioAfterTranscription` = `false`, `onlyWifi` = `false`, `onlyCharging` = `false`

Settings with UI controls: `wakeKeyword`, `transcriptionMode`, `silenceTimeoutSeconds`, `maxRecordingSeconds`, and daily time (shown only when mode = `DAILY`). The remaining settings (`retryFailed`, `onlyWifi`, `onlyCharging`, `deleteAudioAfterTranscription`) exist in the data class but have no UI.

## Soft Delete & Undo Pattern

Recordings are never hard-deleted from the DB immediately. The delete flow is:

1. `RecordingDao.softDelete(id)` — sets `deleted = 1`; the Room Flow removes the item from all lists instantly
2. Show an Undo snackbar for ~4 seconds
3. If **Undo** tapped: `RecordingRepository.restoreRecording(id)` — sets `deleted = 0`, item reappears
4. If snackbar dismissed: `FileUtils.deleteIfExists(filePath)` — removes the audio file from disk

`RecordingDao.getById` and `observeById` both filter `WHERE deleted = 0`, so the detail screen can never load a soft-deleted record. If a recording is deleted while the detail screen is open, the screen navigates back automatically via a `LaunchedEffect` that watches the `recording` StateFlow for a `null` emission.

## UI Conventions

- `RecordingsScreen` and `SettingsScreen` use `Scaffold` as root (to host `SnackbarHost`); other screens use `Surface(modifier = Modifier.fillMaxSize())`
- Cards use `MaterialTheme.shapes.large` and `colorScheme.surfaceVariant`
- `ColumnScope` (not `Column`) for `@Composable` content lambdas passed as parameters
- No `onBack` parameter on screens — navigation is handled entirely by `AppNavGraph`
- `ListenerStateBus` (singleton `StateFlow`) is the only channel between the service and UI
- `AppNavGraph` owns a `RecordingsViewModel` instance to drive the recording-count badge on the nav bar — do not create a second independent instance in `RecordingsScreen`

## MediaPlayer Lifecycle

`MediaPlayer` in `RecordingDetailScreen` is held in Compose `remember` state. Always call `player?.stop()` before `player?.release()` — calling `release()` on an actively playing player without stopping first can throw `IllegalStateException` on some devices. There are three disposal sites, all of which must follow this order:

1. `DisposableEffect(Unit) { onDispose { player?.stop(); player?.release() } }`
2. The Stop playback button handler
3. The delete confirmation dialog's confirm button

## Permission Handling

`HomeViewModel` sets `needsPermission = true` when `RECORD_AUDIO` is not granted. `HomeScreen` launches the system permission dialog in response. If the user denies, `showPermissionDeniedDialog = true` triggers an `AlertDialog` that deep-links to the app's system settings page via `Settings.ACTION_APPLICATION_DETAILS_SETTINGS`. Do not rely on `shouldShowRequestPermissionRationale` — the dialog always opens Settings on any denial.

## Foreground Service Notes

- Service type: `microphone` — required for background mic on Android 10+
- Notification channel: `keyword_listening`, importance LOW
- `BootReceiver` is a no-op — listening is user-initiated only
- The service does NOT auto-start; user must tap the record button on the Home screen

## Known Issues / TODOs

- `PorcupineWakeWordDetector` — all stubs, not integrated
- `CloudTranscriptionEngine` / `LocalTranscriptionEngine` — throw `UnsupportedOperationException`
- Settings without UI: `retryFailed`, `onlyWifi`, `onlyCharging`, `deleteAudioAfterTranscription`
- `SilenceDetector.kt` class exists but is unused — silence detection is inline in the service
- No Paging 3 on the recordings list — `observeAll()` loads everything into memory

## What NOT to Do

- Do not add `fallbackToDestructiveMigration()` to the database builder
- Do not upgrade Gradle past 8.11.1 without also upgrading AGP
- Do not remove the `NonCancellable` wrapper in the service finally block
- Do not use `LiveData` — the project uses `StateFlow` throughout
- Do not add `onBack` parameters to screens — `AppNavGraph` owns navigation
- Do not remove `player?.stop()` before `player?.release()` in `RecordingDetailScreen`
- Do not move the error `catch` back outside the `while(isActive)` loop in `KeywordListeningService` — that would kill auto-recovery

# CLAUDE.md

Behavioral guidelines to reduce common LLM coding mistakes. Merge with project-specific instructions as needed.

**Tradeoff:** These guidelines bias toward caution over speed. For trivial tasks, use judgment.

## 1. Think Before Coding

**Don't assume. Don't hide confusion. Surface tradeoffs.**

Before implementing:
- State your assumptions explicitly. If uncertain, ask.
- If multiple interpretations exist, present them - don't pick silently.
- If a simpler approach exists, say so. Push back when warranted.
- If something is unclear, stop. Name what's confusing. Ask.

## 2. Simplicity First

**Minimum code that solves the problem. Nothing speculative.**

- No features beyond what was asked.
- No abstractions for single-use code.
- No "flexibility" or "configurability" that wasn't requested.
- No error handling for impossible scenarios.
- If you write 200 lines and it could be 50, rewrite it.

Ask yourself: "Would a senior engineer say this is overcomplicated?" If yes, simplify.

## 3. Surgical Changes

**Touch only what you must. Clean up only your own mess.**

When editing existing code:
- Don't "improve" adjacent code, comments, or formatting.
- Don't refactor things that aren't broken.
- Match existing style, even if you'd do it differently.
- If you notice unrelated dead code, mention it - don't delete it.

When your changes create orphans:
- Remove imports/variables/functions that YOUR changes made unused.
- Don't remove pre-existing dead code unless asked.

The test: Every changed line should trace directly to the user's request.

## 4. Goal-Driven Execution

**Define success criteria. Loop until verified.**

Transform tasks into verifiable goals:
- "Add validation" → "Write tests for invalid inputs, then make them pass"
- "Fix the bug" → "Write a test that reproduces it, then make it pass"
- "Refactor X" → "Ensure tests pass before and after"

For multi-step tasks, state a brief plan:
```
1. [Step] → verify: [check]
2. [Step] → verify: [check]
3. [Step] → verify: [check]
```

Strong success criteria let you loop independently. Weak criteria ("make it work") require constant clarification.

---

**These guidelines are working if:** fewer unnecessary changes in diffs, fewer rewrites due to overcomplication, and clarifying questions come before implementation rather than after mistakes.
