package com.kotlinapps.pomodoro.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

/** iOS has no wallpaper-based color system; the setting is hidden and the brand palette is used. */
actual val supportsDynamicColor: Boolean = false

@Composable
internal actual fun dynamicColorScheme(darkTheme: Boolean): ColorScheme? = null
