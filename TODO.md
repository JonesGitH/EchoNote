# EchoNote - Project TODOs

This file tracks pending improvements, UI refinements, and known issues that need to be addressed in future sessions.

## UI/UX Refinements
- [ ] **Recording Panel Buttons**: Review and refine the layout and interaction of the buttons in the home screen recording panel.
- [ ] **RecordingCard**: Refine the design and metadata display in the `RecordingCard` component for better information hierarchy.
- [ ] **Action Chips**: Remove the "AI action chips" (Summarize, Action items, Translate) from the `RecordingDetailScreen` as they are currently placeholders.
- [ ] **Pause Button Consistency**: Update the pause button's styling to match the "Echo.Notes" premium navy/indigo look and feel used in the rest of the application.

## Stability & Lifecycle
- [ ] **MediaPlayer Lifecycle**: Review and harden `MediaPlayer` management in `RecordingDetailScreen`. Ensure it is properly released during all lifecycle events (configuration changes, backgrounding) to prevent memory leaks or audio overlap.

## Accessibility
- [ ] **Accessibility Gaps**: Perform a general accessibility audit.
- [ ] **Remove Emojis**: Replace hard-coded emojis (e.g., 🎙, ⋯, ≡, ▶, ⏸) used for icons with proper Material Design vector icons or custom SVG assets to ensure better screen reader support and visual consistency.

## Technical Debt / Features
- [ ] **PorcupineWakeWordDetector**: Implement the actual Porcupine engine stub.
- [ ] **Cloud/Local Engines**: Implement `CloudTranscriptionEngine` and `LocalTranscriptionEngine`.
- [ ] **Audio Cleanup**: Ensure `deleteAll()` also removes the physical `.m4a` files from storage, not just the database rows.
- [ ] **SilenceDetector**: Migrate the inline silence detection in `KeywordListeningService` to use the dedicated `SilenceDetector.kt` class.
