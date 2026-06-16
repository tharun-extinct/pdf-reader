# PDF Reader Implementation Progress

This document tracks the features and architectural components that have been implemented in the PDF Reader app so far.

## Core Architecture
* **Clean Architecture & MVI:** Structured the app into Presentation (Compose + MVI), Domain (Repositories), and Data (Implementations) layers.
* **State Management:** Fully integrated `PdfReaderViewModel` using `StateFlow` and intent-driven events (`PdfReaderIntent`, `PdfReaderState`).

## PDF Engine & Rendering
* **PDFium-Android Integration:** `PdfiumEngine` is actively implemented to open documents, manage page counts, calculate optimal dimensions, and render individual pages into native memory using high-quality `ARGB_8888` bitmaps.
* **Tiled Paging UI:** A Jetpack Compose `HorizontalPager` UI efficiently requests rendering based on physical display sizes while preventing excessive memory overhead.

## Annotations & Highlighting
* **Annotation Models:** Integrated `FreehandStroke` and `TextAnnotation` definitions. Configured basic tools including `Pen`, `Highlighter`, `Eraser`, and `AddText`.
* **State & Tool Selection:** Hooked up the state model and ViewModel so that user-selected tools are retained. 
* **Custom Color Palettes:** Introduced functional defaults and configuration interfaces for four unique `Pen` and `Highlighter` colors via hex code matching.
* **Canvas Gesture Layer:** Added the foundation of `AnnotationGestureLayer` inside the PDF viewer to pick up dragging and tap inputs (`detectDragGestures`, `detectTapGestures`). Stroke creation, path drawing, text boxes, and eraser intersections are integrated.

## Storage & Sync
* **Storage Access Framework (SAF) Integrations:** Implemented `SafPdfSyncManager` capable of capturing PDF streams from providers like Google Drive into a local cache, and successfully pushing modifications back up without data loss via truncation (`"wt"` mode).

## CI / CD
* **GitHub Actions:**
  * Configured `build.yml` for automated building of the debug APK on PRs and commits to `main`/`master`.
  * Configured `gh-release.yml` to attach the assembled debug APK directly to published GitHub Releases.
  * Successfully stripped local instrumentation test dependencies for faster, non-test compilation loops.