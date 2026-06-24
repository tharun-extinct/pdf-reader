# Technical Audit

Detailed report of the environment, tools, and dependencies used in this project based on the configuration files (`app/build.gradle.kts`, `build.gradle.kts`)

## 1. Development Kit & Languages

| Technology / SDK | Version | Details |
| :--- | :--- | :--- |
| **Language** | Kotlin `1.9.22` | Primary language for the project. |
| **Java Compatibility** | Java `17` | Both `sourceCompatibility` and `targetCompatibility` are set to Java 17. |
| **UI Toolkit** | Jetpack Compose | Core UI framework with Compose Compiler version `1.5.10`. |

### 2. Runtime Environment (Android SDK)

The project is built specifically for the Android operating system with the following SDK configurations:

| SDK Property | Version / API Level | OS Equivalent |
| :--- | :--- | :--- |
| **Compile SDK** | `34` | Android 14 |
| **Target SDK** | `34` | Android 14 |
| **Minimum SDK** | `26` | Android 8.0 (Oreo) |

### 3. Build Tools

| Tool | Version | Description |
| :--- | :--- | :--- |
| **Build System** | Gradle (Kotlin DSL) | Uses `build.gradle.kts` for build scripts. |
| **Android Gradle Plugin** | `8.3.0` | Handles the building and packaging of the Android application. |

### 4. Application Architecture
Based on the project's instructions:
- **Architecture Pattern**: MVI (Model-View-Intent) + Clean Architecture.
- **Layering**: Strict separation between Presentation, Domain, and Data layers. Domain layer is kept framework-agnostic.
- **Concurrency**: Relies heavily on Kotlin Coroutines and Flows to ensure heavy I/O and rendering remain off the main UI thread.

### 5. Project Dependencies

The project leverages several specialized libraries for PDF manipulation, concurrency, and UI rendering:

#### Core PDF & Feature Libraries
| Library / Feature | Dependency / Version | Purpose |
| :--- | :--- | :--- |
| **PDF Rendering** | `pdfium-android:1.9.0` | Tiled, low-latency PDF rendering (performance focused). |
| **PDF Annotation** | `pdfbox-android:2.0.27.0` | Apache PDFBox port for Android. Used for highlights, embedded pen, and eraser features. |
| **Read Aloud** | *Native* | Uses the Android System default TextToSpeech (TTS) Engine. |
| **Syncing** | *Native* | Google Drive background sync using the native Android Storage Access Framework (SAF). |

#### AndroidX & Jetpack Compose
| Dependency | Version | Purpose |
| :--- | :--- | :--- |
| **Compose BOM** | `2024.02.00` | Bill of Materials (BOM) to align all Compose library versions. |
| **Material 3** | (via BOM) | Modern Material Design 3 UI components and styling. |
| **Material Icons** | (via BOM) | Extended material icons. |
| **Compose Navigation**| `2.7.7` | Multi-screen routing and navigation logic within Compose. |
| **Activity Compose** | `1.8.2` | Integration of Compose into Android Activities. |
| **Core KTX** | `1.12.0` | Kotlin extensions for Android Core APIs. |

#### Lifecycle & Concurrency
| Dependency | Version | Purpose |
| :--- | :--- | :--- |
| **Kotlin Coroutines** | `1.7.3` | Asynchronous programming, threading, and Flow management. |
| **Lifecycle Runtime** | `2.7.0` | Integration for lifecycle-aware coroutine scopes. |
| **ViewModel Compose** | `2.7.0` | Connects ViewModels to Compose screens. |
