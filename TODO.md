# EchoNote - Project TODOs

This file tracks pending improvements, UI refinements, and known issues that need to be addressed in future sessions.

## UI/UX Refinements
- [ ] **Recording Panel Buttons**: Review and refine the layout and interaction of the buttons in the home screen recording panel.
- [ ] **RecordingCard**: Refine the design and metadata display in the `RecordingCard` component for better information hierarchy.
- [x] **Action Chips**: Placeholder AI action chips (Summarize, Action items, Translate) removed from `RecordingDetailScreen`.
- [ ] **Pause Button Consistency**: Update the pause button's styling to match the "Echo.Notes" premium navy/indigo look and feel used in the rest of the application.

## Stability & Lifecycle
- [x] **MediaPlayer Lifecycle**: `player?.stop()` now called before `player?.release()` in all three disposal sites in `RecordingDetailScreen` (DisposableEffect, Stop button, Delete dialog). Back-navigation to a deleted recording is also handled via a reactive `LaunchedEffect`.
- [x] **Service Error Recovery**: `KeywordListeningService` now retries on transient errors with 2s/5s back-off before permanently stopping after 3 consecutive failures.

## Accessibility
- [ ] **Accessibility Gaps**: Perform a general accessibility audit.
- [ ] **Remove Emojis**: Replace hard-coded emojis (e.g., 🎙, ⋯, ≡, ▶, ⏸) used for icons with proper Material Design vector icons or custom SVG assets to ensure better screen reader support and visual consistency.

## Bugs Fixed
- [x] **Permanent Permission Denial**: `HomeScreen` now shows a dialog with "Open Settings" link when microphone permission is permanently denied.
- [x] **Deleted Recording Accessible via Detail**: `RecordingDao.getById` and `observeById` now filter `deleted = 0`.
- [x] **Failed Transcription Auto-Retry**: `ScheduledTranscriptionWorker` batch mode now uses `getRetryable(maxRetryCount)` to include eligible FAILED recordings.
- [x] **Silence Timeout Unexposed**: Added silence timeout slider (1–10 s) to Settings screen, wired to DataStore.
- [x] **No Keyword Save Feedback**: Settings screen now shows a Snackbar on keyword save.
- [x] **Mode Descriptions Missing**: Transcription mode section now shows a description of the selected mode.
- [x] **Swipe-to-Delete + Undo**: `RecordingsScreen` supports swipe-to-delete with an Undo snackbar (file deletion deferred until undo window passes).
- [x] **No Delete All UI**: Trash icon added to `RecordingsScreen` header with confirmation dialog.
- [x] **Model Retry Missing**: Error banner in `HomeScreen` now shows a "Retry" button that re-invokes `VoskModelManager.ensureModel()`.
- [x] **Keyword Not Shown While Listening**: `HomeScreen` now displays `Say "keyword"` hint below the status label when in LISTENING state.
- [x] **Recording Count Badge**: Recordings nav item now shows a badge with the current count.

## Technical Debt / Features
- [ ] **PorcupineWakeWordDetector**: Implement the actual Porcupine engine stub.
- [ ] **Cloud/Local Engines**: Implement `CloudTranscriptionEngine` and `LocalTranscriptionEngine`.
- [x] **Audio Cleanup**: `deleteAll()` now fetches all file paths before soft-deleting, then deletes each `.m4a` via `FileUtils.deleteIfExists()`.
- [ ] **SilenceDetector**: Migrate the inline silence detection in `KeywordListeningService` to use the dedicated `SilenceDetector.kt` class.
- [ ] **Pagination**: `RecordingDao.observeAll()` loads all recordings in memory; add Paging 3 if the list grows large.
