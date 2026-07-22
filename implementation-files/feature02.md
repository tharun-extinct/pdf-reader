# Feature 02 — Persistent PDF annotations

## Problem and target

In-memory annotations are visible in the Compose overlay but must survive save/reopen and render consistently in Adobe Acrobat, PDFium, Drive preview, and other compliant viewers.

Support two explicit save modes:

1. **Embedded/editable annotations:** keep `/Annots` entries, with correct `/Rect`, `/QuadPoints` where applicable, color/opacity, and explicit `/AP` normal appearances.
2. **Flattened/burned-in annotations:** paint each supported annotation appearance into the page content stream, then remove the corresponding `/Annots` entries. Flattening is irreversible and must be a separate user action or option.

## Verified implementation status (2026-07-22)

| Area | Status | Evidence / truth |
|---|---|---|
| In-memory pen, highlight, and text-note models | Partial | `AnnotationModels.kt`, `PdfReaderViewModel.kt` |
| Highlighter selection | Partial | `PdfReaderScreen.kt` selects intersecting **word** boxes from PDFBox text extraction and stores normalized rectangles. It is not character-level or line-segment geometry. |
| PDFBox `/Highlight` annotations | Implemented, incomplete | `PdfAnnotationWriter.kt` writes `/Rect`, `/QuadPoints`, RGB color, and opacity. It creates no `/AP` appearance stream. |
| PDFBox `/Ink` annotations | Implemented, incomplete | Writes `/InkList`, rectangle, color, and width metadata; no explicit appearance stream. |
| PDFBox `/Text` notes | Implemented | Writes note contents, icon rectangle, color, and closed state. |
| Save to a new PDF and SAF sync-back | Implemented | `PdfAnnotationWriterImpl.kt` plus `PdfReaderViewModel.saveAnnotations()`. |
| Reopen and rerender after save | Implemented in intent | ViewModel reopens the source URI and increments `renderRevision`; verify with an actual viewer/device test. |
| True flattening | **Not implemented** | No code paints annotation appearances into `/Contents` or removes `/Annots`. |
| Adobe-level cross-viewer fidelity | **Not verified** | No golden PDFs, PDF object assertions, or Acrobat/PDFium/Drive comparison tests are present. |

## Required engineering rules

- Coordinates are stored normalized, top-left origin. Convert to PDF points with `x = xNorm * pageWidth` and `y = (1 - yNorm) * pageHeight`.
- For each highlight line segment, emit eight `/QuadPoints` values in this order: upper-left, upper-right, lower-left, lower-right. `/Rect` must enclose every quad.
- A multi-line highlight is one markup annotation containing multiple 8-value quads; do not use one large rectangle spanning whitespace and unrelated lines.
- Generate an explicit `/AP` normal appearance for highlights, ink, and notes. Keep appearance-stream coordinates and transformation aligned with the annotation rectangle; validate clipping and opacity.
- Never claim that an annotation is flattened merely because it is saved into the PDF. Flattening requires drawing into page `/Contents` and removing the annotation entry.
- Preserve existing annotations and form/signature data unless the selected operation explicitly replaces them. Save atomically and do not clear the in-memory overlay until sync and reopen succeed.

## Implementation order

1. Add a domain-level save mode (`Editable` / `Flattened`) and expose it in the save flow.
2. Replace word-box selection with character/word geometry grouped into line segments. Handle wrapped, rotated, and empty selections; add coordinate conversion tests.
3. Add tested `/AP` generation for highlight, ink, and text annotations. Keep `/QuadPoints` and `/Rect` consistent.
4. Implement flattening with PDFBox content streams using the correct page/annotation transformation, then remove only the flattened annotations.
5. Add JVM/PDF fixture tests that inspect `/Annots`, `/AP`, `/QuadPoints`, `/Rect`, `/Contents`, and reopenability. Validate output in PDFium and at least one external viewer. Do not run Gradle locally; use CI.

## Acceptance criteria

- Save/reopen preserves every supported annotation on the same page and location.
- A two-line highlight has two quads, no highlight in the inter-line whitespace, and renders with the chosen opacity.
- Editable output retains selectable/deletable annotations.
- Flattened output has no selected annotation entries, visibly contains the marks in page content, and cannot be selected as annotations.
- Existing PDF annotations remain intact unless explicitly selected for flattening.
- Failed save or sync leaves the source and in-memory state unchanged.

## Source of truth

Relevant code: `app/src/main/java/com/pdfreader/app/data/pdfbox/PdfAnnotationWriter.kt`, `PdfAnnotationWriterImpl.kt`, `app/src/main/java/com/pdfreader/app/presentation/mvi/PdfReaderViewModel.kt`, `app/src/main/java/com/pdfreader/app/presentation/ui/PdfReaderScreen.kt`, and `app/src/main/java/com/pdfreader/app/data/pdfium/PdfiumEngine.kt`.
