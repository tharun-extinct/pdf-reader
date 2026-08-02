# Annotation Persistence

## Outcome

Save supported annotations back to the selected PDF either as editable PDF
annotations or as irreversible flattened page content, without losing pending
work on write, provider-sync, or reopen failure.

## Current verified status

**Status: Partial - implementation updated 2026-08-03; CI verification pending.**

- `AnnotationSaveMode` exposes `Editable` and `Flattened`.
- PDFBox writes highlights, `/Ink` strokes, and `/Text` notes with normal
  appearances.
- Flattening is implemented to paint newly created supported annotations into
  page content and remove only those new annotation entries. The JVM regression
  tests await CI verification after fixing PDFBox wrapper-identity removal.
- Flattened ink reads the stroke width stored in `/BS /W` instead of substituting
  a fixed width.
- Flattened text notes render their complete encodable contents in a visible
  note box. Notes that cannot fit or cannot be encoded remain editable rather
  than losing their payload.
- Save writes a temporary PDF, syncs through SAF, reopens the document, and
  clears optimistic state only after success.
- Reader controls keep editable versus flattened mode explicit, disable Save
  when all pending annotation and deletion collections are empty, and expose
  save progress.
- The [Android Build run for `30240c2`](https://github.com/tharun-extinct/pdf-reader/actions/runs/30745995096)
  compiled the annotation implementation on 2026-08-02, but that workflow
  revision did not execute the JVM writer tests.
- Broad Acrobat, PDFium, Drive, form, and signature fidelity is not verified by
  fixtures or a maintained cross-viewer matrix.

## Architecture dependencies

- [Commit pipeline](../.github/architecture.md#commit-pipeline)
- [Save modes](../.github/architecture.md#save-modes)
- [Failure handling and invalidation](../.github/architecture.md#failure-handling-and-invalidation)
- [Performance and lifecycle](../.github/architecture.md#performance-and-lifecycle)

## Feature-specific implications

### Commit pipeline

- Snapshot pending annotations before starting background PDF mutation.
- Sync to the original SAF URI before reopening and clearing optimistic state.
- Use the shared mapper and each annotation feature's geometry; persistence must
  not reinterpret display coordinates independently.

### Save modes

- Preserve unrelated existing annotations, forms, and signatures.
- Keep the selected save mode explicit; saving an annotation is not equivalent
  to flattening it.
- Remove a newly flattened annotation only when its complete supported payload
  was represented in page content.

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
  generation, flattening, deletion, or state-clearing behavior changes.
- Inspect [Document library](document-library.md) when URI permission or close
  behavior changes save-back availability.

## Relevant implementation and tests

- `app/src/main/java/com/pdfreader/app/data/pdfbox/PdfAnnotationWriter.kt` - PDF
  objects, appearances, deletion, and loss-aware flattening.
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
  flattened note retention and configured ink-width behavior. The source exists,
  but no recorded CI run has executed this test revision.

## Acceptance criteria

- [ ] Editable output reopens with supported annotations still selectable.
- [ ] Flattened output visibly contains selected marks in page content without the
  newly flattened annotation entries.
- [ ] Flattening never removes an annotation whose supported payload was not fully
  represented in page content.
- [ ] Existing unrelated PDF objects remain intact.
- [ ] Failed write, sync, or reopen retains pending overlays and deletions for retry.
- [ ] Successful save refreshes rendered pages without duplicating annotations.
- [ ] PDF fixtures inspect `/Annots`, `/AP`, geometry arrays, `/Contents`, and
  reopenability; maintained external-viewer checks cover supported types.

## Remaining gaps

- Golden PDF-object fixtures beyond flattened note text and ink width.
- Documented external-viewer validation matrix.
- Provider-failure, form-preservation, and signature-preservation coverage.
- `SafPdfSyncManager.syncBackToSource` currently returns success when the
  provider returns a null output stream; this can allow the save pipeline to
  proceed without proving that bytes were written.
