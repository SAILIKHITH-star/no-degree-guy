package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = RoyalIndigo,
    onPrimary = Color.White,
    primaryContainer = RoyalIndigoContainer,
    onPrimaryContainer = OnRoyalIndigoContainer,
    secondary = SaffronGold,
    onSecondary = Color(0xFF1E1300),
    secondaryContainer = SaffronContainer,
    onSecondaryContainer = OnSaffronContainer,
    tertiary = LuminousTeal,
    onTertiary = Color.White,
    tertiaryContainer = TealContainer,
    onTertiaryContainer = OnTealContainer,
    background = WarmAlabaster,
    onBackground = InkPrimary,
    surface = CrispSurface,
    onSurface = InkPrimary,
    surfaceVariant = SoftPaper,
    onSurfaceVariant = InkSecondary,
    outline = SubtleBorder,
    outlineVariant = Color(0xFFEBE7DF),
    error = ErrorCrimson,
    onError = Color.White,
    errorContainer = ErrorContainerLight,
    onErrorContainer = Color(0xFF410002)
)

private val DarkColorScheme = darkColorScheme(
    primary = IndigoTintDark,
    onPrimary = Color(0xFF0A1B47),
    primaryContainer = RoyalIndigoLight,
    onPrimaryContainer = RoyalIndigoContainer,
    secondary = SaffronTintDark,
    onSecondary = Color(0xFF3A2500),
    secondaryContainer = Color(0xFF543700),
    onSecondaryContainer = SaffronContainer,
    tertiary = TealTintDark,
    onTertiary = Color(0xFF003730),
    tertiaryContainer = Color(0xFF005046),
    onTertiaryContainer = TealContainer,
    background = MidnightBg,
    onBackground = DarkTextPrimary,
    surface = MidnightSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = MidnightSurfaceHigh,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkOutline,
    outlineVariant = Color(0xFF24304B),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun RupaIitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
