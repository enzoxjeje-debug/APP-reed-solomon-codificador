package com.example.reedsolomonlab.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush

private val Scheme = darkColorScheme(
    primary = RsColors.Cyan,
    onPrimary = RsColors.Background,
    secondary = RsColors.Purple,
    onSecondary = RsColors.TextPrimary,
    tertiary = RsColors.Magenta,
    background = RsColors.Background,
    onBackground = RsColors.TextPrimary,
    surface = RsColors.Surface,
    onSurface = RsColors.TextPrimary,
    surfaceVariant = RsColors.SurfaceGlow,
    onSurfaceVariant = RsColors.TextSecondary,
    error = RsColors.Error,
    onError = RsColors.Background,
    outline = RsColors.CyanDim
)

@Composable
fun ReedSolomonLabTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = Scheme,
        typography = RsTypography,
        content = content
    )
}

/** Degradado principal cian → violeta → magenta. */
val NeonGradient: Brush
    get() = Brush.horizontalGradient(
        listOf(RsColors.Cyan, RsColors.Purple, RsColors.Magenta)
    )

/** Degradado vertical de fondo. */
val BackgroundGradient: Brush
    get() = Brush.verticalGradient(
        listOf(RsColors.Background, RsColors.BackgroundSoft, RsColors.Background)
    )
