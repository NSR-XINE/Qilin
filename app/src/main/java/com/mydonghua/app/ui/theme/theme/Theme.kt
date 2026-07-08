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
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.Typography
import com.mydonghua.app.R

val UbuntuSansFamily = FontFamily(
    Font(R.font.ubuntu_sans_regular, FontWeight.Normal),
    Font(R.font.ubuntu_sans_bold, FontWeight.Bold),
    Font(R.font.ubuntu_sans_bold, FontWeight.ExtraBold)
)

val AppTypography = Typography(
    headlineLarge = Typography().headlineLarge.copy(fontFamily = UbuntuSansFamily, fontWeight = FontWeight.Bold),
    headlineMedium = Typography().headlineMedium.copy(fontFamily = UbuntuSansFamily, fontWeight = FontWeight.Bold),
    headlineSmall = Typography().headlineSmall.copy(fontFamily = UbuntuSansFamily, fontWeight = FontWeight.Bold),
    titleLarge = Typography().titleLarge.copy(fontFamily = UbuntuSansFamily, fontWeight = FontWeight.Bold),
    titleMedium = Typography().titleMedium.copy(fontFamily = UbuntuSansFamily, fontWeight = FontWeight.SemiBold),
    titleSmall = Typography().titleSmall.copy(fontFamily = UbuntuSansFamily, fontWeight = FontWeight.Medium),
    bodyLarge = Typography().bodyLarge.copy(fontFamily = UbuntuSansFamily),
    bodyMedium = Typography().bodyMedium.copy(fontFamily = UbuntuSansFamily),
    bodySmall = Typography().bodySmall.copy(fontFamily = UbuntuSansFamily),
    labelLarge = Typography().labelLarge.copy(fontFamily = UbuntuSansFamily, fontWeight = FontWeight.Medium),
    labelMedium = Typography().labelMedium.copy(fontFamily = UbuntuSansFamily),
    labelSmall = Typography().labelSmall.copy(fontFamily = UbuntuSansFamily)
)

private val DarkColorScheme = darkColorScheme(
    primary = AccentCyan,
    onPrimary = Color.Black,
    background = DarkBg,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    outline = DarkOutline,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    onSurfaceVariant = TextSecondaryDark
)

private val LightColorScheme = lightColorScheme(
    primary = AccentCyanLight,
    onPrimary = Color.White,
    background = LightBg,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    outline = LightOutline,
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight,
    onSurfaceVariant = TextSecondaryLight
)

private val AmoledColorScheme = darkColorScheme(
    primary = AccentCyan,
    onPrimary = Color.Black,
    background = AmoledBg,
    surface = AmoledSurface,
    surfaceVariant = AmoledSurfaceVariant,
    outline = AmoledOutline,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    onSurfaceVariant = TextSecondaryDark
)

fun getAccentColor(accentKey: String, isLight: Boolean): Color {
    return when (accentKey.lowercase()) {
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
                surface = AmoledSurface,
                surfaceVariant = AmoledSurfaceVariant
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
        typography = AppTypography,
        content = content
    )
}
