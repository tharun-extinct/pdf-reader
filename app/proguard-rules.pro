# PDFBox Android can optionally use this JPEG2000 decoder when it is present.
# The app does not bundle it, and text extraction does not require it.
-dontwarn com.gemalto.jp2.**

# Keep PDFium Android classes for JNI (Native methods)
-keep class com.shockwave.pdfium.** { *; }

# Keep PDFBox Android classes to prevent reflection/resource loading issues
-keep class com.tom_roush.pdfbox.** { *; }
