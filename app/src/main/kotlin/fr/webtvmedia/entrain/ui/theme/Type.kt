package fr.webtvmedia.entrain.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import fr.webtvmedia.entrain.R

@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private fun inter(weight: Int) = androidx.compose.ui.text.font.Font(
    resId = R.font.inter_variable,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

val InterFontFamily = FontFamily(
    inter(400), inter(500), inter(600), inter(700), inter(800),
)

val EnTrainTypography = Typography(
    displayLarge = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight(800), fontSize = 40.sp, letterSpacing = (-0.5).sp),
    displayMedium = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight(800), fontSize = 32.sp, letterSpacing = (-0.4).sp),
    displaySmall = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight(700), fontSize = 28.sp, letterSpacing = (-0.3).sp),
    headlineLarge = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight(700), fontSize = 26.sp, letterSpacing = (-0.2).sp),
    headlineMedium = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight(700), fontSize = 22.sp, letterSpacing = (-0.2).sp),
    headlineSmall = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight(700), fontSize = 19.sp),
    titleLarge = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight(700), fontSize = 17.sp),
    titleMedium = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight(600), fontSize = 15.sp),
    titleSmall = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight(600), fontSize = 13.sp),
    bodyLarge = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight(400), fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight(400), fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight(400), fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight(600), fontSize = 14.sp),
    labelMedium = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight(600), fontSize = 12.sp),
    labelSmall = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight(600), fontSize = 10.sp, letterSpacing = 0.4.sp),
)
