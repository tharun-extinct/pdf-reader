# Reader Preferences

## Outcome

Apply and persist reader-wide appearance and reading preferences, and provide
clear local-data controls whose effects are immediate, bounded, and private.

## Current verified status

**Status: Partial - code, tests, and Actions history inspected 2026-08-03.**

- System, Light, and Dark theme modes are stored and applied at the activity
  theme root.
- Keep screen awake adds or clears `FLAG_KEEP_SCREEN_ON` while the app activity is
  active.
- Read-aloud rate is clamped to `0.6f..1.6f`, persisted, immediately applied to
  `TtsManager`, and selectable from both Settings and the reader playback panel.
- Four pen and four highlighter colors are edited in the scrollable Settings
  route, persisted in Proto DataStore, and immediately feed the reader palette.
  The former non-scrollable reader editor has been removed.
- Preference updates change immutable UI state immediately and save after a
  250 ms debounce on the IO dispatcher.
- Durable values use the versioned `ReaderDataProto` schema and transactional
  `updateData` writes; legacy SharedPreferences values migrate once.
- Settings explains on-device metadata and confirms before clearing recent
  document names, progress, and bookmarks without deleting PDFs.
- User-facing library, reader, settings, accessibility, and error copy is backed
  by Android string and plural resources; malformed encoding literals have been
  replaced with valid Unicode.
- Android backup rules exclude the shared Proto file and legacy preference file.
  This protects document metadata and URI strings, while also excluding the
  co-located theme, keep-awake, and speech-rate values.
- No recorded GitHub Actions run includes the Proto DataStore commit `7186d8a`
  or the Macrobenchmark workflow, so both remain source-inspected rather than
  CI-verified.
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
- Annotation color editing belongs to Settings; the reader exposes only compact
  palette selection for the active annotation tool.
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

- Preferences, including annotation palettes, and recent-document metadata
  remain device-local in one private Proto DataStore file.
- The shared Proto file and legacy migration source are excluded from Android
  cloud backup and device transfer. If preferences later need cross-device
  restore, they must move to storage with a separate retention policy.
- Clear history removes recent metadata only. Preference values and source PDFs
  remain unchanged unless a separate control explicitly says otherwise.
- Privacy copy must distinguish metadata retention from SAF file access and from
  annotation save-back.

### Performance and lifecycle

- DataStore owns asynchronous preference IO; visible theme and window effects
  follow state without blocking Compose.
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
  modes, defaults, keep-awake flag, speech rate, and palette values.
- `app/src/main/java/com/pdfreader/app/domain/repository/LibraryRepository.kt` -
  preference persistence boundary and history-clearing operation.
- `app/src/main/proto/reader_data.proto` - versioned typed schema.
- `app/src/main/java/com/pdfreader/app/data/preferences/ProtoLibraryRepository.kt` -
  transactional preference updates and domain mappings.
- `app/src/main/java/com/pdfreader/app/data/preferences/ReaderDataStore.kt` -
  application-scoped store, serializer, migration chain, and corruption policy.
- `app/src/main/java/com/pdfreader/app/data/preferences/ReaderDataMigrations.kt` -
  legacy preference import and schema-version normalization.
- `app/src/main/java/com/pdfreader/app/presentation/mvi/PdfReaderViewModel.kt` -
  initial load, optimistic updates, debounced save, and TTS rate propagation.
- `app/src/main/java/com/pdfreader/app/presentation/ui/SettingsScreen.kt` -
  settings controls, scrollable palette editor, and clear-history confirmation.
- `app/src/main/res/values/strings.xml` - localized UI, accessibility, plural,
  formatting, and user-visible error resources.
- `app/src/main/res/xml/backup_rules.xml` and
  `app/src/main/res/xml/data_extraction_rules.xml` - current and legacy store
  exclusions for cloud backup and device transfer.
- `app/src/main/java/com/pdfreader/app/MainActivity.kt` - root theme and window
  flag effects.
- `app/src/test/java/com/pdfreader/app/data/preferences/ProtoLibraryRepositoryTest.kt` -
  preference durability alongside independent history clearing.
- `app/src/test/java/com/pdfreader/app/data/preferences/ReaderDataMigrationsTest.kt` -
  legacy values, schema defaults, and migration idempotence.
- `macrobenchmark/src/main/java/com/pdfreader/macrobenchmark/NoxReaderMacrobenchmark.kt` -
  Settings scrolling frame timing in separate benchmark CI.

## Acceptance criteria

- [ ] Theme mode survives recreation and applies consistently to app chrome.
- [ ] Keep screen awake adds and clears the activity flag exactly when requested.
- [ ] Speech rate remains within `0.6f..1.6f`, survives recreation, and updates
  active and future narration.
- [ ] Valid pen and highlighter palettes survive recreation and update reader
  selection chips without a reader-window editor.
- [ ] Rapid preference changes persist the final accepted value without blocking
  the main thread.
- [ ] Invalid or unknown stored values fall back to documented defaults.
- [ ] Legacy preferences migrate exactly once, and every future semantic schema
  change has an idempotent version-to-version migration test.
- [ ] Clear history requires confirmation, removes only recent metadata, and
  leaves preferences and PDF files unchanged.
- [ ] UI and accessibility copy comes from Android resources and remains free of
  malformed character-encoding sequences.
- [ ] Backup and device transfer exclude the shared store while it contains
  document history and persisted URI strings.
- [ ] Storage failures are observable or have an explicit recovery policy.

## Remaining gaps

- CI has not yet compiled or executed the DataStore tests or Macrobenchmarks;
  the latest recorded Android Build predates commit `7186d8a`.
- DataStore write failures are not yet surfaced by the ViewModel as recoverable
  UI state.
- A pending debounced preference write is not explicitly flushed before the
  ViewModel scope is cancelled.
- No automated coverage for ViewModel debouncing, recreation, theme application,
  or window flags.
- Reduced-motion, font, and additional accessibility preferences are not
  implemented.
