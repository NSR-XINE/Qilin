package com.mydonghua.app.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

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

fun getAccentColor(accentKey: String, isLight: Boolean): Color {
    return when (accentKey.lowercase()) {
        "cyan" -> if (isLight) Color(0xFF009AA6) else Color(0xFF00F2FE)
        "purple" -> if (isLight) Color(0xFF7B2CBF) else Color(0xFF9D4EDD)
        "rose" -> if (isLight) Color(0xFFD90429) else Color(0xFFFF2A5F)
        "green" -> if (isLight) Color(0xFF2EA043) else Color(0xFF3FB950)
        "orange" -> if (isLight) Color(0xFFD97706) else Color(0xFFF0883E)
        "blue" -> if (isLight) Color(0xFF2563EB) else Color(0xFF58A6FF)
        "teal" -> if (isLight) Color(0xFF0D9488) else Color(0xFF2DD4BF)
        "pink" -> if (isLight) Color(0xFFBE185D) else Color(0xFFF472B6)
        else -> if (isLight) Color(0xFF009AA6) else Color(0xFF00F2FE)
    }
}

@Composable
fun MyDonghuaTheme(
    themeMode: String = "dark",
    accentColor: String = "default",
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val isLight = themeMode.lowercase() == "light"

    val baseScheme = when (themeMode.lowercase()) {
        "light" -> LightColorScheme
        "amoled" -> AmoledColorScheme
        else -> DarkColorScheme
    }

    val finalScheme = if (accentColor.lowercase() == "dynamic" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val dynamicScheme = if (isLight) {
            dynamicLightColorScheme(context)
        } else {
            dynamicDarkColorScheme(context)
        }
        if (themeMode.lowercase() == "amoled") {
            dynamicScheme.copy(
                background = AmoledBg,
                surface = AmoledCardBg,
                secondary = AmoledBg
            )
        } else {
            dynamicScheme
        }
    } else {
        val primaryOverride = getAccentColor(accentColor, isLight)
        baseScheme.copy(primary = primaryOverride)
    }

    MaterialTheme(
        colorScheme = finalScheme,
        content = content
    )
}
