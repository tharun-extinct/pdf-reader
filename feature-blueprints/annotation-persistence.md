# Annotation Persistence

## Outcome

Save supported annotations back to the selected PDF as editable standard PDF
annotations without losing pending work on write, provider-sync, or reopen failure.

## Current verified status

**Status: Partial - implementation updated 2026-08-05; CI verification pending.**

- Editable output is the only save behavior; the reader has no mode selector.
- PDFBox writes highlights, `/Ink` strokes, and `/Text` notes with normal
  appearances.
- Highlight appearance streams initialize their own PDF resource dictionaries,
  so opacity state can be written even when the source page has no `/Resources`.
- Ink width is converted from normalized displayed-page width to PDF points, so
  save and reopen preserve the preview thickness across page sizes and rotation.
- Editable `/Ink` gets an explicit rounded, width-exact normal appearance.
- Embedded `/Ink` geometry, width, and color are loaded for in-reader reselection;
  selected embedded ink can be queued for deletion on the next save.
- Embedded `/Text` notes are loaded for reselection; editing creates a pending
  replacement, queues the original for deletion, and restores selection after
  the successful reopen without duplicating the note.
- Save writes a temporary PDF, syncs through SAF, reopens the document, and
  clears optimistic state only after success.
- A provider that returns no output stream is treated as a failed sync, so
  optimistic annotations remain available for retry.
- Reader controls omit save-mode UI, disable Save when all pending annotation
  and deletion collections are empty, and expose save progress.
- The [Android Build run for `30240c2`](https://github.com/tharun-extinct/pdf-reader/actions/runs/30745995096)
  compiled the annotation implementation on 2026-08-02, but that workflow
  revision did not execute the JVM writer tests.
- Broad Acrobat, PDFium, Drive, form, and signature fidelity is not verified by
  fixtures or a maintained cross-viewer matrix.

## Architecture dependencies

- [Commit pipeline](../.github/architecture.md#commit-pipeline)
- [Editable annotation output](../.github/architecture.md#editable-annotation-output)
- [Failure handling and invalidation](../.github/architecture.md#failure-handling-and-invalidation)
- [Performance and lifecycle](../.github/architecture.md#performance-and-lifecycle)

## Feature-specific implications

### Commit pipeline

- Snapshot pending annotations before starting background PDF mutation.
- Sync to the original SAF URI before reopening and clearing optimistic state.
- Use the shared mapper and each annotation feature's geometry; persistence must
  not reinterpret display coordinates independently.

### Editable annotation output

- Preserve unrelated existing annotations, forms, and signatures.
- Preserve highlights, ink, and notes as editable PDF annotation objects.
- Do not expose or retain an irreversible page-content flattening path.

### Failure handling and invalidation

- Treat a read-only provider as a recoverable save failure even when opening
  succeeded.
- A save failure must retain every pending overlay and deletion and expose a
  retryable error; only a successful reopen may clear them.
- A missing provider output stream is a failure, not a successful no-op.

### Performance and lifecycle

- PDFBox mutation, SAF writes, and reopen work stay off the main thread.
- Temporary output and opened PDF resources must be released on success and
  failure without clearing retryable state.

## Related blueprints

### Required

- Load each annotation-type blueprint included in the requested save change:
  [Text highlighting](text-highlighting.md),
  [Freehand annotation](freehand-annotation.md), or
  [Text notes](text-notes.md).

### Impact checks

- Inspect every annotation blueprint whenever writer interfaces, appearance
  generation, deletion, or state-clearing behavior changes.
- Inspect [Document library](document-library.md) when URI permission or close
  behavior changes save-back availability.

## Relevant implementation and tests

- `app/src/main/java/com/pdfreader/app/data/pdfbox/PdfAnnotationWriter.kt` - PDF
  objects, appearances, and embedded highlight/ink deletion.
- `app/src/main/java/com/pdfreader/app/data/pdfbox/PdfAnnotationWriterImpl.kt` -
  background PDFBox load/write boundary and mixed-memory limit.
- `app/src/main/java/com/pdfreader/app/data/sync/SafPdfSyncManager.kt` - original
  SAF URI write-back.
- `app/src/main/java/com/pdfreader/app/domain/repository/PdfAnnotationSaver.kt` -
  save contract shared with the ViewModel.
- `app/src/main/java/com/pdfreader/app/presentation/mvi/PdfReaderViewModel.kt` -
  snapshot, temporary output, sync, reopen, invalidation, and retry state.
- `app/src/main/java/com/pdfreader/app/data/pdfbox/PdfCoordinateMapper.kt` -
  normalized-display to PDF-space conversion boundary.
- `app/src/test/java/com/pdfreader/app/data/pdfbox/PdfAnnotationWriterTest.kt` -
  resource-less-page highlight saving, editable ink appearance width/cap/join,
  embedded-ink deletion, and embedded-note deletion. CI has not executed this
  revision.
- `app/src/test/java/com/pdfreader/app/data/pdfium/PdfEmbeddedTextAnnotationReaderTest.kt`
  - `/Text` content, color, stable source identity, and CropBox/right-angle
  anchor mapping after reopen.

## Acceptance criteria

- [x] Editable output reopens with supported annotations still selectable in
  the implemented reader interaction model; external-viewer validation remains
  pending.
- [x] The reader exposes no flattened-output mode or mode selector.
- [ ] Existing unrelated PDF objects remain intact.
- [ ] Failed write, sync, or reopen retains pending overlays and deletions for retry.
- [x] Successful save refreshes rendered pages without duplicating annotations.
- [ ] PDF fixtures inspect `/Annots`, `/AP`, geometry arrays, `/Contents`, and
  reopenability; maintained external-viewer checks cover supported types.

## Remaining gaps

- Golden PDF-object fixtures for editable notes, highlights, and ink.
- Documented external-viewer validation matrix.
- Provider-failure, form-preservation, and signature-preservation coverage.
- Provider-null-output and broader write/reopen failure paths still need
  automated retry-state coverage.
