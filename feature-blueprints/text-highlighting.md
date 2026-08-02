# Text Highlighting

## Outcome

Create continuous line-aware text highlights, select session or embedded
highlights, and delete supported highlights while keeping preview, hit-testing,
and persisted geometry aligned.

## Current verified status

**Status: Partial - code and test sources inspected 2026-08-03.**

- PDFBox extraction supplies word and per-character bounds.
- `TextHighlightSelector` creates continuous wrapped-line ranges and normalizes
  reverse drags.
- Session and embedded highlights use page-scoped normalized rectangles.
- `HighlightHitTester` selects the smallest overlapping candidate.
- A selected highlight shows a translucent overlay, dashed union boundary, and
  anchored Delete action.
- With no annotation tool active, a tap selects the smallest matching session or
  embedded highlight; an outside tap clears the selection.
- Deleting a session highlight updates state immediately; embedded deletion is
  recorded for the next save.
- Color-change and comment actions, tile-aware transforms, and broad device or
  external-viewer validation remain incomplete.

## Architecture dependencies

- [Coordinate system](../.github/architecture.md#coordinate-system)
- [Text geometry and highlighting](../.github/architecture.md#text-geometry-and-highlighting)
- [Rendering and overlay model](../.github/architecture.md#rendering-and-overlay-model)
- [Commit pipeline](../.github/architecture.md#commit-pipeline)

## Feature-specific implications

### Coordinate system

- Store every selected line segment as normalized top-left display rectangles.
- Use identical geometry for preview, hit-testing, `/Rect`, `/QuadPoints`, and
  appearance generation through the shared mapper.

### Text geometry and highlighting

- Preserve separate line segments; the union rectangle is selection metadata
  and must never be painted as one solid highlight.
- Forward and reverse drags resolve through the same reading-order cursors.

### Rendering and overlay model

- Load and cache embedded highlight metadata off the main thread before pointer
  hit-testing.
- Selection alone never mutates the PDF. Outside taps clear selection without
  creating pending persistence work.

### Commit pipeline

- Merge session and embedded highlights without duplication after save/reopen.
- Record embedded deletion as pending state and remove it only through a
  successful save pipeline.

## Related blueprints

### Required

- [PDF rendering](pdf-rendering.md) for the page transform.

### Impact checks

- [Annotation persistence](annotation-persistence.md) when deletion,
  `/QuadPoints`, appearances, or save/reopen behavior changes.
- [Read aloud](read-aloud.md) when extracted text geometry or transient overlay
  ordering changes.
- [Freehand annotation](freehand-annotation.md) when the Highlighter tool's
  freehand fallback changes.

## Relevant implementation and tests

- `app/src/main/java/com/pdfreader/app/presentation/mvi/TextHighlightSelector.kt` -
  reading-order range construction from per-character boxes.
- `app/src/main/java/com/pdfreader/app/presentation/mvi/HighlightHitTester.kt` -
  inflated hit targets and smallest-overlap selection policy.
- `app/src/main/java/com/pdfreader/app/presentation/mvi/AnnotationModels.kt` -
  session, embedded, and selected highlight representations.
- `app/src/main/java/com/pdfreader/app/presentation/mvi/PdfReaderViewModel.kt` -
  merged selection candidates and pending session/embedded deletion state.
- `app/src/main/java/com/pdfreader/app/presentation/ui/PdfReaderScreen.kt` -
  drag preview, fitted overlay, selection boundary, and Delete action.
- `app/src/main/java/com/pdfreader/app/data/pdfium/PdfiumEngine.kt` - text and
  embedded `/Highlight` extraction caches.
- `app/src/test/java/com/pdfreader/app/presentation/mvi/TextHighlightSelectorTest.kt` -
  wrapped and reverse selection.
- `app/src/test/java/com/pdfreader/app/presentation/mvi/HighlightHitTesterTest.kt` -
  smallest overlapping candidate. No ViewModel deletion, embedded PDF fixture,
  or device-level overlay test currently exists.

## Acceptance criteria

- [ ] Forward and reverse drags select the same continuous reading-order range.
- [ ] Wrapped highlights contain separate line rectangles without filling
  intervening whitespace.
- [ ] Overlapping highlights select the smallest matching candidate.
- [ ] Selection bounds and menu remain aligned and clamped on the page.
- [ ] Outside taps dismiss selection without changing the PDF.
- [ ] Delete updates UI immediately and persists without duplication after save.
- [ ] Rotation, cropped pages, multiline geometry, and overlap are covered by tests.

## Remaining gaps

- Change-color and defined comment actions.
- Tile-aware transforms and complete zoom/pan validation.
- Device and external-viewer verification for selection and persisted geometry.
- `PdfiumEngine` currently normalizes extracted text with MediaBox dimensions
  instead of routing text geometry through the CropBox-aware shared mapper;
  cropped and rotated text fixtures are absent.
- Embedded highlight deletion relies on page annotation-list indices as IDs;
  fixture coverage does not yet prove stability across all save/reopen cases.
