# PDFBox Android can optionally use this JPEG2000 decoder when it is present.
# The app does not bundle it, and text extraction does not require it.
-dontwarn com.gemalto.jp2.**

# Keep PDFium Android classes for JNI (Native methods)
-keep class com.shockwave.** { *; }

# Keep PDFBox Android classes to prevent reflection/resource loading issues
-keep class com.tom_roush.pdfbox.** { *; }
-keep class org.apache.pdfbox.** { *; }
-keep class org.apache.fontbox.** { *; }
-keep class org.apache.xmpbox.** { *; }

-keep class org.spongycastle.** { *; }
-dontwarn org.spongycastle.**
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**
-dontwarn org.apache.commons.logging.**

# Keep resource files and required metadata
-keepattributes Signature,InnerClasses,EnclosingMethod,RuntimeVisibleAnnotations,AnnotationDefault
-keepclassmembers class com.tom_roush.pdfbox.** { *; }

# Keep all AndroidX libraries (including Compose, Navigation, etc.) to prevent stripping issues
-keep class androidx.** { *; }

# Keep Kotlin stdlib and coroutines
-keep class kotlin.** { *; }
-keep class kotlinx.** { *; }

# Keep data / model classes that may be reflected or serialized
-keep class com.pdfreader.app.** { *; }

# Keep R class references for font resources
-keep class com.pdfreader.app.R$font { *; }
-keep class com.pdfreader.app.R$array { *; }
