# NoxReader

A lightweight, highly optimized Android PDF reading application built in Kotlin with Jetpack Compose. This project focuses purely on the reading experience, with deep optimizations for smoother UI/UX and low system resource management.

## Features
- **Fast PDF Rendering** via PDFium-Android.
- **Embedded Annotations**: Highlighting, custom color pen, and eraser tools (powered by Apache PDFBox, saved directly into the PDF).
- **Read Aloud**: Integration with Native Android TextToSpeech.
- **Google Drive Sync**: Direct opening and background saving back to Google Drive using the Android Storage Access Framework (SAF).

## Architecture
- **Clean Architecture + MVI**: Separation of concerns using Jetpack Compose, ViewModels (StateFlow/Intents), and domain repositories.
- Heavily utilizes Kotlin Coroutines and background execution to ensure the UI thread remains entirely unblocked during render calculations and content resolution.

## Development & Testing Workflow
> **Note:** There is **no local runtime environment** configured for compiling this app locally. 

- All compilation and APK aggregation is performed entirely through **GitHub Actions**.
- Pull Requests and commits to master trigger standard CI builds.
- Merging to main triggers a **GitHub Release**.
- Real-world testing is performed by downloading the split architectures APKs (e.g., pp-arm64-v8a-debug.apk) directly from GitHub Releases to an actual device.

## Contributing
Please consult .github/design.md and .github/copilot-instructions.md before making any structural changes to the codebase.
