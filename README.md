# EchoNote

EchoNote is an Android app that listens for a wake keyword, records audio, stores recordings locally, and transcribes them on-device using Vosk.

## Features
- Compose + MVVM app with Home, Recordings, Detail, and Settings screens.
- Foreground microphone listening service with persistent notification.
- On-device wake-word detection using Vosk.
- Audio recording via `MediaRecorder` into `filesDir/recordings/*.m4a`.
- Room database for recording metadata, transcription state, and daily summaries.
- WorkManager daily scheduler + worker to batch transcribe pending recordings.
- Real-time transcription updates on detail screens.
- Automatic daily summary generation.
- Data management options to clear recording history.

## Required permissions
- `android.permission.RECORD_AUDIO`
- `android.permission.POST_NOTIFICATIONS`
- `android.permission.FOREGROUND_SERVICE`
- `android.permission.FOREGROUND_SERVICE_MICROPHONE`
- `android.permission.WAKE_LOCK`
- `android.permission.RECEIVE_BOOT_COMPLETED`

## Run instructions
1. Open project in Android Studio (latest stable).
2. Let Gradle sync.
3. Run on an Android 8.0+ (API 26) device/emulator with microphone support.
4. Grant microphone and notification permissions.
5. On first run, the app will download a small Vosk voice model (English US).

## How to use
1. Open Home screen and tap the record/play button to start listening.
2. Say the wake keyword (default: "keyword") to trigger a recording.
3. The app records until silence is detected or the max duration is reached.
4. View your recordings and transcripts in the **Recordings** tab.
5. Manage keywords and transcription modes in **Settings**.

## Architecture & Logic
- **Scheduling**: Uses `java.time` for precise delay calculation and WorkManager for reliable execution.
- **Persistence**: Room for metadata and summaries; DataStore for user preferences.
- **Boot Recovery**: Automatically resumes listening service after device reboot.
- **Live UI**: Reactive states ensure the UI reflects background transcription progress instantly.
