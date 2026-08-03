# Read Aloud

## Outcome

Read the current PDF page with controllable playback and synchronized text
highlighting while releasing Android TextToSpeech resources when reading ends.

## Current verified status

**Status: Partial - code and test tree inspected 2026-08-04; pure navigation
tests added but not run locally because GitHub Actions is the build authority.**

- `TtsManager` initializes Android TextToSpeech with `Locale.US` and publishes
  Idle, Playing, Paused, PageCompleted, and Error states through `StateFlow`.
- Playback infers paragraphs from extracted text-box line geometry and caps every
  engine utterance at 3000 characters, including an unusually large single box.
- Character-range callbacks are matched to unique utterance IDs, map spoken
  ranges back to normalized text boxes, and expand the active word to its line
  bounds for a synchronized Compose overlay.
- The reader exposes play, pause, resume, and stop controls. Leaving the Read
  aloud tool, closing the document, or clearing the ViewModel stops playback.
- Previous/next paragraph controls navigate the inferred paragraph chunks, and
  page completion scrolls to and starts the next non-terminal page automatically.
- Speech rate is applied from reader preferences, clamped to `0.6f..1.6f`, and
  selectable from the in-reader controls.
- Opening a replacement PDF explicitly stops the previous platform utterance.
- Playback does not detect document language or resume at an exact word after
  pause.

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
- `app/src/main/java/com/pdfreader/app/domain/tts/TtsTextNavigator.kt` - pure
  paragraph inference, engine-safe chunk construction, and line highlight
  geometry.
- `app/src/main/java/com/pdfreader/app/presentation/mvi/PdfReaderIntent.kt` -
  playback commands.
- `app/src/main/java/com/pdfreader/app/presentation/mvi/PdfReaderState.kt` -
  observable TTS state.
- `app/src/main/java/com/pdfreader/app/presentation/mvi/PdfReaderViewModel.kt` -
  state collection, rate application, and stop/shutdown integration.
- `app/src/main/java/com/pdfreader/app/presentation/ui/PdfReaderScreen.kt` -
  controls, text request, page matching, and highlight overlay.
- `app/src/test/java/com/pdfreader/app/domain/tts/TtsTextNavigatorTest.kt` -
  character-map alignment, paragraph-gap detection, and line expansion.
- No automated platform callback, lifecycle, ViewModel page-turn, or Compose
  control coverage currently exists.

## Acceptance criteria

- [x] Play reads the current page in extraction order and exposes a visible
  Playing state.
- [x] Spoken-range highlights use the fitted page transform and callback-specific
  character mapping; cropped/rotated fixture verification remains outstanding.
- [ ] Pause, resume, stop, tool changes, document replacement, close, and
  ViewModel destruction have defined and tested state transitions.
- [ ] Empty text, initialization failure, unsupported language, and playback
  errors are visible and recoverable.
- [x] Long unpunctuated text is chunked within a documented engine-safe limit
  without losing or reordering text-box mappings.
- [x] Speech-rate changes affect active and subsequent playback within the
  configured bounds.
- [x] TTS never mutates PDF contents or persistent annotation state.
- [x] Previous/next controls navigate inferred paragraphs, and completion turns
  the page and continues narration until the document ends; automated
  ViewModel/page-turn coverage remains outstanding.

## Remaining gaps

- Pause uses `stop()` and resume restarts the current chunk rather than the exact
  spoken range.
- Paragraph boundaries are geometry heuristics because PDF text extraction does
  not expose semantic paragraph markers; unusual multi-column or tightly spaced
  layouts need fixture verification.
- Language is fixed to US English; there is no detection, selection, or installed
  voice/data recovery flow.
- No automated tests around platform callbacks, lifecycle, automatic page turns,
  or the Compose controls.
- Spoken-range alignment inherits the current MediaBox-based text extraction;
  cropped and rotated page alignment is not fixture-verified.
- A Play request made before TTS initialization completes is ignored without an
  observable loading or retry state.
