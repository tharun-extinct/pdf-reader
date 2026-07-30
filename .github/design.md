# PDF Reader Android App — Design & Architecture

## System overview

This is a low-latency Android PDF reader built for reading and annotation. PDFium owns static page rasterization; PDFBox owns text geometry, embedded annotation metadata, and durable PDF changes. Compose owns all interaction and optimistic UI feedback.

The data layer targets `com.tom-roush:pdfbox-android:2.0.27.0`. Its annotation API is an Android-specific PDFBox 2.x surface: freehand marks are represented by `PDAnnotationMarkup` with `/Subtype /Ink`, and opacity/ink paths use the methods exposed by that fork. Newer upstream-only annotation classes must not be introduced without first changing and validating the dependency.

## Technology and boundaries

| Layer | Responsibility | Main components |
|---|---|---|
| Presentation | Compose UI, MVI intents/state, gesture handling, overlays | `PdfReaderScreen`, `PdfReaderViewModel`, `PdfReaderState` |
| Domain | Stable contracts for rendering, saving, and sync | `PdfEngine`, `PdfAnnotationSaver`, `PdfSyncManager` |
| Data | PDFium/PDFBox/SAF integration | `PdfiumEngine`, `PdfAnnotationWriterImpl`, `SafPdfSyncManager` |

```mermaid
flowchart LR
  UI["Compose base + overlay + selection"] -->|"PdfReaderIntent"| VM["PdfReaderViewModel"]
  VM --> ENGINE["PdfEngine / PdfiumEngine"]
  VM --> SAVER["PdfAnnotationSaver / PDFBox"]
  VM --> SYNC["PdfSyncManager / SAF"]
  ENGINE --> PDFIUM["PDFium bitmap + embedded highlight cache"]
  ENGINE --> PDFBOX_READ["PDFBox text/highlight parsing"]
  SAVER --> PDFBOX_WRITE["Editable or flattened PDF output"]
  PDFBOX_WRITE --> SYNC
  SYNC --> ENGINE
```

## Annotation pipeline

1. PDFium renders the static page bitmap. Compose draws in-progress pen/highlighter input, session annotations, selection bounds, and menus above it.
2. UI stores positions as normalized display-space values with a top-left origin.
3. `PdfCoordinateMapper` maps normalized coordinates to/from PDFBox CropBox coordinates, including right-angle page rotation. All PDFBox annotation creation and embedded-highlight loading use it.
4. On Save, the ViewModel snapshots pending state and performs PDFBox writing plus SAF sync on `Dispatchers.IO`.
5. After sync and successful reopen, `renderRevision` invalidates rendered pages and state caches. Only then are session overlays/deletion intents cleared.

### Text highlighter interaction

Text extraction retains both word bounds and per-character bounds. The highlighter resolves drag endpoints into reading-order cursors and creates one continuous rectangle per affected line: the range from the starting cursor to the end of the first line, complete intervening lines, and the beginning of the final line through the ending cursor. Reverse drags are normalized to the same reading-order range. The preview and persisted annotation use the same normalized rectangles.

### Save modes

- **Editable:** PDFBox keeps `/Annots` entries for highlights, ink, and text notes. New text highlights include a normal appearance made from their selected quads with the configured opacity; the annotation union rectangle is metadata only and is never painted as a solid box.
- **Flattened:** PDFBox appends supported new annotation marks to page `/Contents`, then removes only those new annotation entries. Existing annotations remain intact.

## Future enhancements

- Local NPU-based voice model for higher-quality Read Aloud (for example, Piper TTS via ONNX/TFLite).

## Embedded highlight selection

When a page becomes visible, `PdfiumEngine.getEmbeddedHighlights()` reads PDFBox highlight quads off the main thread and caches normalized rectangles. `HighlightHitTester` chooses the smallest matching highlight on an idle-tool tap. `SelectedHighlightOverlay` renders a dashed blue union bound and an in-window Delete action. Deletion updates state immediately and is persisted in the next save.

## Performance and lifecycle

- PDF rendering, PDFBox parsing/saving, text/highlight extraction, and SAF I/O run off the main thread.
- PDFBox document loading uses `MemoryUsageSetting.setupMixed(50 MiB)`.
- Annotation saves load the in-memory PDF through PDFBox's `InputStream` plus `MemoryUsageSetting` overload; this preserves the mixed-memory limit while remaining compatible with the Android fork.
- PDF documents and temporary save files are closed/deleted during close, replacement, save completion, and failure paths.
- The current page renderer is still a full-page bitmap. Tile/viewport rendering, bitmap eviction, and request cancellation are planned work; do not describe them as implemented.

## Verification and delivery

- Coordinate round-trip and overlap hit-testing JVM tests live in `app/src/test`.
- `TextHighlightSelectorTest` covers contiguous wrapped-line ranges and reverse drag direction.
- GitHub Actions is the only build/test environment; do not run local Gradle.
- `.github/workflows/gh-release.yml` generates releases on `main` and `feature` pushes.

## Feature mapping

| Feature file | Design sections | Current scope |
|---|---|---|
| `implementation-files/feature01.md` | Annotation pipeline; Performance and lifecycle | Rendering/coordinate/overlay foundation |
| `implementation-files/feature02.md` | Annotation pipeline; Save modes | Persistent editable and flattened annotations |
| `implementation-files/feature03.md` | Embedded highlight selection | Existing-highlight selection and deletion |
