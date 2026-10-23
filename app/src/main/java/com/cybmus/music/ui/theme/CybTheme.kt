package com.cybmus.music.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.palette.graphics.Palette

/**
 * Dynamic color scheme extracted from the selected wallpaper.
 * Falls back to a calm cyber-night palette.
 */
data class CybColors(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val onPrimary: Color,
    val onBackground: Color,
    val onSurface: Color,
    val accent: Color,
    val progressGlow: Color
)

val LocalCybColors = staticCompositionLocalOf {
    defaultCybColors()
}

fun defaultCybColors() = CybColors(
    primary = Color(0xFF00E5FF),
    secondary = Color(0xFF7C4DFF),
    tertiary = Color(0xFFFF00AA),
    background = Color(0xFF0A0E17),
    surface = Color(0xFF121826),
    surfaceVariant = Color(0xFF1C2438),
    onPrimary = Color(0xFF001018),
    onBackground = Color(0xFFE8F0FF),
    onSurface = Color(0xFFD0D8F0),
    accent = Color(0xFF00FFC8),
    progressGlow = Color(0xFF00E5FF)
)

fun Palette.toCybColors(): CybColors {
    val dominant = getDominantColor(0xFF0A0E17.toInt())
    val vibrant = getVibrantColor(dominant)
    val muted = getMutedColor(dominant)
    val darkVibrant = getDarkVibrantColor(dominant)
    val lightVibrant = getLightVibrantColor(dominant)

    fun intToColor(c: Int) = Color(c)

    val primary = intToColor(vibrant)
    val secondary = intToColor(darkVibrant)
    val tertiary = intToColor(lightVibrant)
    val bg = intToColor(dominant).copy(alpha = 1f).let {
        // Darken for readable background
        Color(
            red = it.red * 0.25f,
            green = it.green * 0.25f,
            blue = it.blue * 0.3f,
            alpha = 1f
        )
    }
    val surface = Color(
        red = bg.red + 0.05f,
        green = bg.green + 0.05f,
        blue = bg.blue + 0.08f,
        alpha = 1f
    )

    return CybColors(
        primary = primary,
        secondary = secondary,
        tertiary = tertiary,
        background = bg,
        surface = surface,
        surfaceVariant = surface.copy(alpha = 0.9f),
        onPrimary = Color.Black,
        onBackground = Color(0xFFF0F4FF),
        onSurface = Color(0xFFE0E8F8),
        accent = intToColor(muted).let { if (it.luminance() < 0.3f) primary else it },
        progressGlow = primary
    )
}

private fun Color.luminance(): Float {
    return 0.299f * red + 0.587f * green + 0.114f * blue
}

@Composable
fun cybColorScheme(cyb: CybColors): ColorScheme = darkColorScheme(
    primary = cyb.primary,
    secondary = cyb.secondary,
    tertiary = cyb.tertiary,
    background = cyb.background,
    surface = cyb.surface,
    surfaceVariant = cyb.surfaceVariant,
    onPrimary = cyb.onPrimary,
    onBackground = cyb.onBackground,
    onSurface = cyb.onSurface,
    onSecondary = Color.White,
    onTertiary = Color.White
)
