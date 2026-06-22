---
applyTo: "**/*"
name: PDF Reader Android App
description: Instructions for developing the highly optimized Android PDF Reader
---


## Project Description:

This project is a Compute Intensive Application. A lightweight, highly optimized Android PDF reader focused purely on reading and annotating. 
Before production deployment, we need to optimize a lot. We must focus on smoother UI / UX and low system resources management.

Core capabilities:
- PDF Rendering (PDFium-Android)
- Highlight, Pen annotation (embedded), and Eraser tools (PDFBox)
- Read Aloud (Android Native TTS)
- Google Drive Background Sync (Storage Access Framework)

**Development & Build Workflow:**
- **There is NO local runtime environment.** Do not attempt to run `./gradlew` locally.
- Everything is built via GitHub Actions pipeline.
- UI Testing is performed strictly by downloading the generated APKs from **GitHub Releases** (triggered on the `main` branch).

Performance, low latency, and a clean modular architecture are non-negotiable.

Full architecture and design decisions live in `.github/design.md`. **Always consult it before making structural changes.**


---


- Low resource usage and low latency execution are crucial for providing a seamless user experience. 
- Efficient error handling and logging should be implemented to ensure that any issues can be quickly identified and resolved. 
- The architecture should be clean and modular to facilitate future development and maintenance.


---

<workflow>

## This is how you should always act:

### Planning and Approach
- **Always plan before writing code.** Create a todo list; add "Commit and Push to remote for CI build" as the final step.
- Follow **First principles thinking** — breaking down a problem to its most fundamental truths and reasoning up from there 
- Focus on **smoother UI / UX and low system resources management** — avoid memory leaks and unneeded allocations, especially with bitmaps or Jetpack Compose recompositions.
- **Never assume** on anything tech-stack or feature-related — ask first.
- **Never implement a feature** until it's explicitly requested.



### Refining Ideas

- Suggest **3-4 approaches** with trade-offs (tech stack, architecture, algorithm, library) before implementing. Prioritize the Optimal and the Industry Standard ones



</workflow>



### Code Implementation:

- Always think about edge cases and how to handle them
- Write code that are production ready


### Tools instruction
- For referring latest document and resolving package error use MCP [`websearch`, `context7`]



