# NoxReader

NoxReader is a local-first Android PDF reader built with Kotlin and Jetpack Compose. It combines a calm bookshelf and reading experience with PDF annotations that can be saved back to the file selected through Android's Storage Access Framework.

## Highlights

- **Focused library:** recent documents, animated reading progress, resume position, and page bookmarks.
- **Fast page rendering:** PDFium-backed full-page bitmap rendering with horizontal paging and 1x-5x pinch zoom.
- **PDF annotations:** pen, text-aware highlighter, eraser, editable text notes, custom color palettes, and selection/deletion of existing highlights.
- **Two save modes:** keep new annotations editable or flatten supported new marks into page content.
- **Read aloud:** Android TextToSpeech with play, pause, resume, stop, adjustable speed, and synchronized word-area highlighting.
- **Comfort controls:** System, Light, and Dark themes plus an optional keep-screen-awake setting.
- **Provider-friendly storage:** open local or cloud-provider PDFs through SAF and save changes back when the provider grants write access.

## Experience

### Bookshelf

NoxReader opens to a responsive library with a prominent **Continue reading** card, recent-document list, saved progress, bookmark counts, clear empty/loading/error states, and a system document picker.

Only lightweight metadata is stored by NoxReader: the persisted document URI, title, page count, last page, last-opened time, bookmarks, and reader preferences. PDF contents stay at the location chosen by the user.

### Reader

The reader provides:

- horizontal page navigation;
- page title and `Page N of M` status;
- pinch zoom;
- text selection;
- current-page bookmarking;
- a contextual floating annotation toolbar;
- explicit editable/flattened mode and Save action;
- optimistic annotation overlays that remain available if saving fails.

Opening, rendering, saving, and read-aloud expose visible states. Failed document loads provide a **Choose another PDF** recovery path.

### Settings

Settings include theme selection, screen-awake behavior, speech rate from 0.6x to 1.6x, local-history information, and a confirmed **Clear recent history** action. Clearing history never deletes PDF files.

## Annotation behavior

NoxReader keeps interaction responsive by drawing new annotations in Compose first. When Save is selected:

1. PDFBox Android writes annotations into a temporary PDF.
2. The updated file is synced to the original SAF URI.
3. The document is reopened and the page render revision is refreshed.
4. Pending overlays are cleared only after the source reopens successfully.

Save modes:

- **Editable** retains new highlight, ink, and text-note annotation objects.
- **Flattened** paints newly created supported marks into page content and removes only those newly created annotation entries. Flattening is irreversible.

Read-only providers may allow a PDF to open but reject save-back.

## Architecture

NoxReader uses Clean Architecture boundaries with an MVI-style Compose presentation layer.

```text
Compose UI
  -> PdfReaderIntent
  -> PdfReaderViewModel
  -> domain contracts
  -> PDFium / PDFBox / SAF / SharedPreferences

Compose UI <- StateFlow<PdfReaderState>
```

| Area | Responsibility |
|---|---|
| Presentation | Compose screens, theme, immutable state, intents, gestures, optimistic overlays |
| Domain | PDF engine, annotation saver, sync, library, and model contracts |
| Data | PDFium rendering, PDFBox parsing/writing, SAF sync, local metadata persistence |

PDFium renders page bitmaps. PDFBox Android extracts positioned text, reads embedded highlights, and writes PDF changes. UI coordinates are normalized and converted through one shared CropBox/rotation-aware mapper.

For the complete product and engineering contract, see [`.github/design.md`](.github/design.md). Contributors and coding agents should also read [`.github/copilot-instructions.md`](.github/copilot-instructions.md).

## Technology

- Kotlin 1.9.22 and Java 17
- Jetpack Compose, Material 3, Navigation, ViewModel, StateFlow
- Android minSdk 26; compile/targetSdk 34
- PDFium Android 1.9.0
- PDFBox Android 2.0.27.0
- Android TextToSpeech
- Storage Access Framework
- SharedPreferences for small local metadata

## Project structure

```text
app/src/main/java/com/pdfreader/app/
├── data/
│   ├── pdfbox/       PDF coordinate mapping and annotation writing
│   ├── pdfium/       Rendering, text extraction, embedded highlights
│   ├── preferences/  Recent documents and preferences
│   └── sync/         SAF copy and save-back
├── domain/           Models and stable contracts
├── presentation/
│   ├── mvi/          State, intents, ViewModel, selection logic
│   ├── theme/        Color, typography, and spacing tokens
│   └── ui/           Bookshelf, reader, and settings screens
└── MainActivity.kt   Navigation and dependency composition
```

## Build and testing workflow

This repository intentionally uses GitHub Actions as the build/test authority. Do not run Gradle locally.

- Pushes and pull requests to `master` run [`.github/workflows/build.yml`](.github/workflows/build.yml), which assembles a release APK and uploads it as a 14-day artifact.
- Pushes to `main` or `feature` run [`.github/workflows/gh-release.yml`](.github/workflows/gh-release.yml), which assembles and publishes a GitHub Release APK.
- Pushes to `gptoss` use the separate gptoss release workflow.
- JVM tests cover coordinate mapping, overlapping highlight selection, and wrapped/reverse text-highlighter selection.

The current release build uses the debug signing configuration. Configure a protected production keystore before distributing through an app store.

To test on a device:

1. Open the relevant GitHub Actions run or GitHub Release.
2. Download `pdf-reader-release-apk` or the attached release APK.
3. Install the APK on an Android 8.0 (API 26) or newer device.
4. Open PDFs through the system picker and verify save-back using both local and cloud document providers.

## Current limitations

- Rendering uses full-page bitmaps; tile rendering, bounded bitmap eviction, and render cancellation are not yet implemented.
- Zoom supports scaling but not a complete pan/reset interaction.
- Long-document thumbnail or scrubber navigation is not yet available.
- Cross-viewer validation for every editable/flattened annotation type is still in progress.
- Device and Compose UI automation do not yet cover all gestures, rotations, provider failures, and accessibility scenarios.

## Contributing

Before changing UI, architecture, storage, annotations, or build workflows:

1. read [`.github/design.md`](.github/design.md);
2. read [`.github/copilot-instructions.md`](.github/copilot-instructions.md);
3. keep PDF work off the main thread;
4. preserve the shared coordinate-mapping and save/reopen contracts;
5. update documentation when user-visible behavior or delivery rules change.
