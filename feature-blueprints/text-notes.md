# Text Notes

## Outcome

Place, edit, display, and persist page-scoped text notes without blocking reader
interaction or losing unsaved note contents during a failed save.

## Current verified status

**Status: Partial - implementation updated 2026-08-02; CI verification pending.**

- `TextAnnotation` stores page, normalized position, color, and text.
- Add Text places an editable note field and updates it through MVI intents.
- The reader exposes Add text as a semantic 48 dp toolbar action and keeps the
  editable field as an optimistic Compose overlay until Save succeeds.
- PDFBox writes `/Text` note annotations with contents, icon rectangle, color,
  closed state, and a normal appearance.
- Newly saved notes participate in editable and flattened save modes. Flattened
  notes render their complete encodable text into a visible note box; notes that
  cannot be represented without loss remain editable.
- Existing embedded note loading, selection, editing, and deletion are not
  implemented, and note-specific persistence fixtures are absent.

## Architecture dependencies

- [Coordinate system](../.github/architecture.md#coordinate-system)
- [Rendering and overlay model](../.github/architecture.md#rendering-and-overlay-model)
- [Commit pipeline](../.github/architecture.md#commit-pipeline)
- [Failure handling and invalidation](../.github/architecture.md#failure-handling-and-invalidation)

## Feature-specific implications

- Store the note anchor in normalized page space and derive popup placement from
  the shared page transform.
- Keep text editing in optimistic MVI state; do not write PDFBox objects from a
  Compose input callback.
- Preserve text exactly across state updates, save, provider sync, and reopen.
- Clamp the editor or popup to the visible window without changing the durable
  page anchor.
- A failed write, sync, or reopen must retain note text and placement for retry.

## Related blueprints

- **Required:** [`pdf-rendering.md`](pdf-rendering.md) for page alignment.
- **Impact checks:** [`annotation-persistence.md`](annotation-persistence.md)
  when `/Text`, appearance, flattening, or save behavior changes.

## Relevant implementation and tests

- `presentation/mvi/AnnotationModels.kt`
- `presentation/mvi/PdfReaderIntent.kt`
- `presentation/mvi/PdfReaderViewModel.kt`
- `presentation/ui/PdfReaderScreen.kt`
- `data/pdfbox/PdfAnnotationWriter.kt`
- `data/pdfbox/PdfAnnotationWriterTest.kt`

## Acceptance criteria

- A note remains anchored to the same page position through fit, rotation, and
  zoom transforms.
- Editing updates optimistic state without blocking pointer interaction.
- Editable save/reopen preserves note contents and a usable note icon.
- Flattened output visibly preserves the complete supported note content; an
  unencodable or oversized note remains editable instead of being discarded.
- Failed persistence retains the complete pending note.
- Empty, multiline, Unicode, long, and read-only-provider cases have explicit
  behavior and tests before the feature is marked Verified.

## Remaining gaps

- Embedded note discovery, selection, editing, and deletion.
- Defined empty-note and long-note UX.
- Editable-note, multiline, long-note, coordinate, and external-viewer tests.
