# PDF Rendering

## Outcome

Render readable PDF pages responsively while keeping annotation and selection
overlays aligned through page fit, rotation, and zoom. Large or obsolete render
requests must not cause unbounded memory use.

## Current verified status

**Status: Partial - last inspected 2026-08-02.**

- `PdfiumEngine` renders PDFium pages into ARGB bitmaps off the main thread.
- Rendered bitmaps are returned through callbacks rather than stored in
  `PdfReaderState`.
- Compose draws pending annotations and interaction feedback above the bitmap.
- The reader presents pages in a horizontal pager, fades in completed renders,
  reports `Page N of M`, and clamps pinch zoom to `1x..5x`.
- Saving reopens the document and increments `renderRevision`.
- Rendering still creates one full-page bitmap. Viewport tiles, bounded bitmap
  eviction, render cancellation, and complete zoom panning are not implemented.

## Architecture dependencies

- [Rendering and overlay model](../.github/architecture.md#rendering-and-overlay-model)
- [Coordinate system](../.github/architecture.md#coordinate-system)
- [Performance and lifecycle](../.github/architecture.md#performance-and-lifecycle)
- [Failure handling and invalidation](../.github/architecture.md#failure-handling-and-invalidation)

## Feature-specific implications

- PDFium owns the static page layer; interaction previews stay in Compose.
- Opening and rendering expose progress; a load error keeps a Choose another PDF
  recovery action available.
- Rendering and every overlay must use the same fitted page `contentBounds`.
- Page size, CropBox, and right-angle rotation data must feed the shared
  coordinate mapper rather than local DPI or Y-flip calculations.
- Obsolete work must be cancellable before viewport/tile rendering is described
  as implemented.
- Any future cache key must include document version, page, viewport, zoom, and
  tile identity and must be invalidated after a successful reopen.

## Related blueprints

- **Required:** none.
- **Impact checks:** inspect all annotation blueprints when a rendering change
  alters `contentBounds`, overlay transforms, rotation, or invalidation.

## Relevant implementation and tests

- `data/pdfium/PdfiumEngine.kt`
- `presentation/ui/PdfReaderScreen.kt`
- `presentation/mvi/PdfReaderViewModel.kt`
- `data/pdfbox/PdfCoordinateMapper.kt`
- `PdfCoordinateMapperTest.kt`

## Acceptance criteria

- Pointer interaction does not rerender the base PDF bitmap.
- Page and annotation geometry stay aligned for portrait, landscape, cropped,
  and right-angle rotated pages.
- Superseded rendering work does not overwrite newer page state.
- Large and zoomed pages remain within a documented bitmap-memory budget.
- PDFium documents, descriptors, and discarded bitmaps are released on success,
  replacement, close, and failure.

## Remaining gaps

- Viewport/tile rendering and stable tile cache keys.
- Bounded bitmap eviction and obsolete-request cancellation.
- Complete pan, reset, and double-tap zoom behavior.
- Page thumbnails or a scrubber for long-document navigation.
- Device validation for rotation, rapid paging, and large documents.
