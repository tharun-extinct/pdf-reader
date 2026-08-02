# Freehand Annotation

## Outcome

Create responsive pen and freehand-highlight strokes, erase eligible pending
strokes, and persist them as standards-compatible vector ink or flattened page
content.

## Current verified status

**Status: Partial - code and test sources inspected 2026-08-03.**

- `FreehandStroke` stores page, tool, color, width, and normalized points.
- Compose renders the in-progress stroke and committed session overlays.
- Pen and highlighter palettes are configurable in reader state.
- The floating toolbar exposes Pen, Highlighter, and Eraser with 48 dp semantic
  controls; Pen and Highlighter show the active contextual palette.
- The eraser removes matching unsaved strokes along a drag.
- PDFBox writes vector `/Ink` annotations with normal appearances; newly saved
  strokes can be flattened with their configured `/BS /W` width.
- The [Android Build run for `30240c2`](https://github.com/tharun-extinct/pdf-reader/actions/runs/30745995096)
  compiled this implementation, but the writer and mapper test sources were not
  executed by that workflow revision.
- Pressure sensitivity, persisted-stroke reselection, and dedicated gesture or
  PDF-fixture coverage are not implemented.

## Architecture dependencies

- [Coordinate system](../.github/architecture.md#coordinate-system)
- [Rendering and overlay model](../.github/architecture.md#rendering-and-overlay-model)
- [Commit pipeline](../.github/architecture.md#commit-pipeline)
- [Performance and lifecycle](../.github/architecture.md#performance-and-lifecycle)

## Feature-specific implications

### Coordinate system

- Capture and render points in the same normalized page space used by the page
  overlay; never store raw screen pixels as durable geometry.
- Map stroke width consistently between display space, PDF points, `/Ink`
  metadata, normal appearance, and flattened output.

### Rendering and overlay model

- Keep pointer previews entirely in Compose and defer PDFBox work until Save.
- Erasing pending strokes updates immutable MVI state first.

### Commit pipeline

- A save snapshots page-scoped strokes and retains them if write, sync, or
  reopen fails.
- Editable and flattened output use the same durable path and width data.

### Performance and lifecycle

- Drag sampling and preview drawing must not trigger PDF parsing or base-page
  rendering.
- Do not claim pressure-sensitive input unless the model, rendering, writer, and
  tests preserve pressure data end to end.

## Related blueprints

### Required

- [PDF rendering](pdf-rendering.md) for page alignment.

### Impact checks

- [Annotation persistence](annotation-persistence.md) when `/Ink`, appearance,
  flattening, or save behavior changes.
- [Text highlighting](text-highlighting.md) when the shared Highlighter tool's
  text-selection versus freehand fallback behavior changes.

## Relevant implementation and tests

- `app/src/main/java/com/pdfreader/app/presentation/mvi/AnnotationModels.kt` -
  stroke tool, color, width, and normalized points.
- `app/src/main/java/com/pdfreader/app/presentation/mvi/PdfReaderIntent.kt` -
  palette, stroke, and erase intents.
- `app/src/main/java/com/pdfreader/app/presentation/mvi/PdfReaderViewModel.kt` -
  immutable pending-stroke state and page-scoped erase policy.
- `app/src/main/java/com/pdfreader/app/presentation/ui/PdfReaderScreen.kt` -
  drag capture, preview, palettes, tool semantics, and fixed pen/highlighter
  widths.
- `app/src/main/java/com/pdfreader/app/data/pdfbox/PdfAnnotationWriter.kt` -
  `/Ink`, `/BS /W`, normal appearance, and flattened paths.
- `app/src/test/java/com/pdfreader/app/data/pdfbox/PdfCoordinateMapperTest.kt` -
  partial coordinate evidence shared with annotation persistence.
- `app/src/test/java/com/pdfreader/app/data/pdfbox/PdfAnnotationWriterTest.kt` -
  configured flattened ink width. No gesture, editable `/Ink`, opacity, or
  external-viewer test currently exists.

## Acceptance criteria

- [ ] Drawing remains smooth without rerendering or mutating the base PDF.
- [ ] Preview and saved stroke positions, color, opacity, and width agree.
- [ ] Erasing eligible pending strokes is immediate and page-scoped.
- [ ] Editable output contains standards-compatible vector ink.
- [ ] Flattened output contains the stroke in page content without its newly created
  annotation entry.
- [ ] Failed persistence retains the pending stroke for retry.

## Remaining gaps

- Pressure-sensitive geometry and end-to-end persistence.
- Selection or editing of existing embedded ink.
- Gesture, broader PDF-object, and external-viewer tests.
- Palette edits are session-only and reset when the ViewModel is recreated.
