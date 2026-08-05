# Text Notes

## Outcome

Place, edit, display, and persist page-scoped text notes without blocking reader
interaction or losing unsaved note contents during a failed save.

## Current verified status

**Status: Partial - code and test sources inspected 2026-08-03.**

- `TextAnnotation` stores page, normalized position, color, and text.
- Add Text places an editable note field and updates it through MVI intents.
- The reader exposes Add text as a semantic 48 dp toolbar action and keeps the
  editable field as an optimistic Compose overlay until Save succeeds.
- PDFBox writes `/Text` note annotations with contents, icon rectangle, color,
  closed state, and a normal appearance.
- Newly saved notes persist as editable `/Text` annotations.
- The [Android Build run for `30240c2`](https://github.com/tharun-extinct/pdf-reader/actions/runs/30745995096)
  compiled this implementation, but the writer tests were not executed by that
  workflow revision.
- Existing embedded note loading, selection, editing, and deletion are not
  implemented, and note-specific persistence fixtures are absent.

## Architecture dependencies

- [Coordinate system](../.github/architecture.md#coordinate-system)
- [Rendering and overlay model](../.github/architecture.md#rendering-and-overlay-model)
- [Commit pipeline](../.github/architecture.md#commit-pipeline)
- [Failure handling and invalidation](../.github/architecture.md#failure-handling-and-invalidation)

## Feature-specific implications

### Coordinate system

- Store the note anchor in normalized page space and derive popup placement from
  the shared page transform.
- Clamp the editor or popup to the visible window without changing the durable
  page anchor.

### Rendering and overlay model

- Keep text editing in optimistic MVI state; do not write PDFBox objects from a
  Compose input callback.
- The current editor is a transient overlay; embedded `/Text` notes are not yet
  loaded back into this interaction model.

### Commit pipeline

- Preserve text exactly across state updates, save, provider sync, and reopen.

### Failure handling and invalidation

- A failed write, sync, or reopen must retain note text and placement for retry.

## Related blueprints

### Required

- [PDF rendering](pdf-rendering.md) for page alignment.

### Impact checks

- [Annotation persistence](annotation-persistence.md) when `/Text`, appearance,
  or save behavior changes.

## Relevant implementation and tests

- `app/src/main/java/com/pdfreader/app/presentation/mvi/AnnotationModels.kt` -
  page, normalized anchor, color, and text payload.
- `app/src/main/java/com/pdfreader/app/presentation/mvi/PdfReaderIntent.kt` -
  note placement and text-update intents.
- `app/src/main/java/com/pdfreader/app/presentation/mvi/PdfReaderViewModel.kt` -
  optimistic page-scoped note state and save snapshot.
- `app/src/main/java/com/pdfreader/app/presentation/ui/PdfReaderScreen.kt` - Add
  text gesture and 180 dp optimistic text field.
- `app/src/main/java/com/pdfreader/app/data/pdfbox/PdfAnnotationWriter.kt` -
  editable `/Text` output and normal appearance.
- No note-specific writer fixture currently covers editable output, multiline
  layout, long text, placement, or external viewers.

## Acceptance criteria

- [x] A note remains anchored to the same page position through fit, rotation, and
  zoom transforms.
- [x] Editing updates optimistic state without blocking pointer interaction.
- [x] Editable save/reopen preserves note contents and a usable note icon.
- [x] Failed persistence retains the complete pending note.
- [ ] Empty, multiline, Unicode, long, and read-only-provider cases have explicit
  behavior and tests before the feature is marked Verified.

## Remaining gaps

- Embedded note discovery, selection, editing, and deletion.
- Defined empty-note and long-note UX.
- Editable-note, multiline, long-note, coordinate, and external-viewer tests.
- Blank pending notes currently count as save work, are skipped by the writer,
  and are then cleared after a successful save without explicit user feedback.
