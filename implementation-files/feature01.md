# Feature 01 — Hybrid PDF rendering and annotation contract

## Goal

Provide low-latency PDF reading and annotation with PDFium for raster rendering and PDFBox for durable PDF annotations. Keep the UI responsive, avoid bitmap/document leaks, and preserve coordinate accuracy.

## Required architecture

1. **Base layer:** PDFium renders the static page. Use viewport/tile rendering for large pages and zoom; do not render an unbounded zoomed page as one bitmap.
2. **Overlay layer:** Compose/Android Canvas draws in-progress strokes, highlights, text, handles, and selection feedback without mutating PDFBox or re-rendering PDFium on every pointer event.
3. **Interaction layer:** Receives gestures and performs hit-testing in the same normalized page coordinate space as the overlay.
4. **Commit pipeline:** On an explicit save/commit boundary, snapshot overlay state, convert coordinates, write standard PDFBox annotations off the main thread, persist/sync the result, reopen or invalidate the PDFium document, refresh affected tiles, then remove only successfully committed overlay items.

## Coordinate contract

- Store page positions as normalized top-left coordinates (`0..1`) in the domain/UI model.
- Convert using the actual page box used for rendering (normally CropBox, with MediaBox fallback), page rotation, and display transform. Do not hard-code screen DPI or assume every page is unrotated.
- For an unrotated page with width `W` and height `H` points: `pdfX = x * W`, `pdfY = (1 - y) * H`.
- Keep conversion helpers bidirectional and unit-test them for portrait, landscape, rotated, cropped, and non-standard pages.
- PDF text rectangles used for selection/highlighting must use the same transform as the rendered page.

## Performance and lifecycle rules

- Keep all file I/O, PDFBox parsing/saving, text extraction, and PDFium rendering on background dispatchers.
- Bound bitmap/tile memory; recycle or evict off-screen results and cancel obsolete render requests.
- Do not retain duplicate full-document byte arrays and heap-loaded PDFBox documents unnecessarily. Use PDFBox mixed memory settings or an equivalent bounded strategy for large files.
- Close/release PDFium documents, file descriptors, PDFBox documents, and temporary files on success, failure, and ViewModel close.
- Use stable cache keys for document version, page, viewport, zoom, and tile; invalidate after a successful commit.

## Current implementation review (2026-07-22)

Implemented: MVI/Clean layer interfaces, PDFium page rendering, Compose annotation overlays, normalized in-memory strokes/highlights/text notes, PDFBox `/Highlight`, `/Ink`, and `/Text` writing, background save/sync, and document reopen after save.

Not yet compliant with this contract:

- `PdfPage` renders one full page bitmap and applies Compose scaling; there is no tile cache, viewport rendering, bitmap eviction, or cancellation.
- A stroke is added to state only on drag end; there is no visible live-stroke overlay during the drag. Edits are not committed at interaction end; only `SaveAnnotations` performs the PDFBox flush.
- `PdfiumEngine` retains the complete PDF byte array and calls `PDDocument.load(ByteArrayInputStream(...))`; bounded `MemoryUsageSetting` is not used.
- Text boxes come from PDFBox `PDFTextStripper`, not PDFium `FPDFText_*` APIs.
- Annotation writing uses MediaBox and a simple Y flip. Rotation, CropBox, and renderer-specific transforms are not handled.
- `renderPageBitmap(..., false)` should be verified/configured to render embedded annotations; otherwise the post-save PDFium refresh will not show the baked annotations.
- The feature blueprint’s “shared-memory buffer” and tile-cache invalidation are not present; the current refresh is a document reopen plus `renderRevision`.

## Acceptance criteria

- Pointer interaction remains smooth while the base PDF bitmap stays unchanged.
- Committed annotations appear in PDFium and in another compliant PDF viewer at the same location and scale.
- Rotated/cropped pages pass coordinate and highlight tests.
- Large/zoomed pages stay within a documented memory budget without OOM.
- Failed save/sync preserves unsaved overlays and reports an actionable error.
- CI builds and device validation are performed through the repository’s GitHub Actions/release workflow; do not run Gradle locally.

## Agent instructions

Before structural changes, read `.github/copilot-instructions.md` and `.github/design.md`. Treat the “Current implementation review” as the source of truth: implement the gaps above incrementally, and update this section when behavior changes. Do not claim tile rendering, PDFium text extraction, rotation support, or automatic commit until verified in code and tests.
