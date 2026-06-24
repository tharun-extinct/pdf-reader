# PDFBox Android can optionally use this JPEG2000 decoder when it is present.
# The app does not bundle it, and text extraction does not require it.
-dontwarn com.gemalto.jp2.**

# Keep PDFium Android classes for JNI (Native methods)
-keep class com.shockwave.pdfium.** { *; }

# Keep PDFBox Android classes to prevent reflection/resource loading issues
-keep class com.tom_roush.pdfbox.** { *; }

# Keep resource files and required metadata
-keepattributes Signature,InnerClasses,EnclosingMethod,RuntimeVisibleAnnotations,AnnotationDefault
-keepclassmembers class com.tom_roush.pdfbox.** { *; }

# Keep Google Fonts Provider / Downloadable Fonts infrastructure
-keep class androidx.core.provider.** { *; }

# Keep Compose runtime — R8 can accidentally strip @Composable metadata
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.material.icons.** { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# Keep Kotlin coroutines internals used by Compose
-dontwarn kotlinx.coroutines.**
-keep class kotlinx.coroutines.** { *; }

# Keep data / model classes that may be reflected or serialized
-keep class com.pdfreader.app.presentation.mvi.** { *; }
-keep class com.pdfreader.app.domain.** { *; }
-keep class com.pdfreader.app.data.** { *; }

# Keep the ViewModel factory from being stripped
-keep class com.pdfreader.app.MainActivity$* { *; }

# Keep R class references for font resources
-keep class com.pdfreader.app.R$font { *; }
-keep class com.pdfreader.app.R$array { *; }
