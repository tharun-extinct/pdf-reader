# NoxReader Architecture

## Product vision

NoxReader is a focused Android PDF reader for people who want a calm library, fast page navigation, lightweight annotation, and durable changes without uploading documents to an application-owned cloud.

The experience should feel:

- **Quiet:** reading content dominates; controls stay compact and contextual.
- **Trustworthy:** saves are explicit, progress is visible, and destructive actions require confirmation or a clear selected target.
- **Local-first:** files remain at the Storage Access Framework (SAF) location selected by the user; only library metadata and preferences are stored by the app.
- **Responsive:** page rendering, text extraction, PDF mutation, and provider I/O never block Compose.
- **Accessible:** controls use meaningful labels, at least 48 dp touch targets where practical, system-aware color contrast, and non-color selected states.

## Information architecture

```mermaid
flowchart LR
    Library["Library / Bookshelf"] -->|"Open recent or choose PDF"| Reader["Reader"]
    Library --> Settings["Settings"]
    Reader -->|"Back / close document"| Library
    Settings -->|"Back"| Library
```

Navigation is implemented by a Compose `NavHost` in `MainActivity`:

| Route | Purpose | Primary actions |
|---|---|---|
| `bookshelf` | Home and document history | Continue reading, open a recent PDF, choose a PDF, open Settings |
| `reader` | Read and annotate the active document | Change page, zoom, bookmark, read aloud, annotate, save |
| `settings` | App-level preferences and local-data controls | Theme, keep screen awake, speech rate, clear recent history |

Opening a document is state-driven. The bookshelf observes `isPdfLoaded` and navigates to the reader only after the file has opened successfully.

## Feature experience ownership

User-visible behavior belongs to the task-routed feature blueprints rather than
this shared contract. Load the manifest entry for the requested capability:

- [Document library](../feature-blueprints/document-library.md) owns opening,
  recents, resume position, bookmarks, and library states.
- [PDF rendering](../feature-blueprints/pdf-rendering.md) owns the reader canvas,
  paging, zoom, and render feedback.
- [Read aloud](../feature-blueprints/read-aloud.md) owns playback controls,
  chunking, synchronized highlighting, and TTS errors.
- [Reader preferences](../feature-blueprints/reader-preferences.md) owns settings
  behavior, theme selection, keep-awake, speech rate, and history clearing.
- Annotation interaction and save behavior belong to their corresponding
  blueprints in the [feature manifest](../feature-blueprints/README.md).

## Visual system

### Color

The Material 3 palette is defined in `presentation/theme/Color.kt`.

- Light surfaces use a warm paper background (`#FAF9F7`) and white lowest containers.
- Primary navy (`#03192E`) anchors high-emphasis actions and selected states.
- Tertiary brown-gold supports warmth without competing with document content.
- Dark mode uses near-black blue surfaces and the corresponding light primary.
- Material semantic colors must be used instead of hard-coded screen colors, except annotation colors and purpose-specific overlay colors.

### Typography

The hierarchy combines an editorial serif voice with a utilitarian sans-serif UI:

- Serif: display titles, headings, and long-form reading styles.
- Sans serif: buttons, labels, metadata, and settings.
- Current code uses Android system serif/sans-serif families for crash-free offline startup. Source Serif 4 and Inter are token names and future bundled-font candidates, not downloaded runtime dependencies.

### Spacing and layout

Spacing tokens live in `NoxReaderSpacing`:

| Token | Value | Use |
|---|---:|---|
| Base | 8 dp | Small rhythm unit |
| Mobile margin | 20 dp | Standard phone content inset |
| Gutter | 24 dp | Section and column separation |
| Desktop margin | 64 dp | Future larger-window layouts |
| Reading max width | 720 dp | Comfortable content width |

Feature blueprints own screen-specific width caps and component arrangements.
Interactive controls use at least 48 dp touch targets where practical and must
remain clear of floating actions and system insets.

### Motion

Motion communicates state rather than decoration. Feature blueprints define the
specific transition and its trigger. Transitions must remain short,
non-essential, and safe to disable when reduced-motion support is added.

## Component model and state ownership

NoxReader uses MVI-style unidirectional state with Clean Architecture boundaries.

| Layer | Responsibility | Main components |
|---|---|---|
| Presentation | Compose UI, immutable state, intents, gesture interpretation, optimistic overlays | `BookshelfScreen`, `PdfReaderScreen`, `SettingsScreen`, `PdfReaderViewModel`, `PdfReaderState` |
| Domain | Stable engine, saving, sync, library, and TTS contracts/models | `PdfEngine`, `PdfAnnotationSaver`, `PdfSyncManager`, `LibraryRepository`, library models |
| Data | PDFium, PDFBox Android, SAF, and Proto DataStore implementations | `PdfiumEngine`, `PdfAnnotationWriterImpl`, `SafPdfSyncManager`, `ProtoLibraryRepository` |

Dependencies are manually assembled in `MainActivity`. Shared state is owned by one activity-scoped `PdfReaderViewModel` and exposed through `StateFlow`.

```mermaid
flowchart LR
    UI["Compose screens and overlays"] -->|"PdfReaderIntent"| VM["PdfReaderViewModel"]
    VM -->|"StateFlow<PdfReaderState>"| UI
    VM --> Engine["PdfEngine / PdfiumEngine"]
    VM --> Saver["PdfAnnotationSaver / PDFBox"]
    VM --> Sync["PdfSyncManager / SAF"]
    VM --> Library["LibraryRepository / Proto DataStore"]
    VM --> TTS["Android TextToSpeech"]
    Engine --> PDFium["PDFium page bitmap"]
    Engine --> PDFBoxRead["PDFBox text and highlight parsing"]
    Saver --> PDFBoxWrite["Editable PDF annotations"]
    PDFBoxWrite --> Sync
    Sync --> Engine
```

## Document and annotation pipeline

PDFBox compatibility is pinned to `com.tom-roush:pdfbox-android:2.0.27.0`. This Android PDFBox 2.x fork does not expose every newer upstream API. Ink uses `PDAnnotationMarkup` with `/Subtype /Ink`; text notes and markup appearances must use APIs verified against the pinned AAR.

### Coordinate system

Domain and UI annotation geometry uses normalized `0..1` display-space
coordinates with a top-left origin. Durable PDF geometry uses PDF points and
the page box used for rendering, normally CropBox with MediaBox fallback.

`PdfCoordinateMapper` is the single conversion boundary. It accounts for page
dimensions and right-angle rotation; feature code must not introduce ad-hoc DPI
scaling, Y-axis flips, or MediaBox-only conversions. For an unrotated page of
width `W` and height `H`, the shared mapping is `pdfX = x * W` and
`pdfY = (1 - y) * H`.

The same bidirectional transform applies to rendering bounds, pointer input,
text geometry, hit-testing, annotation rectangles, quad points, ink paths, and
note anchors. Portrait, landscape, cropped, rotated, and non-standard pages
must remain covered by mapper tests.

Freehand stroke width is stored as a fraction of displayed page width. Compose
multiplies it by the fitted page width for preview and overlays, while
`PdfCoordinateMapper` converts it to PDF points using the rotated displayed-page
width. Screen pixels must never be written directly as `/Ink` border width.

### Rendering and overlay model

PDFium owns the static page-bitmap layer. Compose owns pending annotations,
gesture previews, selection feedback, handles, and contextual actions. Pointer
interaction updates optimistic MVI state without mutating PDFBox or rerendering
the base page.

Rendering, overlays, and hit-testing share the fitted page `contentBounds`
transform. The current implementation renders one full-page bitmap; viewport
tiles, bounded bitmap eviction, obsolete-request cancellation, and complete
zoom panning remain planned and must not be described as shipped.

### Commit pipeline

1. The Android document picker returns a persistable SAF URI.
2. The ViewModel reads the file on `Dispatchers.IO`, retaining raw bytes for PDFBox and a file descriptor for PDFium.
3. PDFium renders static page bitmaps. PDFBox supplies positioned word/character geometry and embedded highlight metadata.
4. Compose records pending annotation changes in immutable MVI state.
5. Save snapshots pending state and writes a temporary PDF off the main thread.
6. The sync layer copies the result to the original SAF URI.
7. The engine reopens the synchronized document and increments `renderRevision`.
8. Only successfully persisted overlays and deletions are cleared.

### Text geometry and highlighting

Text extraction retains word bounds and per-character bounds. `TextHighlightSelector` resolves drag endpoints into reading-order cursors and creates a continuous range:

- start cursor to end of the first line;
- all complete intervening lines;
- beginning of the final line through the end cursor.

Reverse drags normalize to the same result. Preview and persistence use identical normalized rectangles.

### Editable annotation output

All saves preserve annotations as standard editable PDF `/Annots`: text ranges
use `/Highlight` with `/QuadPoints`, freehand strokes use `/Ink`, and notes use
`/Text`. The reader exposes no save-mode selector and never paints newly created
annotations irreversibly into page `/Contents`.

The annotation union rectangle is metadata only and must never be rendered as a solid highlight because it can obscure text between selected lines.

### Failure handling and invalidation

Opening may succeed while save-back fails because a SAF provider is read-only
or unavailable. A failed write, sync, or reopen keeps pending overlays,
deletions, and note contents available for retry and exposes an actionable
error.

A successful reopen clears page-scoped text and embedded-highlight caches and
increments `renderRevision` before persisted optimistic items are removed.
Future tile caches must include document version, page, viewport, zoom, and tile
identity and must invalidate affected entries only after commit succeeds.

## Local data and privacy

`ProtoLibraryRepository` stores one private `ReaderDataProto` containing only:

- document URI and display title;
- page count and last page;
- last-opened timestamp;
- bookmarked page indices;
- theme, keep-awake, speech-rate, and annotation-palette preferences.

The root message carries an explicit `schema_version`. Protobuf field numbers
must never be reused, and compatible fields are added with safe defaults.
Semantic changes require an ordered, idempotent `DataMigration` that upgrades
one schema version at a time before normal reads or writes occur.

Version 1 first imports the legacy `nox_reader_preferences` SharedPreferences
keys, then normalizes schema defaults and marks the migration complete. A
completed version prevents a cleanup retry from overwriting newer DataStore
state. All normal mutations use `DataStore.updateData`, making each
read-transform-write operation transactional and durable before it returns.
Malformed Proto data is replaced with versioned defaults; source PDFs remain
unaffected.

The Proto file and its legacy SharedPreferences migration source contain
document names, reading history, bookmarks, and SAF URI strings. They must be
excluded from Android cloud backup and device-to-device transfer. Persisted URI
grants are provider- and installation-scoped capabilities, not portable account
data; restoring their string values without valid grants would create unusable
or misleading recent-document entries. Because preferences currently share the
same Proto root, theme, keep-awake, speech-rate, and annotation-palette values
are excluded with the sensitive metadata until storage is split by retention
policy.

PDF contents stay at their selected SAF location. The file provider determines whether save-back is available; read-only sources may open but cannot be updated.

## Performance and lifecycle

- Rendering, text extraction, PDFBox parsing/writing, SAF I/O, and local metadata
  access never block Compose. Proto DataStore owns its application-scoped IO and
  serializes concurrent metadata updates.
- PDFBox uses `MemoryUsageSetting.setupMixed(50 MiB)`.
- Render callbacks keep bitmap objects out of `PdfReaderState`.
- Page text and embedded highlights are cached per open document.
- PDF documents, descriptors, TTS resources, and temporary save files must be released on replacement, close, completion, and failure paths.
- Saving never clears optimistic state before sync and reopen succeed.

## Validation and delivery

- JVM test sources cover coordinate mapping, overlapping-highlight and ink hit
  testing, contiguous/reverse text selection, and editable PDF annotation output
  under `app/src/test`. Proto repository and migration tests
  cover bounded history, bookmarks, preferences, defaults, legacy JSON mapping,
  and migration idempotence.
- GitHub Actions is the build and test authority; repository guidance intentionally forbids local Gradle execution.
- `.github/workflows/build.yml` runs debug JVM tests and builds a release APK for
  pushes and pull requests targeting `master`; lint is not yet an explicit task.
- `.github/workflows/macrobenchmark.yml` runs cold-start and Settings frame
  benchmarks on a fixed API 34 managed emulator and uploads results and traces.
  Emulator results are regression signals, not device-representative release
  numbers; authoritative comparisons run the same tests on controlled physical
  hardware.
- `.github/workflows/docs-check.yml` is manually dispatched and validates local
  Markdown links and anchors, the required feature-blueprint sections, and
  complete task-router coverage without invoking the Android toolchain.
- `.github/workflows/gh-release.yml` publishes release APKs for pushes to `main` or `feature`. Note that `main` and `master` are distinct branch names; workflow changes must keep branch policy explicit.
- `.github/workflows/gh-release-gptoss.yml` publishes the separate `gptoss` branch release.

## Cross-cutting gaps

- Device and Compose UI coverage does not yet verify accessibility, rotation,
  process recreation, or provider-failure recovery across feature domains.
- Resource ownership and cancellation need stress coverage for rapid document
  replacement, paging, saving, and activity destruction.
- Supported PDF mutations need a maintained provider and external-viewer
  compatibility matrix.
- Product typography still relies on system serif and sans-serif families;
  bundling fonts would be a cross-screen visual-system change.

## Feature blueprints

[`feature-blueprints/README.md`](../feature-blueprints/README.md) is the task
router for progressive agent context loading. Shared contracts remain
authoritative here; each blueprint states how they constrain that feature.

| Blueprint | Current scope |
|---|---|
| [`pdf-rendering.md`](../feature-blueprints/pdf-rendering.md) | Page rendering, transforms, zoom, caching, and lifecycle |
| [`annotation-persistence.md`](../feature-blueprints/annotation-persistence.md) | Editable annotation save, SAF sync, reopen, and failure recovery |
| [`text-highlighting.md`](../feature-blueprints/text-highlighting.md) | Text selection, embedded-highlight interaction, deletion, and geometry |
| [`freehand-annotation.md`](../feature-blueprints/freehand-annotation.md) | Pen/freehand strokes, erasing, ink output, and reselection |
| [`text-notes.md`](../feature-blueprints/text-notes.md) | Note placement, editing, display, and persistence |
| [`document-library.md`](../feature-blueprints/document-library.md) | PDF selection, persisted access, recents, progress, and bookmarks |
| [`read-aloud.md`](../feature-blueprints/read-aloud.md) | TTS lifecycle, chunking, synchronized highlighting, and playback controls |
| [`reader-preferences.md`](../feature-blueprints/reader-preferences.md) | Theme, screen-awake, speech-rate, privacy, and history controls |
