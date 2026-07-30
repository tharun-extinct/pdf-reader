# Text Highlighting

## Outcome

Create continuous line-aware text highlights, select session or embedded
highlights, and delete supported highlights while keeping preview, hit-testing,
and persisted geometry aligned.

## Current verified status

**Partial — last verified 2026-07-30.**

- PDFBox extraction supplies word and per-character bounds.
- `TextHighlightSelector` creates continuous wrapped-line ranges and normalizes
  reverse drags.
- Session and embedded highlights use page-scoped normalized rectangles.
- `HighlightHitTester` selects the smallest overlapping candidate.
- A selected highlight shows a translucent overlay, dashed union boundary, and
  anchored Delete action.
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

- Store every selected line segment as normalized top-left display rectangles.
- Use identical geometry for preview, hit-testing, `/Rect`, `/QuadPoints`, and
  appearance generation through the shared mapper.
- Preserve separate line segments; the union rectangle is selection metadata
  and must never be painted as one solid highlight.
- Load and cache embedded highlight metadata off the main thread before pointer
  hit-testing.
- Selection alone never mutates the PDF. Outside taps clear selection without
  creating pending persistence work.
- Merge session and embedded highlights without duplication after save/reopen.

## Related blueprints

- **Required:** [`pdf-rendering.md`](pdf-rendering.md) for the page transform.
- **Impact checks:** [`annotation-persistence.md`](annotation-persistence.md)
  when deletion, `/QuadPoints`, appearances, or save/reopen behavior changes.

## Relevant implementation and tests

- `presentation/mvi/TextHighlightSelector.kt`
- `presentation/mvi/HighlightHitTester.kt`
- `presentation/mvi/AnnotationModels.kt`
- `presentation/ui/PdfReaderScreen.kt`
- `data/pdfium/PdfiumEngine.kt`
- `TextHighlightSelectorTest.kt`
- `HighlightHitTesterTest.kt`

## Acceptance criteria

- Forward and reverse drags select the same continuous reading-order range.
- Wrapped highlights contain separate line rectangles without filling
  intervening whitespace.
- Overlapping highlights select the smallest matching candidate.
- Selection bounds and menu remain aligned and clamped on the page.
- Outside taps dismiss selection without changing the PDF.
- Delete updates UI immediately and persists without duplication after save.
- Rotation, cropped pages, multiline geometry, and overlap are covered by tests.

## Remaining gaps

- Change-color and defined comment actions.
- Tile-aware transforms and complete zoom/pan validation.
- Device and external-viewer verification for selection and persisted geometry.
