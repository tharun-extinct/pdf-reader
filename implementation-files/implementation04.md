# Launch Crash Debugging & Fix — Implementation Document

**Date:** 2026-06-27  
**Branch:** `gptoss`  
**Context:** App crashed immediately on launch — never rendered a single frame. Two rounds of root-cause analysis and fixes were required.

---

## Round 1: Incompatible Window Theme

### Root Cause
`AndroidManifest.xml` declared `android:theme="@android:style/Theme.Material.Light.NoActionBar"` — a **framework theme** (not an AppCompat/Material3 theme). `ComponentActivity` + Jetpack Compose's `setContent {}` requires a valid Material or AppCompat XML window theme descendant. Using a bare `@android:style/Theme.*` causes an immediate crash in `onCreate`.

### Fix Applied

| File | Change |
|------|--------|
| `app/src/main/res/values/themes.xml` | **Created.** Defines `<style name="Theme.Libro" parent="Theme.Material3.Light.NoActionBar" />` |
| `app/src/main/AndroidManifest.xml` | Changed `android:theme` from `@android:style/Theme.Material.Light.NoActionBar` to `@style/Theme.Libro` |
| `app/build.gradle.kts` | **Added** `implementation("com.google.android.material:material:1.11.0")` — the `Theme.Material3.*` XML parent lives in this library, not in Compose. Without it, the theme parent fails to resolve at build/runtime. |
| `app/src/main/java/com/pdfreader/app/data/pdfium/PdfiumEngine.kt` | Changed `private val pdfiumCore = PdfiumCore(context)` to `private val pdfiumCore by lazy { ... }` — defers native `.so` loading from app startup to first PDF open, preventing a secondary crash path if ABI splits or native extraction fail. |

### Result
Theme resolution fixed. App still crashed — another issue remained.

---

## Round 2: Downloadable Fonts via Google Play Services

### Root Cause
The `res/font/*.xml` files were **Downloadable Font** definitions pointing to `com.google.android.gms.fonts` (Google Play Services Font Provider). Combined with `<meta-data android:name="preloaded_fonts" .../>` in `AndroidManifest.xml`, Android attempted to **eagerly fetch all fonts at app startup** via Play Services. On any device without (or with broken) Play Services — including most CI emulators, AOSP builds, Huawei devices, GrapheneOS — this throws a `RuntimeException` before the first Compose frame is drawn.

### Fix Applied

| File | Change |
|------|--------|
| `app/src/main/AndroidManifest.xml` | **Removed** `<meta-data android:name="preloaded_fonts" android:resource="@array/preloaded_fonts" />` |
| `app/src/main/java/com/pdfreader/app/presentation/theme/Type.kt` | **Rewrote font families.** `InterFontFamily` now uses `FontFamily.SansSerif`; `SourceSerif4FontFamily` now uses `FontFamily.Serif`. Both are system fonts that ship with every Android device — zero external dependencies. Removed all `Font(R.font.*)` references and the `try/catch` wrappers. |
| `app/src/main/res/font/*.xml` (6 files) | **Deleted.** `inter_regular.xml`, `inter_medium.xml`, `inter_semibold.xml`, `sourceserif4_regular.xml`, `sourceserif4_semibold.xml`, `sourceserif4_bold.xml` |
| `app/src/main/res/values/preloaded_fonts.xml` | **Deleted.** No longer referenced. |
| `app/src/main/res/values/font_certs.xml` | **Deleted.** Contained Google Play Services font provider certificates. |

### Result
- **Compile errors:** 0
- **Runtime font dependencies:** None (system fonts only)
- **Visual fidelity:** Serif headlines + sans-serif UI still match the Stitch "Libro" design, rendered via the device's default Noto Serif / Roboto stack.

---

## Repository Memory Created

`/memories/repo/android_pitfalls.md` — Documents these two crash patterns plus the native-library-lazy-init rule for future reference.

---

## Future: Re-introducing Branded Fonts

When the exact Inter + Source Serif 4 typefaces are desired:

1. Download the `.ttf` files from Google Fonts.
2. Place them in `app/src/main/res/font/` (e.g., `inter_regular.ttf`, `sourceserif4_regular.ttf`).
3. In `Type.kt`, replace the system-font constants with:
   ```kotlin
   val InterFontFamily = FontFamily(
       Font(R.font.inter_regular, FontWeight.Normal),
       Font(R.font.inter_medium, FontWeight.Medium),
       Font(R.font.inter_semibold, FontWeight.SemiBold),
   )
   val SourceSerif4FontFamily = FontFamily(
       Font(R.font.sourceserif4_regular, FontWeight.Normal),
       Font(R.font.sourceserif4_semibold, FontWeight.SemiBold),
       Font(R.font.sourceserif4_bold, FontWeight.Bold),
   )
   ```
4. **Never** use Downloadable Fonts (`fontProviderAuthority="com.google.android.gms.fonts"`) for a critical launch path. If Downloadable Fonts are absolutely required, always provide a `.ttf` fallback via `FontListFontFamily` and never use `preloaded_fonts` meta-data.

---

## Commits

| Commit | Description |
|--------|-------------|
| `5f0508e` | Round 1: theme fix + Material dependency + lazy PdfiumCore |
| `35966b8` | Round 2: remove downloadable fonts, use system font fallbacks |