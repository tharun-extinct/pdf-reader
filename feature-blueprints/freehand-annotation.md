# Freehand Annotation

## Outcome

Create responsive pen and freehand-highlight strokes, erase eligible pending
strokes, and persist them as standards-compatible vector ink or flattened page
content.

## Current verified status

**Partial — implementation updated 2026-08-02; CI verification pending.**

- `FreehandStroke` stores page, tool, color, width, and normalized points.
- Compose renders the in-progress stroke and committed session overlays.
- Pen and highlighter palettes are configurable in reader state.
- The eraser removes matching unsaved strokes along a drag.
- PDFBox writes vector `/Ink` annotations with normal appearances; newly saved
  strokes can be flattened with their configured `/BS /W` width.
- Pressure sensitivity, persisted-stroke reselection, and dedicated gesture or
  PDF-fixture coverage are not implemented.

## Architecture dependencies

- [Coordinate system](../.github/architecture.md#coordinate-system)
- [Rendering and overlay model](../.github/architecture.md#rendering-and-overlay-model)
- [Commit pipeline](../.github/architecture.md#commit-pipeline)
- [Performance and lifecycle](../.github/architecture.md#performance-and-lifecycle)

## Feature-specific implications

- Capture and render points in the same normalized page space used by the page
  overlay; never store raw screen pixels as durable geometry.
- Keep pointer previews entirely in Compose and defer PDFBox work until Save.
- Map stroke width consistently between display space, PDF points, `/Ink`
  metadata, normal appearance, and flattened output.
- Erasing pending strokes updates immutable MVI state first.
- Do not claim pressure-sensitive input unless the model, rendering, writer, and
  tests preserve pressure data end to end.

## Related blueprints

- **Required:** [`pdf-rendering.md`](pdf-rendering.md) for page alignment.
- **Impact checks:** [`annotation-persistence.md`](annotation-persistence.md)
  when `/Ink`, appearance, flattening, or save behavior changes.

## Relevant implementation and tests

- `presentation/mvi/AnnotationModels.kt`
- `presentation/mvi/PdfReaderIntent.kt`
- `presentation/mvi/PdfReaderViewModel.kt`
- `presentation/ui/PdfReaderScreen.kt`
- `data/pdfbox/PdfAnnotationWriter.kt`
- `data/pdfbox/PdfCoordinateMapperTest.kt`
- `data/pdfbox/PdfAnnotationWriterTest.kt`

## Acceptance criteria

- Drawing remains smooth without rerendering or mutating the base PDF.
- Preview and saved stroke positions, color, opacity, and width agree.
- Erasing eligible pending strokes is immediate and page-scoped.
- Editable output contains standards-compatible vector ink.
- Flattened output contains the stroke in page content without its newly created
  annotation entry.
- Failed persistence retains the pending stroke for retry.

## Remaining gaps

- Pressure-sensitive geometry and end-to-end persistence.
- Selection or editing of existing embedded ink.
- Gesture, broader PDF-object, and external-viewer tests.
