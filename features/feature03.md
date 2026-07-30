# Feature 03 — Reselect an Existing Highlight

## Goal

When the user taps an existing text highlight, select it like the reference image (`implementation-files/image.png`):

- draw a blue dashed bounding box around the selected highlight;
- show a compact floating action menu near the selection;
- keep the PDF readable and avoid blocking work on the UI thread.

This feature is for selecting and acting on existing **text highlights**. It is not a new highlighting workflow.

## Design mapping

See [`.github/design.md`](../.github/design.md): **Embedded highlight selection**. Feature 03 consumes the same normalized-coordinate and save/reopen pipeline defined by Features 01 and 02.

## Verified implementation status (2026-07-23)

**Status: implemented for selection and delete; follow-up actions/validation remain.**

- `EmbeddedTextHighlight` caches page-scoped embedded PDF highlights off the main thread in `PdfiumEngine`.
- `SelectedHighlight`, `HighlightSource`, and `selectedHighlight` state distinguish session and embedded highlights.
- `HighlightHitTester` performs normalized hit-testing and chooses the smallest overlap candidate; JVM coverage includes overlap selection.
- Idle-tool taps dispatch `SelectHighlightAt`; outside taps clear selection.
- `SelectedHighlightOverlay` draws a translucent fill and dashed blue union bound, then places a clamped Delete menu above/below the selection.
- Delete updates session state immediately or records an embedded-highlight deletion for the next save. Save/reopen invalidates the cache to prevent duplication.

Remaining: change-color/comment actions, character-level geometry, tile-aware transforms, and device/external-viewer validation.

Relevant files:

- `presentation/mvi/AnnotationModels.kt`
- `presentation/mvi/PdfReaderState.kt`
- `presentation/mvi/PdfReaderIntent.kt`
- `presentation/mvi/PdfReaderViewModel.kt`
- `presentation/ui/PdfReaderScreen.kt`
- `data/pdfium/PdfiumEngine.kt`
- `data/pdfbox/PdfAnnotationWriter.kt`

## Required design

### 1. Annotation source and cache

Use a lightweight page-scoped `UIAnnotationModel` (or equivalent) containing at least:

- stable annotation id;
- page index;
- normalized display-space rectangles/quad points;
- annotation type.

Populate it off the main thread when a page becomes visible. Do not parse PDFBox annotations during a pointer callback. For the first implementation, a page `List` is sufficient; add spatial indexing only if profiling shows it is needed.

The cache must include highlights embedded in an opened PDF as well as highlights created in the current session. Avoid duplicate entries after saving/reopening.

### 2. Hit-testing

When no drawing tool is active, convert the tap from the page container into normalized display coordinates. Test the point against the candidate highlight rectangles/quad points. Prefer the smallest/most-specific matching annotation when highlights overlap.

On a hit, dispatch an intent such as `SelectHighlight(pageIndex, annotationId)`. A tap outside any highlight clears the selection and dismisses the menu.

Coordinate conversion must use the same `contentBounds` transform as rendering. Keep PDF-space conversion in the data/domain boundary; do not query PDFBox from Compose input handlers.

### 3. Selection overlay

Add selection state to `PdfReaderState`, preferably a nullable selected annotation reference plus its page. Render the selected highlight above the page and below/alongside the input layer:

- union all selected highlight rectangles for the visual bounds;
- transform normalized rectangles using `contentBounds`;
- draw a subtle translucent selection fill;
- draw a blue dashed `Stroke` outline;
- clear it when the page changes, selection is dismissed, or an action completes.

The overlay must remain aligned with the bitmap and existing annotation canvas during fit, rotation, and zoom. Do not mutate the PDF merely because an annotation was selected.

### 4. Floating action menu

Use a Compose `Popup`/`DropdownMenu` or an equivalent overlay consistent with the existing Compose UI. Anchor it to the transformed selection bounds, clamp it to the window, and place it above unless there is insufficient space, then place it below.

Initial actions should be explicit intents and may be implemented incrementally:

- `Highlight` / change highlight color;
- `Add comment` (only if comment behavior is defined);
- `Delete`;
- optional `More actions`.

Do not add placeholder actions that have no implemented behavior.

### 5. Mutation and rendering

Actions must update the in-memory state first. Perform PDFBox writes and SAF sync on `Dispatchers.IO`. After a persisted mutation, reopen/invalidate the affected page and re-render. Do not assume a public PDFium tile-cache invalidation API exists; use the repository’s existing reopen plus `renderRevision` mechanism unless a concrete API is verified.

Preserve existing annotations when writing. Use stable ids/metadata so a save/reopen cycle can map embedded annotations back to UI models.

## Acceptance criteria

1. Open a PDF containing an embedded text highlight; tap inside it; the correct highlight is selected.
2. The selected bounds and dashed outline align with every highlight rectangle, including multi-line highlights.
3. The menu stays within the window and flips above/below as needed.
4. Tapping outside dismisses selection/menu without changing the PDF.
5. Delete or color-change updates the UI immediately and persists correctly after save/reopen.
6. Existing and newly-created highlights do not duplicate after save/reopen.
7. Pointer handling, rendering, PDF parsing, and saving remain non-blocking on the main thread.
8. Add unit tests for coordinate conversion and hit-testing, including overlap, page rotation, and multi-rect highlights.

## Implementation order

1. Add models, state, and intents for selection/menu/action results.
2. Load/cache embedded highlight metadata and merge it with session highlights.
3. Implement normalized-coordinate hit-testing in the reader.
4. Render the dashed selection overlay and anchored menu.
5. Implement one complete mutation path (delete is the smallest), then color/comment actions.
6. Reopen/invalidate affected pages after persistence.
7. Add tests and verify through CI; do not run Gradle locally per repository instructions.
