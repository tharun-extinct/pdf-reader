---
applyTo: "**/*"
name: PDF Reader Android App
description: Agent context for the Android PDF reader and annotation pipeline
---

# PDF Reader — Agent Context

## Product and stack

Android PDF reader focused on fast reading, pen/highlight/text-note annotation, persistent PDF output, TTS, and SAF sync.

- Kotlin + Jetpack Compose
- MVI + Clean Architecture; `Presentation → Domain → Data`
- PDFium-Android for page rasterization; Apache PDFBox Android for text geometry and PDF mutation
- Coroutines/Flow for background work; Android system TTS; SAF for persistence/sync

PDFBox compatibility is pinned to `com.tom-roush:pdfbox-android:2.0.27.0`. This Android fork does not expose every class from newer upstream PDFBox releases; consult its 2.x API before adding PDF annotation code.

Read `.github/design.md` before structural work. It is the architecture source of truth; `implementation-files/feature01.md`, `feature02.md`, and `feature03.md` are feature-level contracts and status records.

## Current architecture contract

1. Compose renders a PDFium bitmap as the base layer and draws unsaved annotations/selection UI as overlays.
2. UI positions are normalized (`0..1`, top-left origin). `data/pdfbox/PdfCoordinateMapper.kt` is the shared PDFBox conversion boundary for CropBox and 90° page rotation.
3. PDFBox reads word boxes plus per-character geometry and embedded highlights off the main thread. `PdfiumEngine` caches both per page; character geometry is required for line-contiguous highlighter selection.
4. Save snapshots MVI state, writes or flattens annotations on `Dispatchers.IO`, syncs through SAF, reopens the document, increments `renderRevision`, then clears only successfully persisted state.
5. Existing embedded highlights are cached before pointer hit-testing. Selection is state-driven; selection alone never mutates the PDF.

## Engineering constraints

- Never block the main thread with PDF I/O, parsing, rendering, saving, or sync.
- Keep domain APIs free of PDFBox/PDFium implementation details. Presentation must not query PDFBox inside a pointer callback.
- Use `PdfCoordinateMapper`; do not add ad-hoc Y flips, DPI formulas, or MediaBox-only mapping.
- Preserve existing annotations, forms, and signatures unless a selected mutation explicitly changes them.
- Editable and flattened saves are distinct. Flattening paints new supported annotations into `/Contents` and removes only those newly created `/Annots`; it is irreversible.
- Keep bitmaps bounded and release documents, descriptors, and temporary files. PDFBox uses a 50 MiB mixed-memory threshold.
- For ink annotations, use `PDAnnotationMarkup` with the `/Ink` subtype and its `setInkList`/`setConstantOpacity` APIs; do not reference the newer `PDAnnotationInk` class. Load byte arrays through the supported `InputStream` overload when applying `MemoryUsageSetting.setupMixed(50 MiB)`.
- Text highlighter selection must be based on cached character geometry, not word-box intersection. A drag selects a contiguous range on the first and last lines and all intervening lines, including reverse drags.
- Text-markup normal appearances must draw each selected quad with the annotation opacity. Never use the union rectangle as a filled appearance because it obscures the PDF text.
- Do not claim tile/viewport rendering or cross-viewer fidelity until code and CI/device validation prove it.

## Current feature status

- **Feature 01:** hybrid overlay/commit pipeline, shared coordinate mapper, mixed PDFBox memory, and mapper tests are implemented. Tile/viewport rendering, bitmap eviction, and PDFium text APIs are still pending.
- **Feature 02:** editable/flattened save modes, quad-based transparent highlight appearances, save/sync/reopen, and flattening of newly saved supported annotations are implemented. External-viewer validation remains pending.
- **Feature 03:** cached embedded highlights, normalized hit-testing, dashed selection bounds, anchored Delete action, and persistence of deletion are implemented. Color/comment actions and device validation remain pending.

## Validation and delivery

- Do **not** run Gradle locally. GitHub Actions is the build/test authority.
- Unit tests are under `app/src/test`; add/maintain mapper and hit-testing coverage when changing those contracts.
- Character-range selection coverage belongs in `TextHighlightSelectorTest`; keep wrapped-line and reverse-drag cases covered when changing highlighter interaction.
- Release generation runs on pushes to `main` and `feature` via `.github/workflows/gh-release.yml`.
- Preserve unrelated worktree changes. Stage only task files, inspect the staged diff, then commit and push when requested or when following the repository delivery workflow.
