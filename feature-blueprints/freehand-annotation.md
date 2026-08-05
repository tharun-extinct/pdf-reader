# Freehand Annotation

## Outcome

Create responsive pen strokes, erase eligible pending strokes, persist them as
standards-compatible vector ink, and reselect pending or embedded ink.

## Current verified status

**Status: Partial - implementation updated 2026-08-04; CI verification pending.**

- `FreehandStroke` stores page, tool, color, normalized width, and normalized
  points.
- Compose invalidates a stable snapshot point list throughout a pen drag, so ink
  appears under the pointer before touch-up.
- In-progress and committed overlays use the same midpoint spline with round
  caps and joins; editable PDF appearances mirror that curve.
- Pen and highlighter palettes are persisted preferences edited from the
  scrollable Settings screen; the reader keeps compact palette selection chips.
- The floating toolbar exposes recognizable pen, text-highlighter, and eraser
  symbols in 48 dp semantic controls.
- Palette taps restart gesture capture with the selected color, preventing a
  prior pointer-input closure from writing the previous color.
- The eraser removes matching unsaved strokes along a drag.
- PDFBox converts normalized display width to PDF points and supplies a
  width-exact rounded `/Ink` appearance instead of delegating appearance shape
  and thickness to viewer defaults.
- The [Android Build run for `30240c2`](https://github.com/tharun-extinct/pdf-reader/actions/runs/30745995096)
  compiled this implementation, but the writer and mapper test sources were not
  executed by that workflow revision.
- Pending and embedded `/Ink` strokes are hit-tested against their vector paths;
  selection shows a boundary and Delete action, and embedded deletion saves.
- Pressure sensitivity and device-level gesture coverage are not implemented.

## Architecture dependencies

- [Coordinate system](../.github/architecture.md#coordinate-system)
- [Rendering and overlay model](../.github/architecture.md#rendering-and-overlay-model)
- [Commit pipeline](../.github/architecture.md#commit-pipeline)
- [Performance and lifecycle](../.github/architecture.md#performance-and-lifecycle)

## Feature-specific implications

### Coordinate system

- Capture and render points in the same normalized page space used by the page
  overlay; never store raw screen pixels as durable geometry.
- Store width relative to displayed page width and map it consistently between
  Compose, PDF points, `/Ink` metadata, normal appearance, and loaded ink.

### Rendering and overlay model

- Keep pointer previews entirely in Compose and defer PDFBox work until Save.
- Erasing pending strokes updates immutable MVI state first.

### Commit pipeline

- A save snapshots page-scoped strokes and retains them if write, sync, or
  reopen fails.
- Editable output and reloaded ink use the same durable path and width data.

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
  deletion, or save behavior changes.
- [Reader preferences](reader-preferences.md) when palette defaults, storage, or
  Settings ownership changes.
- [Text highlighting](text-highlighting.md) when Highlighter versus Pen gesture
  ownership changes.

## Relevant implementation and tests

- `app/src/main/java/com/pdfreader/app/presentation/mvi/AnnotationModels.kt` -
  stroke tool, color, width, and normalized points.
- `app/src/main/java/com/pdfreader/app/presentation/mvi/PdfReaderIntent.kt` -
  palette, stroke, and erase intents.
- `app/src/main/java/com/pdfreader/app/presentation/mvi/PdfReaderViewModel.kt` -
  pending/embedded ink selection, deletion state, and page-scoped erase policy.
- `app/src/main/java/com/pdfreader/app/presentation/ui/PdfReaderScreen.kt` -
  pen drag capture, normalized-width preview, ink selection overlay, and tool semantics.
- `app/src/main/java/com/pdfreader/app/presentation/ui/SettingsScreen.kt` -
  scrollable pen and highlighter palette editor.
- `app/src/main/java/com/pdfreader/app/data/pdfbox/PdfAnnotationWriter.kt` -
  `/Ink`, `/BS /W`, normal appearance, and embedded deletion.
- `app/src/main/java/com/pdfreader/app/presentation/mvi/InkHitTester.kt` -
  width-aware nearest-path reselection policy.
- `app/src/test/java/com/pdfreader/app/data/pdfbox/PdfCoordinateMapperTest.kt` -
  partial coordinate evidence shared with annotation persistence.
- `app/src/test/java/com/pdfreader/app/data/pdfbox/PdfAnnotationWriterTest.kt` -
  editable `/Ink` width, rounded normal appearance operators, and deletion.
- `app/src/test/java/com/pdfreader/app/presentation/mvi/InkHitTesterTest.kt` -
  segment proximity and miss behavior.

## Acceptance criteria

- [x] Drawing remains smooth without rerendering or mutating the base PDF.
- [x] Preview and saved stroke positions, color, opacity, and width agree.
- [x] Erasing eligible pending strokes is immediate and page-scoped.
- [x] Editable output contains standards-compatible vector ink.
- [x] Pending and embedded ink can be reselected and deleted without flattening.
- [x] Failed persistence retains the pending stroke for retry.

## Remaining gaps

- Pressure-sensitive geometry and end-to-end persistence.
- Editing embedded ink color, width, or geometry after reselection.
- Gesture, broader PDF-object, and external-viewer tests.
- CI execution is pending for normalized-width and palette-persistence tests.
