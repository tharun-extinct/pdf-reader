# NoxReader issue log

This file records defects and engineering problems encountered during development. Product enhancements that have not yet been implemented are listed separately under **Open issues**.

## Closed issues

### Release build: invalid `animateFloatAsState` import

**Status:** Closed

**Symptom:** `:app:compileReleaseKotlin` failed in `BookshelfScreen.kt` because `animateFloatAsState` could not be resolved.

> **Fix:** Imported `animateFloatAsState` from `androidx.compose.animation.core`, which is the package used by the project's Compose version.

**Verification:** The unresolved reference no longer appears in the targeted Kotlin compilation check. The authoritative release build remains GitHub Actions.

### Release build: unsupported PDFBox appearance API

**Status:** Closed

**Symptom:** `:app:compileReleaseKotlin` failed in `PdfAnnotationWriter.kt` because `PDAnnotationText.createRectangleAppearance` is not available in PDFBox Android `2.0.27.0`.

> **Fix:** Replaced the unavailable call with `constructAppearances(document)`, the compatible API for generating the text annotation appearance.

**Verification:** The unresolved reference no longer appears in the targeted Kotlin compilation check. The authoritative release build remains GitHub Actions.

### Release build: invalid collection mutation in text selection

**Status:** Closed

**Symptom:** Kotlin compilation failed around `result += mutableListOf()` while grouping character rectangles into text lines.

> **Fix:** Replaced the ambiguous `+=` expression with the explicit mutation `result.add(mutableListOf())`.

### Native PDF library packaging configuration

**Status:** Closed

**Symptom:** PDFium's native libraries required legacy extraction behavior, while the manifest-level `android:extractNativeLibs` configuration was deprecated and conflicted with modern Android Gradle Plugin packaging.

> **Fix:** Removed `android:extractNativeLibs` from `AndroidManifest.xml` and configured `packaging.jniLibs.useLegacyPackaging = true` in `app/build.gradle.kts`.

**Note:** Warnings that some PDFium `.so` files cannot be stripped are non-fatal; Gradle packages those libraries unchanged.

### Annotations could target or overwrite the wrong PDF

**Status:** Closed

**Symptom:** Annotation output was previously associated with temporary mobile storage, creating a risk that saving one document could overwrite another document.

> **Fix:** The reader now keeps the active document's Storage Access Framework URI, writes edits to a unique temporary output file, truncates and syncs that output back only to the active source URI, then reopens the same document. Temporary files are deleted on completion and failure.

### Read-aloud highlight was misaligned with the PDF page

**Status:** Closed

**Symptom:** The spoken-text highlight floated away from the rendered text when the PDF page was letterboxed inside its container.

> **Fix:** Text geometry is stored in normalized page coordinates and the read-aloud overlay is transformed through the fitted PDF bounds rather than the full Compose container.

### Multi-line text highlighting produced incorrect appearance

**Status:** Closed

**Symptom:** Drag selection could group text incorrectly, and a highlight's union rectangle could paint over the whitespace and text between selected lines.

> **Fix:** Added ordered character-to-line grouping with forward and reverse selection support. Saved highlights use per-line quad points and appearances; the union rectangle is retained only as annotation metadata.

**Verification:** JVM coverage was added for contiguous, wrapped, and reverse text selection.

### Reader persistence and invalid-document edge cases

**Status:** Closed

**Symptom:** Recent-document progress could be inaccurate, empty PDFs could enter a partially loaded state, and a failed open could leave the PDF engine holding stale resources.

> **Fix:** Progress now uses the completed-page count and is clamped to `0..1`. Documents with no readable pages are rejected with a recoverable message. Failed opens close the engine and reset the loaded state.

### Preference repository return-type mismatch

**Status:** Closed

**Symptom:** Synchronized SharedPreferences writes returned the result of `commit()` where repository functions were expected to return `Unit`.

> **Fix:** Completed the synchronized write blocks with an explicit `Unit` value after committing.

### Feature branch integration

**Status:** Closed

**Symptom:** Recent UI/UX and reader work existed on `feature` and needed to be preserved while synchronizing and integrating it with `master`.

> **Fix:** Updated the feature branch, resolved the integration without discarding the recent UI/UX work, and merged `feature` into `master`.

## Open issues

### Large-document rendering and memory pressure

**Status:** Open

**Impact:** Full-page bitmaps can consume substantial memory and rendering work on large or image-heavy PDFs.

> **Next action:** Add render cancellation, bounded bitmap eviction, and tiled rendering for zoomed pages; validate the result on representative large documents.

### Zoom navigation is incomplete

**Status:** Open

**Impact:** Pinch zoom is available, but deliberate pan behavior and an obvious reset-to-fit action still need refinement.

> **Next action:** Define pan-versus-page-swipe gesture ownership, constrain page bounds, and add an accessible reset-to-fit control.

### Fast navigation through long PDFs

**Status:** Open

**Impact:** Horizontal paging alone is inefficient for documents with many pages.

> **Next action:** Add a page scrubber or page-number jump control while preserving the current reading position and accessibility semantics.

### Release and device validation

**Status:** Open

**Impact:** PDF rendering, native-library loading, Storage Access Framework write-back, and annotation compatibility can vary by Android version, device, and external PDF viewer.

> **Next action:** Run the GitHub Actions release build, install the produced APK on physical devices, and verify editable and flattened saves in at least one external PDF viewer. Record any device-specific failures here.
