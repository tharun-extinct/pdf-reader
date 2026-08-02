# Read Aloud

## Outcome

Read the current PDF page with controllable playback and synchronized text
highlighting while releasing Android TextToSpeech resources when reading ends.

## Current verified status

**Status: Partial - code and test tree inspected 2026-08-03; no dedicated
automated tests found.**

- `TtsManager` initializes Android TextToSpeech with `Locale.US` and publishes
  Idle, Playing, Paused, and Error states through `StateFlow`.
- Playback uses extracted page text boxes, chunks at sentence-ending punctuation,
  and applies a 3000-character fallback split.
- Character-range callbacks map spoken ranges back to normalized text-box bounds
  for a Compose overlay on the active page.
- The reader exposes play, pause, resume, and stop controls. Leaving the Read
  aloud tool, closing the document, or clearing the ViewModel stops playback.
- Speech rate is applied from reader preferences and clamped to `0.6f..1.6f`.
- Playback is page-scoped; it does not continue to the next page, detect document
  language, or resume at an exact word after pause.
- Opening a replacement PDF resets observable TTS state but does not explicitly
  stop the platform engine first.

## Architecture dependencies

- [Component model and state ownership](../.github/architecture.md#component-model-and-state-ownership)
- [Coordinate system](../.github/architecture.md#coordinate-system)
- [Text geometry and highlighting](../.github/architecture.md#text-geometry-and-highlighting)
- [Rendering and overlay model](../.github/architecture.md#rendering-and-overlay-model)
- [Performance and lifecycle](../.github/architecture.md#performance-and-lifecycle)

## Feature-specific implications

### Component model and state ownership

- `TtsManager` owns the platform engine, chunks, current chunk index, and spoken
  range; `PdfReaderViewModel` mirrors only observable playback state into UI
  state.
- Playback controls dispatch intents. Compose must not call or retain the
  platform TTS engine directly.
- Starting a new playback request replaces the active utterance rather than
  creating concurrent narration.

### Coordinate system and text geometry

- Spoken-range highlights use the extracted `PdfTextBox` bounds unchanged in
  normalized top-left page space.
- The overlay maps those bounds through the same fitted page `contentBounds` as
  selection and annotations; pager padding and letterboxing are not part of the
  durable geometry.
- Chunk text-to-box indices must remain aligned when whitespace or punctuation is
  inserted between extracted boxes.

### Rendering and overlay model

- Narration highlighting is a transient Compose overlay and never changes the
  PDF or annotation state.
- Only Playing or Paused bounds for the matching page may be drawn.
- Selecting another reader tool stops narration and clears active highlight
  state.

### Performance and lifecycle

- Text extraction must remain off the main thread and may be reused from the
  per-document page-text cache.
- Stop is required on tool exit and document close; shutdown is required when the
  ViewModel is cleared.
- Initialization, missing-language, and playback failures must become observable
  states rather than uncaught platform errors.

## Related blueprints

### Required

- [PDF rendering](pdf-rendering.md) for the page transform used by synchronized
  highlights.

### Impact checks

- [Reader preferences](reader-preferences.md) when speech-rate bounds, defaults,
  or persistence change.
- [Text highlighting](text-highlighting.md) when shared text geometry or overlay
  ordering changes.

## Relevant implementation and tests

- `app/src/main/java/com/pdfreader/app/domain/tts/TtsManager.kt` - platform TTS
  lifecycle, chunking, range mapping, and playback state.
- `app/src/main/java/com/pdfreader/app/presentation/mvi/PdfReaderIntent.kt` -
  playback commands.
- `app/src/main/java/com/pdfreader/app/presentation/mvi/PdfReaderState.kt` -
  observable TTS state.
- `app/src/main/java/com/pdfreader/app/presentation/mvi/PdfReaderViewModel.kt` -
  state collection, rate application, and stop/shutdown integration.
- `app/src/main/java/com/pdfreader/app/presentation/ui/PdfReaderScreen.kt` -
  controls, text request, page matching, and highlight overlay.
- No dedicated chunking, state-transition, range-mapping, lifecycle, or Compose
  control tests currently exist under `app/src/test`.

## Acceptance criteria

- [ ] Play reads the current page in extraction order and exposes a visible
  Playing state.
- [ ] Spoken-range highlights align with fitted, rotated, and letterboxed pages.
- [ ] Pause, resume, stop, tool changes, document replacement, close, and
  ViewModel destruction have defined and tested state transitions.
- [ ] Empty text, initialization failure, unsupported language, and playback
  errors are visible and recoverable.
- [ ] Long unpunctuated text is chunked within a documented engine-safe limit
  without losing or reordering text-box mappings.
- [ ] Speech-rate changes affect active and subsequent playback within the
  configured bounds.
- [ ] TTS never mutates PDF contents or persistent annotation state.

## Remaining gaps

- Pause uses `stop()` and resume restarts the current chunk rather than the exact
  spoken range.
- No automatic next-page continuation, page-turn coordination, or progress
  position across pages.
- Language is fixed to US English; there is no detection, selection, or installed
  voice/data recovery flow.
- The 3000-character fallback is checked after each complete text box, so one
  unusually large box can still exceed that threshold.
- No automated tests around chunking, mapping, platform callbacks, or lifecycle.
- Spoken-range alignment inherits the current MediaBox-based text extraction;
  cropped and rotated page alignment is not fixture-verified.
- Document replacement can leave the previous utterance playing underneath the
  newly opened document because `openPdf()` does not call `TtsManager.stop()`.
- A Play request made before TTS initialization completes is ignored without an
  observable loading or retry state.
