**Implemented features & fixes**

| File | Change | Reason |
|------|--------|--------|
| `PdfReaderScreen.kt` | • Wrapped the `Image` and `Canvas` inside a `Box` within `AnimatedVisibility`.<br>• Added missing import `androidx.compose.foundation.gestures.detectTransformGestures`.<br>• Corrected import for `graphicsLayer` to `androidx.compose.ui.draw.graphicsLayer`.<br>• Cleaned duplicate imports and removed stale comments. | Fixed illegal sibling‑composable error that caused >100 compile failures and resolved missing/incorrect imports. |
| `BookshelfScreen.kt` | • Imported `androidx.compose.material3.ExperimentalMaterial3Api`.<br>• Annotated the composable function with `@OptIn(ExperimentalMaterial3Api::class)`. | Suppressed experimental‑API warnings and allowed use of Material 3 components. |
| `SettingsScreen.kt` | • Imported `androidx.compose.material3.ExperimentalMaterial3Api`.<br>• Added `@OptIn(ExperimentalMaterial3Api::class)` to the composable. | Same as above – eliminates experimental‑API warnings. |
| **Git workflow** | • Committed the above changes in two separate commits.<br>• Pushed the `gptoss` branch to the remote. | Ensures the fixes are available for CI verification. |

**Result:** All previously reported compilation errors (duplicate imports, missing symbols, illegal composable structure, and experimental‑API warnings) have been addressed and the changes are now pushed for CI to validate.