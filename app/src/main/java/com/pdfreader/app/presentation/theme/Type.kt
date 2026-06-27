package com.pdfreader.app.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.pdfreader.app.R

// ── Font Families ─────────────────────────────────────────────────────

val InterFontFamily = try {
    FontFamily(
        Font(R.font.inter_regular, FontWeight.Normal),
        Font(R.font.inter_medium, FontWeight.Medium),
        Font(R.font.inter_semibold, FontWeight.SemiBold),
    )
} catch (_: Throwable) {
    FontFamily.SansSerif
}

val SourceSerif4FontFamily = try {
    FontFamily(
        Font(R.font.sourceserif4_regular, FontWeight.Normal),
        Font(R.font.sourceserif4_semibold, FontWeight.SemiBold),
        Font(R.font.sourceserif4_bold, FontWeight.Bold),
    )
} catch (_: Throwable) {
    FontFamily.Serif
}


// ── Stitch Typography Tokens ──────────────────────────────────────────

/** "display-title": Source Serif 4, 40sp, weight 700 */
val DisplayTitleStyle = TextStyle(
    fontFamily = SourceSerif4FontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 40.sp,
    lineHeight = 48.sp,        // 1.2 × 40
    letterSpacing = (-0.8).sp  // -0.02em
)

/** "headline-lg": Source Serif 4, 32sp, weight 600 */
val HeadlineLgStyle = TextStyle(
    fontFamily = SourceSerif4FontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 32.sp,
    lineHeight = 41.6.sp       // 1.3 × 32
)

/** "headline-lg-mobile": Source Serif 4, 24sp, weight 600 */
val HeadlineLgMobileStyle = TextStyle(
    fontFamily = SourceSerif4FontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 24.sp,
    lineHeight = 31.2.sp       // 1.3 × 24
)

/** "body-reading": Source Serif 4, 18sp, weight 400, generous line height */
val BodyReadingStyle = TextStyle(
    fontFamily = SourceSerif4FontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 18.sp,
    lineHeight = 30.6.sp       // 1.7 × 18
)

/** "ui-main": Inter, 16sp, weight 500 */
val UiMainStyle = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 16.sp,
    lineHeight = 24.sp
)

/** "ui-sm": Inter, 14sp, weight 400 */
val UiSmStyle = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 14.sp,
    lineHeight = 20.sp
)

/** "label-caps": Inter, 12sp, weight 600, wide tracking */
val LabelCapsStyle = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 12.sp,
    lineHeight = 16.sp,
    letterSpacing = 0.6.sp     // 0.05em
)

// ── Material 3 Typography Mapping ─────────────────────────────────────

val NoxReaderTypography = Typography(
    displayLarge = DisplayTitleStyle,
    displayMedium = HeadlineLgStyle,
    displaySmall = HeadlineLgMobileStyle,
    headlineLarge = HeadlineLgStyle,
    headlineMedium = HeadlineLgMobileStyle,
    headlineSmall = TextStyle(
        fontFamily = SourceSerif4FontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp
    ),
    titleLarge = UiMainStyle.copy(fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
    titleMedium = UiMainStyle,
    titleSmall = UiSmStyle.copy(fontWeight = FontWeight.Medium),
    bodyLarge = BodyReadingStyle,
    bodyMedium = UiMainStyle.copy(fontWeight = FontWeight.Normal),
    bodySmall = UiSmStyle,
    labelLarge = LabelCapsStyle,
    labelMedium = LabelCapsStyle.copy(fontSize = 11.sp),
    labelSmall = LabelCapsStyle.copy(fontSize = 10.sp)
)
