# PDF Rendering

## Outcome

Render readable PDF pages responsively while keeping annotation and selection
overlays aligned through page fit, rotation, and zoom. Large or obsolete render
requests must not cause unbounded memory use.

## Current verified status

**Status: Partial - code and test sources inspected 2026-08-03.**

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

### Rendering and overlay model

- PDFium owns the static page layer; interaction previews stay in Compose.
- Rendering and every overlay must use the same fitted page `contentBounds`.

### Coordinate system

- Page size, CropBox, and right-angle rotation data must feed the shared
  coordinate mapper rather than local DPI or Y-flip calculations.

### Performance and lifecycle

- Obsolete work must be cancellable before viewport/tile rendering is described
  as implemented.
- Bitmap ownership and eviction must be explicit before large or zoomed pages
  are considered memory-bounded.

### Failure handling and invalidation

- Opening and rendering expose progress; a load error keeps a Choose another PDF
  recovery action available.
- Any future cache key must include document version, page, viewport, zoom, and
  tile identity and must be invalidated after a successful reopen.

## Related blueprints

### Required

- None.

### Impact checks

- Inspect [Text highlighting](text-highlighting.md),
  [Freehand annotation](freehand-annotation.md), and
  [Text notes](text-notes.md) when `contentBounds`, overlay transforms,
  rotation, or invalidation changes.
- Inspect [Read aloud](read-aloud.md) when transient text-overlay alignment or
  page lifecycle changes.

## Relevant implementation and tests

- `app/src/main/java/com/pdfreader/app/data/pdfium/PdfiumEngine.kt` - PDFium
  document lifetime, page size, full-page bitmap rendering, text extraction,
  and embedded-highlight caches.
- `app/src/main/java/com/pdfreader/app/presentation/ui/PdfReaderScreen.kt` -
  horizontal pager, bitmap callback ownership, fitted bounds, overlays, and
  `1x..5x` scale gesture.
- `app/src/main/java/com/pdfreader/app/presentation/mvi/PdfReaderViewModel.kt` -
  background render requests and post-save render revision.
- `app/src/main/java/com/pdfreader/app/data/pdfbox/PdfCoordinateMapper.kt` -
  CropBox and right-angle rotation conversion used by annotation consumers.
- `app/src/test/java/com/pdfreader/app/data/pdfbox/PdfCoordinateMapperTest.kt` -
  unrotated CropBox origin and one 90-degree round trip; it does not cover the
  full rendering lifecycle or every rotation.

## Acceptance criteria

- [x] Pointer interaction does not rerender the base PDF bitmap.
- [ ] Page and annotation geometry stay aligned for portrait, landscape, cropped,
  and right-angle rotated pages.
- [ ] Superseded rendering work does not overwrite newer page state.
- [ ] Large and zoomed pages remain within a documented bitmap-memory budget.
- [ ] PDFium documents, descriptors, and discarded bitmaps are released on success,
  replacement, close, and failure.

## Remaining gaps

- Viewport/tile rendering and stable tile cache keys.
- Bounded bitmap eviction and obsolete-request cancellation.
- Complete pan, reset, and double-tap zoom behavior.
- Page thumbnails or a scrubber for long-document navigation.
- Device validation for rotation, rapid paging, and large documents.
