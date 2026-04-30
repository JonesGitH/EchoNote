# Keyword Recorder

Keyword Recorder is an Android MVP app that listens for a wake keyword, records audio, stores recordings locally, and transcribes pending items on a schedule.

## Features in this MVP
- Compose + MVVM app with Home, Recordings, Detail, and Settings screens.
- Foreground microphone listening service with persistent notification.
- Fake wake-word detector to trigger recordings without external dependencies.
- Audio recording via `MediaRecorder` into `filesDir/recordings/*.m4a`.
- Room database for recording metadata and transcription state.
- WorkManager daily scheduler + worker to batch transcribe pending recordings.
- Fake transcription engine with pluggable interfaces for real engines.

## Required permissions
- `android.permission.RECORD_AUDIO`
- `android.permission.POST_NOTIFICATIONS`
- `android.permission.FOREGROUND_SERVICE`
- `android.permission.FOREGROUND_SERVICE_MICROPHONE`
- `android.permission.WAKE_LOCK`

## Run instructions
1. Open project in Android Studio (latest stable).
2. Let Gradle sync.
3. Run on an Android 8.0+ device/emulator with microphone support.
4. Grant microphone and notification permissions.

## How to test fake wake-word detection
1. Open Home screen and tap **Start Listening**.
2. Verify persistent notification appears.
3. Wait a few moments (fake detector triggers probabilistically).
4. Confirm status switches to **RECORDING**, then returns to **LISTENING**.
5. Open Recordings and confirm a saved item exists.

## How to test scheduled transcription
1. Open Settings and set a near-future daily time.
2. Keep app installed; WorkManager enqueues daily one-time work at selected time.
3. Optionally open recording detail and use **Transcribe Now**.
4. Verify transcript text appears.

## Extension points
- Wake word engines:
  - `audio/FakeWakeWordDetector.kt`
  - `audio/PorcupineWakeWordDetector.kt` (TODO)
- Transcription engines:
  - `transcription/FakeTranscriptionEngine.kt`
  - `transcription/LocalTranscriptionEngine.kt` (TODO for Whisper.cpp / Vosk)
  - `transcription/CloudTranscriptionEngine.kt` (TODO for cloud API)

## Known Android background limitations
- OEM battery optimization can stop foreground services and delay WorkManager.
- Notification permission denied (Android 13+) reduces service UX reliability.
- Long-running audio behavior differs by OEM and power mode.
