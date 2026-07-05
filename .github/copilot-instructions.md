---
applyTo: "**/*"
name: PDF Reader Android App
description: Instructions for developing the highly optimized Android PDF Reader
---

# PDF Reader — Agent Instructions

A lightweight, high-performance Android PDF reader focused purely on **reading and annotating**. This is a compute-intensive app, so **performance, low latency, and a clean modular architecture are non-negotiable**.

## Tech Stack (do not change without asking)
- **Language / UI**: Kotlin + Jetpack Compose
- **Architecture**: MVI + Clean Architecture; Coroutines + Flow for concurrency
- **PDF Rendering**: PDFium-Android
- **Annotation / Editing**: Apache PDFBox — highlight, embedded pen, eraser
- **Read Aloud**: Uses Android System default TextToSpeech (TTS) Engine
- **Sync**: Google Drive background sync via Storage Access Framework (SAF)

## Non-Negotiable Constraints
- **Performance first.** Avoid memory leaks and unneeded allocations — especially with **bitmaps** and **Compose recompositions**.
- **No blocking work on the main thread.** Use Coroutines/Flow; keep heavy I/O and rendering off the UI.
- **Respect layer boundaries**: Presentation → Domain → Data. Keep the Domain layer framework-agnostic.
- **Production-ready code only.** Handle edge cases; add efficient error handling and logging so issues are easy to identify and resolve.
- Full architecture and design decisions live in `.github/design.md`. **Consult it before any structural change.**

## Build & Test Workflow
- **No local runtime.** Do **NOT** run `./gradlew` locally.
- All builds run via the **GitHub Actions** pipeline.
- UI testing is done by downloading APKs from **GitHub Releases** (triggered on the `main` branch).

## Required Workflow (every task)
1. **Plan before coding.** Create a todo list; make the final item *"Commit and push to remote for CI build."*
2. **Use first-principles thinking** — break the problem down to its fundamentals and reason up from there.
3. **Before implementing**, propose **3–4 approaches** with trade-offs (tech stack / architecture / algorithm / library). Recommend the **optimal** and the **industry-standard** options.
4. **Never assume** tech-stack or feature details — **ask first**.
5. **Never implement a feature** unless it is **explicitly requested**.

## Tools
- For latest docs or resolving package errors, use MCP #tool:websearch and #tool:context7/query-docs

