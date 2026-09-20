package fr.webtvmedia.entrain.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val LightScheme = lightColorScheme(
    primary = Violet40,
    onPrimary = Color.White,
    primaryContainer = Violet90,
    onPrimaryContainer = Violet10,
    secondary = Violet60,
    onSecondary = Color.White,
    secondaryContainer = Violet95,
    onSecondaryContainer = Violet20,
    tertiary = AmberDeep,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE9C7),
    onTertiaryContainer = Color(0xFF5C3A00),
    background = LightBackground,
    onBackground = Slate10,
    surface = LightSurface,
    onSurface = Slate10,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = Color(0xFF5B5566),
    outline = Color(0xFFD9D2E0),
    outlineVariant = Color(0xFFE8E2EF),
    error = LateRed,
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF5C1311),
)

private val DarkScheme = darkColorScheme(
    primary = Violet80,
    onPrimary = Violet10,
    primaryContainer = Violet30,
    onPrimaryContainer = Violet95,
    secondary = Violet60,
    onSecondary = Color.White,
    secondaryContainer = Violet20,
    onSecondaryContainer = Violet90,
    tertiary = AmberAccent,
    onTertiary = Color(0xFF3E2600),
    tertiaryContainer = Color(0xFF5C4300),
    onTertiaryContainer = Color(0xFFFFE0A8),
    background = DarkBackground,
    onBackground = Color(0xFFEAE4F0),
    surface = DarkSurface,
    onSurface = Color(0xFFEAE4F0),
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFCBC3D6),
    outline = Color(0xFF4A4256),
    outlineVariant = Color(0xFF352F40),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF5C1311),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFFFDAD6),
)

/** Couleurs sémantiques temps réel (retard, à l'heure, supprimé). */
data class StatusColors(
    val onTime: Color,
    val late: Color,
    val cancelled: Color,
    val info: Color,
)

val LocalStatusColors = staticCompositionLocalOf {
    StatusColors(SuccessGreen, LateRed, LateRed, InfoBlue)
}

@Composable
fun EnTrainTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography = EnTrainTypography,
        shapes = EnTrainShapes,
        content = content,
    )
}
