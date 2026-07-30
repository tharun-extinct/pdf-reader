# NoxReader Feature Blueprints

These blueprints describe independently deliverable reader behavior. Shared
technical rules live in [`.github/architecture.md`](../.github/architecture.md);
blueprints explain how those rules affect one feature without redefining them.

## Loading protocol

1. Classify the request by task intent, not only by the files it mentions.
2. Load the single primary blueprint from the table below.
3. Read only the architecture sections linked by that blueprint.
4. Inspect its relevant implementation and tests.
5. Load an impact-check blueprint only if the proposed change touches the
   shared behavior named in the final column.

If no router row matches, do not force a blueprint match. Isolated CI,
dependency, or general documentation work should inspect its directly relevant
files and load architecture context only when it changes a shared contract.

Load the complete architecture and all affected blueprints for structural,
coordinate-system, persistence-wide, or otherwise ambiguous changes. Several
features share `PdfReaderScreen.kt` and `PdfReaderViewModel.kt`, so source paths
alone are not reliable routing signals.

## Task router

| Task concepts and synonyms | Primary blueprint | Architecture sections | Principal code areas | Impact checks |
|---|---|---|---|---|
| render, bitmap, PDFium, page, zoom, viewport, tile, cache, cancellation | [`pdf-rendering.md`](pdf-rendering.md) | [Rendering and overlay model](../.github/architecture.md#rendering-and-overlay-model), [Performance and lifecycle](../.github/architecture.md#performance-and-lifecycle) | `PdfiumEngine`, reader page UI, render intents | Annotation blueprints when overlay alignment or render invalidation changes |
| save, flatten, editable, PDFBox, SAF, sync, reopen, appearance stream | [`annotation-persistence.md`](annotation-persistence.md) | [Commit pipeline](../.github/architecture.md#commit-pipeline), [Save modes](../.github/architecture.md#save-modes), [Failure handling and invalidation](../.github/architecture.md#failure-handling-and-invalidation) | annotation writer/saver, sync manager, ViewModel save flow | All annotation blueprints whose persisted representation changes |
| highlight, selection, text geometry, quad points, highlight color, embedded highlight | [`text-highlighting.md`](text-highlighting.md) | [Coordinate system](../.github/architecture.md#coordinate-system), [Text geometry and highlighting](../.github/architecture.md#text-geometry-and-highlighting), [Rendering and overlay model](../.github/architecture.md#rendering-and-overlay-model) | selector, hit tester, reader overlays, embedded-highlight cache | Persistence when save/delete behavior or PDF output changes |
| pen, ink, stroke, freehand highlight, eraser, pressure, width | [`freehand-annotation.md`](freehand-annotation.md) | [Coordinate system](../.github/architecture.md#coordinate-system), [Rendering and overlay model](../.github/architecture.md#rendering-and-overlay-model) | annotation models/intents, reader gestures, ink writer | Persistence when `/Ink`, flattening, or save behavior changes |
| text note, sticky note, comment, note popup, note icon | [`text-notes.md`](text-notes.md) | [Coordinate system](../.github/architecture.md#coordinate-system), [Rendering and overlay model](../.github/architecture.md#rendering-and-overlay-model) | annotation models/intents, note editor UI, text-annotation writer | Persistence when `/Text`, appearance, flattening, or save behavior changes |

## Status vocabulary

- **Planned:** specified but not implemented.
- **Partial:** some required behavior exists, with named gaps.
- **Implemented:** present in code and covered by proportionate automated checks.
- **Verified:** implemented and validated in the environments named by the blueprint.

Code and verified tests are authoritative when status text is stale. Update a
blueprint's status and `Last verified` entry whenever relevant behavior changes.

## Dependency map

```text
pdf-rendering
    ├── text-highlighting ──┐
    ├── freehand-annotation ├── annotation-persistence
    └── text-notes ─────────┘
```

Rendering supplies the common page and overlay coordinate surface. Each
annotation feature owns its interaction behavior and optimistic state.
Persistence owns the shared durable-write workflow.

## Routing examples

| Request | Expected context |
|---|---|
| Fix multiline highlight selection | `text-highlighting.md` plus its coordinate, text-geometry, and overlay architecture links |
| Add pressure-sensitive pen strokes | `freehand-annotation.md` plus coordinate and overlay contracts; persistence only if the durable model changes |
| Fix flattened annotation save-back | `annotation-persistence.md`, the complete commit/save contracts, and impact checks for every affected annotation type |
| Change normalized coordinates | Complete `architecture.md` plus every coordinate-consuming blueprint |
| Change text-note popup styling | `text-notes.md` plus its overlay contract; no highlighting or freehand blueprint |
