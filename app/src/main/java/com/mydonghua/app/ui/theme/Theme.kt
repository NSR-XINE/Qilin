package com.mydonghua.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = AccentCyan,
    background = DarkBg,
    surface = DarkCardBg,
    secondary = DarkSecondaryBg,
    onBackground = TextPrimaryDark,
    onSurface = TextSecondaryDark,
    outline = DarkBorder
)

private val LightColorScheme = lightColorScheme(
    primary = AccentCyanLight,
    background = LightBg,
    surface = LightCardBg,
    secondary = LightSecondaryBg,
    onBackground = TextPrimaryLight,
    onSurface = TextSecondaryLight,
    outline = LightBorder
)

private val AmoledColorScheme = darkColorScheme(
    primary = AccentCyan,
    background = AmoledBg,
    surface = AmoledCardBg,
    secondary = AmoledBg,
    onBackground = TextPrimaryDark,
    onSurface = TextSecondaryDark,
    outline = AmoledBorder
)

@Composable
fun MyDonghuaTheme(
    themeMode: String = "dark",
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeMode.lowercase()) {
        "light" -> LightColorScheme
        "amoled" -> AmoledColorScheme
        else -> DarkColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
