package com.kotlinapps.pomodoro.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFFB3261E),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDAD5),
    onPrimaryContainer = Color(0xFF410001),
    secondary = Color(0xFF2E6A44),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFB1F1C1),
    onSecondaryContainer = Color(0xFF00210E),
    tertiary = Color(0xFF00658E),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFC7E7FF),
    onTertiaryContainer = Color(0xFF001E2E),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB4A9),
    onPrimary = Color(0xFF690003),
    primaryContainer = Color(0xFF930009),
    onPrimaryContainer = Color(0xFFFFDAD5),
    secondary = Color(0xFF96D5A6),
    onSecondary = Color(0xFF00391C),
    secondaryContainer = Color(0xFF12512E),
    onSecondaryContainer = Color(0xFFB1F1C1),
    tertiary = Color(0xFF84CFFF),
    onTertiary = Color(0xFF00344C),
    tertiaryContainer = Color(0xFF004C6C),
    onTertiaryContainer = Color(0xFFC7E7FF),
)

/** True when the platform supports Material You dynamic color (Android 12+ only). */
expect val supportsDynamicColor: Boolean

/** Wallpaper-based colors, or null where unsupported. */
@Composable
internal expect fun dynamicColorScheme(darkTheme: Boolean): ColorScheme?

@Composable
fun PomodoroTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = (if (dynamicColor) dynamicColorScheme(darkTheme) else null)
        ?: if (darkTheme) DarkColors else LightColors

    MaterialTheme(colorScheme = colorScheme, content = content)
}
