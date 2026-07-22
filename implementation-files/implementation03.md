# NoxReader App Restyle Implementation Document

## Overview
This document outlines the implementation details for restyling the Android PDF Reader app to the "Cloud PDF" design system (app name: NoxReader), sourced from Stitch (Project ID: `6349643411786464155`).

## Design System Integration
The app has been restyled using Jetpack Compose Material 3 with custom theming to match the Stitch design.

### 1. Typography
Two Google Fonts are used, bundled via downloadable font resources:
- **Inter**: Used for UI elements, navigation, and secondary text (`ui-main`, `ui-sm`, `label-caps`).
- **Source Serif 4**: Used for literary content, headers, and the app title (`display-title`, `headline-lg`, `body-reading`).

### 2. Color Palette
A custom light-mode color palette was implemented, heavily featuring warm off-white tones for a "book sanctuary" aesthetic:
- **Primary**: Deep Navy (`#03192E`)
- **Background/Surface**: Warm Off-White (`#FAF9F7`)
- **Highlight/Accent**: Warm Orange (`#FFDDB0`)
- **Containers**: Elevated surfaces use subtle grey-whites (`#EFEEEC`, `#F4F3F1`)

### 3. Theme Infrastructure
- `NoxReaderTheme.kt`: Wraps `MaterialTheme` with the custom color scheme and typography.
- `Color.kt`: Contains all exact hex codes from the Stitch design.
- `Type.kt`: Maps Stitch typography tokens to Material 3 typography roles.

## UI Components & Screens

### Library Screen (`BookshelfScreen.kt`)
The main landing screen was completely rewritten:
- **Top App Bar**: Minimalist design with a hamburger menu, center title ("NoxReader"), and settings gear.
- **Continue Reading Hero Card**: Displays the current book with a gradient placeholder cover, title, author, and a progress bar.
- **My Collection**: A horizontally scrolling row of book cards with varied gradient covers.
- **Annotations & Notes**: A vertical list of recent user annotations and highlights.
- **Bottom Navigation**: Pill-styled navigation bar with 4 tabs (All Books, Recent, Annotations, Collections).

### Reader Screen (`PdfReaderScreen.kt`)
The PDF rendering and gesture logic were left untouched. Only the UI "chrome" was restyled:
- **Top Bar**: Streamlined top bar showing a back arrow, chapter title breadcrumb, and a bookmark icon.
- **Annotation Toolbar**: Redesigned as a floating, pill-shaped toolbar positioned at the bottom center, matching the Stitch design. Features rounded icons and a contextual palette row that appears when the Pen or Highlighter tools are active.

### Settings Screen (`SettingsScreen.kt`)
Completely rewritten to present a sectioned layout:
- **Reading Preferences**: Font size slider, compact line height toggle, and a serif/sans-serif font toggle.
- **Theme**: Dark mode toggle and a custom accent color hex input with a live color swatch preview.
- **Sync & Storage**: Google Drive sync toggle, storage usage display, and clear cache button.
- **About**: App version and information.

## Next Steps / Pending Items
- Wrap `MainActivity.kt`'s `NavHost` with the new `NoxReaderTheme`.
- Update `AndroidManifest.xml` to rename the app label to "NoxReader".
- Connect the Settings Screen UI components (e.g., Theme Color Picker, Reading Font toggle) to persistent state (DataStore/SharedPreferences) to make them fully functional.
- Generate and load actual book cover images in the Library screen to replace the placeholder gradients.
