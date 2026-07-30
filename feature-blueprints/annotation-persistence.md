# Annotation Persistence

## Outcome

Save supported annotations back to the selected PDF either as editable PDF
annotations or as irreversible flattened page content, without losing pending
work on write, provider-sync, or reopen failure.

## Current verified status

**Partial — last verified 2026-07-30.**

- `AnnotationSaveMode` exposes `Editable` and `Flattened`.
- PDFBox writes highlights, `/Ink` strokes, and `/Text` notes with normal
  appearances.
- Flattening paints newly created supported annotations into page content and
  removes only those new annotation entries.
- Save writes a temporary PDF, syncs through SAF, reopens the document, and
  clears optimistic state only after success.
- Broad Acrobat, PDFium, Drive, form, and signature fidelity is not verified by
  fixtures or a maintained cross-viewer matrix.

## Architecture dependencies

- [Commit pipeline](../.github/architecture.md#commit-pipeline)
- [Save modes](../.github/architecture.md#save-modes)
- [Failure handling and invalidation](../.github/architecture.md#failure-handling-and-invalidation)
- [Performance and lifecycle](../.github/architecture.md#performance-and-lifecycle)

## Feature-specific implications

- Snapshot pending annotations before starting background PDF mutation.
- Preserve unrelated existing annotations, forms, and signatures.
- Keep the selected save mode explicit; saving an annotation is not equivalent
  to flattening it.
- Sync to the original SAF URI before reopening and clearing optimistic state.
- Treat a read-only provider as a recoverable save failure even when opening
  succeeded.
- Use the shared mapper and each annotation feature's geometry; persistence must
  not reinterpret display coordinates independently.

## Related blueprints

- **Required:** the blueprint for every annotation type included in a save.
- **Impact checks:** all annotation blueprints whenever writer interfaces,
  appearance generation, flattening, deletion, or state-clearing behavior
  changes.

## Relevant implementation and tests

- `data/pdfbox/PdfAnnotationWriter.kt`
- `data/pdfbox/PdfAnnotationWriterImpl.kt`
- `domain/repository/PdfAnnotationSaver.kt`
- `presentation/mvi/PdfReaderViewModel.kt`
- `data/pdfbox/PdfCoordinateMapper.kt`

## Acceptance criteria

- Editable output reopens with supported annotations still selectable.
- Flattened output visibly contains selected marks in page content without the
  newly flattened annotation entries.
- Existing unrelated PDF objects remain intact.
- Failed write, sync, or reopen retains pending overlays and deletions for retry.
- Successful save refreshes rendered pages without duplicating annotations.
- PDF fixtures inspect `/Annots`, `/AP`, geometry arrays, `/Contents`, and
  reopenability; maintained external-viewer checks cover supported types.

## Remaining gaps

- Golden PDF-object fixtures for every supported annotation and save mode.
- Documented external-viewer validation matrix.
- Provider-failure, form-preservation, and signature-preservation coverage.
