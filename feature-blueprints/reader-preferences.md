# Reader Preferences

## Outcome

Apply and persist reader-wide appearance and reading preferences, and provide
clear local-data controls whose effects are immediate, bounded, and private.

## Current verified status

**Status: Partial - code inspected 2026-08-02; no dedicated automated tests found.**

- System, Light, and Dark theme modes are stored and applied at the activity
  theme root.
- Keep screen awake adds or clears `FLAG_KEEP_SCREEN_ON` while the app activity is
  active.
- Read-aloud rate is clamped to `0.6f..1.6f`, persisted, and immediately applied
  to `TtsManager`.
- Preference updates change immutable UI state immediately and save after a
  250 ms debounce on the IO dispatcher.
- Settings explains on-device metadata and confirms before clearing recent
  document names, progress, and bookmarks without deleting PDFs.
- Preference persistence failures and process-recreation behavior have no
  dedicated automated coverage.

## Architecture dependencies

- [Information architecture](../.github/architecture.md#information-architecture)
- [Visual system](../.github/architecture.md#visual-system)
- [Component model and state ownership](../.github/architecture.md#component-model-and-state-ownership)
- [Local data and privacy](../.github/architecture.md#local-data-and-privacy)
- [Performance and lifecycle](../.github/architecture.md#performance-and-lifecycle)

## Feature-specific implications

### Information architecture and visual system

- Settings remains a top-level route from the library and returns without
  replacing the current metadata state.
- Theme selection applies consistently to library, reader, and settings chrome;
  annotation colors and rendered PDF pixels are not rewritten as theme colors.
- Settings groups appearance, reading, privacy/storage, and about information in
  a bounded, scrollable layout with accessible controls.

### Component model and state ownership

- `ReaderPreferences` is the domain value; `PdfReaderViewModel` owns the current
  optimistic value and `LibraryRepository` owns durable storage.
- Activity-only effects, such as window flags and root theme selection, derive
  from state and do not belong in the repository.
- Rapid slider and toggle changes may coalesce writes, but the last accepted
  state must be the value persisted.

### Local data and privacy

- Preferences and recent-document metadata remain device-local in private
  SharedPreferences.
- Clear history removes recent metadata only. Preference values and source PDFs
  remain unchanged unless a separate control explicitly says otherwise.
- Privacy copy must distinguish metadata retention from SAF file access and from
  annotation save-back.

### Performance and lifecycle

- Preference reads and writes stay off the main thread; visible theme and window
  effects follow state without blocking Compose.
- Keep-awake flags must be cleared when the preference is false and must not leak
  to unrelated windows or outlive the activity.
- Speech-rate changes update the live TTS manager as well as durable preferences.

## Related blueprints

### Required

- None.

### Impact checks

- [Read aloud](read-aloud.md) when speech-rate bounds, defaults, or live
  application change.
- [Document library](document-library.md) when history clearing, retention, or
  privacy descriptions change.
- [PDF rendering](pdf-rendering.md) and annotation blueprints when theme changes
  affect reader contrast or transient overlay visibility.

## Relevant implementation and tests

- `app/src/main/java/com/pdfreader/app/domain/model/LibraryModels.kt` - theme
  modes, defaults, keep-awake flag, and speech-rate value.
- `app/src/main/java/com/pdfreader/app/domain/repository/LibraryRepository.kt` -
  preference persistence boundary and history-clearing operation.
- `app/src/main/java/com/pdfreader/app/data/preferences/SharedPreferencesLibraryRepository.kt` -
  serialization, defaults, rate clamping, and recent-metadata deletion.
- `app/src/main/java/com/pdfreader/app/presentation/mvi/PdfReaderViewModel.kt` -
  initial load, optimistic updates, debounced save, and TTS rate propagation.
- `app/src/main/java/com/pdfreader/app/presentation/ui/SettingsScreen.kt` -
  settings controls and clear-history confirmation.
- `app/src/main/java/com/pdfreader/app/MainActivity.kt` - root theme and window
  flag effects.
- No dedicated preference repository, debounce, process-recreation, theme, or
  window-flag tests currently exist under `app/src/test`.

## Acceptance criteria

- [ ] Theme mode survives recreation and applies consistently to app chrome.
- [ ] Keep screen awake adds and clears the activity flag exactly when requested.
- [ ] Speech rate remains within `0.6f..1.6f`, survives recreation, and updates
  active and future narration.
- [ ] Rapid preference changes persist the final accepted value without blocking
  the main thread.
- [ ] Invalid or unknown stored values fall back to documented defaults.
- [ ] Clear history requires confirmation, removes only recent metadata, and
  leaves preferences and PDF files unchanged.
- [ ] Storage failures are observable or have an explicit recovery policy.

## Remaining gaps

- SharedPreferences commit results are ignored, so failed durable writes are not
  visible to the user or ViewModel.
- A pending debounced preference write is not explicitly flushed before the
  ViewModel scope is cancelled.
- No automated coverage for serialization, defaults, debouncing, recreation,
  theme application, window flags, or clear-history boundaries.
- Reduced-motion, font, and additional accessibility preferences are not
  implemented.
