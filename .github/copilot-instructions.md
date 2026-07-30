---
applyTo: "**/*"
name: NoxReader Android App
description: Product, architecture, UI, and delivery context for the Android PDF reader
---

# NoxReader Agent Context

## Start here

Read `.github/design.md` before product, UI, architecture, PDF pipeline, or persistence work. It is the current source of truth. The files under `implementation-files/` are historical feature contracts and audits; use them for rationale, but prefer current code and `design.md` when they differ.

NoxReader is a local-first Android PDF reader with:

- a bookshelf for recent documents, progress, and bookmarks;
- horizontal reading, pinch zoom, text selection, and Android TTS;
- pen, text-aware highlighter, eraser, text notes, and palette customization;
- editable or flattened annotation save-back through Android SAF;
- System, Light, and Dark themes plus keep-awake and speech-rate preferences.

Do not describe unfinished work as shipped. Tile/viewport rendering, bounded bitmap eviction, zoom panning, page thumbnails, broad external-viewer validation, and automated device UI coverage remain open.

## Stack and versions

- Kotlin 1.9.22, Java 17
- Android Gradle Plugin 8.3.0; Gradle wrapper 8.4
- minSdk 26, target/compileSdk 34
- Jetpack Compose with BOM `2024.02.00` and Material 3
- Compose Navigation 2.7.7
- PDFium Android 1.9.0 for bitmap rendering
- `com.tom-roush:pdfbox-android:2.0.27.0` for text geometry, embedded annotations, and PDF mutation
- Coroutines/StateFlow, Android TextToSpeech, SAF, and SharedPreferences

PDFBox Android is a 2.x fork and does not expose every newer upstream PDFBox API. Verify methods against the pinned AAR before using them. Ink annotations use `PDAnnotationMarkup` with `/Subtype /Ink`; do not introduce the newer `PDAnnotationInk` class.

## Architecture contract

NoxReader uses Clean Architecture boundaries with an MVI-style presentation layer:

```text
Compose screens -> PdfReaderIntent -> PdfReaderViewModel -> domain contracts -> data implementations
                <- StateFlow<PdfReaderState> <-
```

- **Presentation:** `presentation/ui`, `presentation/mvi`, and `presentation/theme`.
- **Domain:** engine, saver, sync, library, and TTS contracts/models.
- **Data:** PDFium/PDFBox, SAF, and SharedPreferences implementations.
- **Composition root:** `MainActivity` manually creates dependencies and owns the shared activity-scoped ViewModel.

Keep domain APIs independent of concrete PDFBox/PDFium types except for unavoidable Android boundary types already in existing contracts. Presentation must not parse a PDF or perform storage I/O inside a pointer callback.

## State and navigation

Routes are `bookshelf`, `reader`, and `settings`.

- The bookshelf navigates to the reader only after `isPdfLoaded` becomes true.
- Opening a recent document restores its last valid page and bookmarks.
- Page changes persist reading progress asynchronously.
- Back from the reader dispatches `ClosePdf` before popping navigation.
- `PdfReaderState` stays immutable and lightweight; rendered bitmaps are returned by callbacks rather than stored in StateFlow.
- UI changes flow through `PdfReaderIntent`; avoid screen-owned shadow state for durable product behavior.

## UI and UX rules

- Preserve the calm, editorial visual system in `presentation/theme`: serif headings, sans-serif controls, warm paper surfaces, navy emphasis, rounded containers, and restrained elevation.
- Use Material semantic colors and typography tokens. Hard-coded colors are reserved for annotation palettes and purpose-specific overlays.
- Use `NoxReaderTheme.spacing` and current width caps (760 dp library, 720 dp settings) instead of scattered layout constants for new top-level content.
- Keep document content visually dominant and controls contextual. Reader controls belong in the compact top bar or floating bottom tool system.
- Every stateful operation needs visible feedback and a recoverable failure state. Never remove library content while a new document is opening.
- Use at least 48 dp touch targets where practical and meaningful `contentDescription`/semantics for icon-only actions. Do not rely on color alone for selected state.
- Confirm destructive collection-level actions. Targeted highlight deletion is allowed only after an explicit selection is visible.
- Keep UI copy direct and specific: say what will change, where data is stored, and whether the source PDF is affected.

## Document pipeline

1. The document picker returns a persistable SAF URI.
2. `PdfReaderViewModel` reads on `Dispatchers.IO`.
3. `PdfiumEngine` uses PDFium for static bitmaps and PDFBox for text geometry and embedded highlight metadata.
4. Compose renders pending annotation and selection overlays.
5. Save writes a temporary PDF, syncs it to the original URI, reopens it, increments `renderRevision`, and only then clears successfully persisted state.

All UI annotation coordinates are normalized `0..1` display-space values with a top-left origin. `data/pdfbox/PdfCoordinateMapper.kt` is the only PDFBox coordinate conversion boundary for CropBox and right-angle page rotation. Do not add ad-hoc Y flips, DPI formulas, or MediaBox-only conversions.

### Text and highlights

- Text extraction retains word bounds and per-character geometry.
- `TextHighlightSelector` creates a continuous reading-order range across wrapped lines and normalizes reverse drags.
- Preview and persisted text highlights use the same normalized rectangles.
- Embedded highlights are loaded and cached before hit testing.
- `HighlightHitTester` selects the smallest overlapping target.
- Selection alone never mutates the PDF.
- Highlight appearances paint individual quads with opacity. Never fill the union rectangle.

### Saving

- **Editable:** keeps new annotation objects in `/Annots`.
- **Flattened:** paints only newly created supported marks into page contents and removes only those new annotation entries.
- Preserve unrelated existing annotations, forms, and signatures.
- Flattening is irreversible; keep the UI mode explicit.
- A failed write, sync, or reopen must retain pending overlays/deletions for retry.

## Concurrency and resources

- Never block the main thread with rendering, parsing, saving, SAF access, or preference disk I/O.
- Keep PDFBox at `MemoryUsageSetting.setupMixed(50 MiB)` and use the supported `InputStream` load overload.
- Bound allocations and release PDFium/PDFBox documents, file descriptors, bitmaps, temporary files, and TTS resources on close and failure paths.
- Cache text boxes and embedded highlights only for the active document; invalidate them after reopen.
- Keep provider access resilient: some SAF sources are read-only. Opening may succeed while save-back fails.

## Tests and delivery

- Do **not** run Gradle locally. GitHub Actions is the build/test authority for this repository.
- JVM tests live under `app/src/test`.
- Maintain `PdfCoordinateMapperTest` for CropBox/rotation mapping.
- Maintain `HighlightHitTesterTest` for overlap and smallest-target selection.
- Maintain `TextHighlightSelectorTest` for wrapped lines and reverse drags.
- `.github/workflows/build.yml` builds release APKs for pushes and pull requests to `master`.
- `.github/workflows/gh-release.yml` publishes releases for `main` and `feature`; `.github/workflows/gh-release-gptoss.yml` handles `gptoss`.
- Treat `main` and `master` as different branches. Do not silently change workflow triggers or claim a release runs on a branch it does not list.
- Preserve unrelated worktree changes. Stage only task files, inspect staged changes, and push only when the user or repository workflow authorizes it.

## Documentation discipline

When behavior, navigation, visual tokens, dependencies, build branches, or architecture change:

1. update `.github/design.md`;
2. update this file if agent guidance changed;
3. update `README.md` if users or contributors are affected;
4. keep statements limited to behavior present in code or verified CI.




---
applyTo: "**/features/"
name: features folder update
description: Product, architecture, UI, and delivery context for the Android PDF reader
---

