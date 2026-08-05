# Document Library

## Outcome

Let a user choose a PDF, reopen recent documents, resume at a valid page, and
manage page bookmarks without moving the source file or confusing metadata with
document contents.

## Current verified status

**Status: Partial - code, tests, and Actions history inspected 2026-08-03.**

- The Android document picker is restricted to `application/pdf` and the app
  attempts to persist read/write access, falling back to read access.
- Navigation to the reader occurs only after `isPdfLoaded` becomes true.
- Recent metadata is keyed by the SAF URI, sorted by last-opened time, and capped
  at 20 entries.
- History and bookmarks use transactional Proto DataStore updates. Existing
  SharedPreferences JSON is imported once before schema version 1 is activated.
- Opening a recent item clamps and restores its last page; page changes are
  saved after a 500 ms debounce.
- Bookmarks are page-index sets stored with each recent document.
- Android cloud backup and device transfer exclude both the Proto DataStore file
  and its legacy SharedPreferences migration source, so document names, history,
  bookmarks, and persisted URI strings are not restored without valid SAF
  grants.
- The library renders loading, empty, opening, populated, and recoverable error
  states; clearing history is exposed through Settings.
- No recorded GitHub Actions run includes the Proto DataStore commit `7186d8a`
  or a later project revision, so the repository and migration tests remain
  source-inspected rather than CI-verified.

## Architecture dependencies

- [Information architecture](../.github/architecture.md#information-architecture)
- [Component model and state ownership](../.github/architecture.md#component-model-and-state-ownership)
- [Local data and privacy](../.github/architecture.md#local-data-and-privacy)
- [Performance and lifecycle](../.github/architecture.md#performance-and-lifecycle)

## Feature-specific implications

### Information architecture

- A picker result starts an open request; it must not navigate directly.
- Back from the reader closes the active document before returning to the
  bookshelf.
- The most recent item is the Continue reading target; all saved entries remain
  available in the recent list.

### Component model and state ownership

- `PdfReaderViewModel` owns library loading, open progress, the active document,
  current page, and bookmarks in immutable state.
- `LibraryRepository` owns only device-local metadata. The PDF engine owns the
  open document, and the SAF URI remains its durable identity.
- Repository mutations transform the current Proto root atomically, so progress,
  bookmark, and history updates cannot overwrite one another with stale reads.
- Opening a URI merges existing bookmarks and clamps stored progress against the
  newly observed page count.

### Local data and privacy

- The library stores URI, display title, page count, last page, last-opened time,
  and bookmarked page indices; it does not store PDF bytes.
- Clearing history removes those metadata records only. It must never delete or
  rewrite a selected PDF.
- Backup and device-transfer rules exclude the current and legacy metadata
  stores because stored URI strings do not recreate installation-scoped SAF
  grants.
- A persisted read grant may keep a recent document openable when write access is
  unavailable; save-back capability is a separate persistence concern.

### Performance and lifecycle

- Provider access stays off the main thread; the application-scoped DataStore
  serializes metadata IO and completes updates only after durable persistence.
- Rapid page changes coalesce progress writes. Close snapshots the last observed
  page, schedules its metadata write, and then releases active reader state.
- Replacement, close, and failure paths must release the previous document and
  descriptor without erasing unrelated recent entries.

## Related blueprints

### Required

- None.

### Impact checks

- [Annotation persistence](annotation-persistence.md) when URI permissions,
  close behavior, read-only sources, or save-back ownership changes.
- [Reader preferences](reader-preferences.md) when history clearing, privacy
  copy, or metadata retention controls change.
- [PDF rendering](pdf-rendering.md) when restored page indices or reader
  navigation alter render startup.

## Relevant implementation and tests

- `app/src/main/java/com/pdfreader/app/MainActivity.kt` - document picker,
  persisted URI grants, and state-driven navigation.
- `app/src/main/java/com/pdfreader/app/domain/model/LibraryModels.kt` - recent
  document metadata and progress calculation.
- `app/src/main/java/com/pdfreader/app/domain/repository/LibraryRepository.kt` -
  metadata persistence boundary.
- `app/src/main/proto/reader_data.proto` - versioned history and preference
  schema.
- `app/src/main/java/com/pdfreader/app/data/preferences/ProtoLibraryRepository.kt` -
  transactional ordering, 20-item limit, bookmarks, progress, and domain maps.
- `app/src/main/java/com/pdfreader/app/data/preferences/ReaderDataMigrations.kt` -
  legacy JSON import and ordered schema upgrade.
- `app/src/main/res/xml/backup_rules.xml` and
  `app/src/main/res/xml/data_extraction_rules.xml` - exclude current and legacy
  document metadata from cloud backup and device transfer.
- `app/src/main/java/com/pdfreader/app/presentation/mvi/PdfReaderViewModel.kt` -
  open, resume, progress debounce, bookmark, clear, and close flows.
- `app/src/main/java/com/pdfreader/app/presentation/ui/BookshelfScreen.kt` -
  loading, empty, opening, populated, and error presentation.
- `app/src/test/java/com/pdfreader/app/data/preferences/ProtoLibraryRepositoryTest.kt` -
  bounded history, bookmark, palette preference, and clear-history transactions.
- `app/src/test/java/com/pdfreader/app/data/preferences/ReaderDataMigrationsTest.kt` -
  legacy history conversion, defaults, and idempotence.
- `macrobenchmark/src/main/java/com/pdfreader/macrobenchmark/NoxReaderMacrobenchmark.kt` -
  cold startup through the library-ready signal; executed only in benchmark CI
  or on a connected physical device.

## Acceptance criteria

- [x] Successful picker and recent-item opens navigate only after the engine has
  reported a readable document.
- [x] Reopening restores the last valid page and the correct bookmark set.
- [x] Recent entries remain URI-unique, newest-first, and capped at 20.
- [x] Loading, empty, opening, populated, revoked-access, and provider-error
  states have recoverable UI behavior.
- [x] Page progress and bookmarks survive process recreation without UI-thread
  disk access.
- [x] Existing SharedPreferences history and bookmarks migrate exactly once and
  future schema versions preserve all still-supported fields.
- [x] Clearing history removes metadata but leaves every source PDF untouched.
- [x] Cloud backup and device transfer do not restore recent-document metadata
  or persisted URI strings without corresponding SAF grants.
- [ ] Read-only access remains useful for reading and produces an explicit save
  limitation when a write is requested.

## Remaining gaps

- CI has not yet compiled or executed the Proto repository and migration tests;
  the latest recorded Android Build predates commit `7186d8a`.
- No ViewModel, navigation, or SAF permission-revocation coverage.
- Back closes a document without confirming pending annotation changes; any fix
  must be coordinated with annotation persistence.
- Android instrumentation does not yet verify the complete on-device
  SharedPreferences cleanup step after a successful import.
- Revoked or moved recent-document URIs have no dedicated repair or removal flow.
