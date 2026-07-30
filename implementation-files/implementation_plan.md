# Text-to-Speech (TTS) Perfect Sync Highlighting

The current TTS implementation has several issues:
1. It joins all text boxes with a space (`joinToString(" ")`), removing paragraph breaks. `TtsManager` splits by `\n\n`, which means the whole page becomes a single massive chunk. This can exceed the Android TTS engine's maximum character limit, causing it to fail or only read the title.
2. It hardcodes `pageIndex = 0`, so it doesn't read the currently visible page.
3. The UI highlighting uses a simplistic vertical spacing heuristic that doesn't sync with what is currently being spoken.

To achieve the "perfect sync" highlighting similar to Microsoft Edge, we need to precisely map the audio output to the PDF text bounds on the screen.

## User Review Required

I propose implementing **Approach 1 (Word-Level Sync)** to provide the best user experience, matching the Microsoft Edge reference. Please review the approaches below and approve the plan, or let me know if you prefer Approach 2.

## Open Questions

1. Do you want the highlight to be just the current word (like Edge), or the entire sentence being read? (Approach 1 supports both by highlighting the sentence with a lighter color and the current word with a darker color, but let me know your preference).

## Proposed Changes

### Approach 1: Word-Level Sync using `onRangeStart` (Recommended)

This approach uses Android's modern TTS API to get the exact character index of the word currently being spoken. We map these characters back to the PDF text bounds.

1. **`PdfReaderIntent` & `PdfReaderViewModel`**:
   - Update `PlayTts` to accept the current `pageIndex` and the `List<PdfTextBox>`.
   - The ViewModel will dynamically fetch the current visible page instead of hardcoding `0`.

2. **`TtsManager`**:
   - Accept a structured text object instead of a raw `String`. We will stitch the text boxes together while keeping an index map.
   - Implement `UtteranceProgressListener.onRangeStart` (API 26+) to get the start and end character index of the word being spoken.
   - Update `TtsState.Playing` to emit the list of `Rect` (bounding boxes) that correspond to the currently spoken word/sentence.

3. **`PdfReaderScreen`**:
   - Update `TtsControlsOverlay` to use the currently visible `pageIndex` from the Pager state.
   - Update `SelectableTextLayer` to draw precise highlighting rectangles emitted by `TtsState` instead of guessing paragraphs.

### Approach 2: Sentence-by-Sentence Sync

This approach manually splits the text into sentences and sends them one by one to the TTS engine.

- **Pros**: Simpler to implement on older Android versions where `onRangeStart` might be buggy.
- **Cons**: Can introduce slight pauses between sentences. Doesn't support precise word-level highlighting (only sentence-level).

## Verification Plan

### Manual Verification
- Open a PDF with multiple pages.
- Scroll to a specific page and press "Read Aloud".
- Verify that it reads the text of the *current* page.
- Verify that the highlighted text on the screen perfectly matches the words being spoken by the TTS engine.
- Verify that play, pause, and stop controls work correctly during synchronized playback.
