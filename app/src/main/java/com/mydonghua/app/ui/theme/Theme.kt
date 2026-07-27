package com.mydonghua.app.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.mydonghua.app.R

val JustSansFamily = FontFamily(
    Font(R.font.just_sans_regular, FontWeight.Normal),
    Font(R.font.just_sans_regular, FontWeight.Medium),
    Font(R.font.just_sans_regular, FontWeight.SemiBold),
    Font(R.font.just_sans_exbold, FontWeight.Bold),
    Font(R.font.just_sans_exbold, FontWeight.ExtraBold)
)

val AppTypography = Typography(
    displayLarge = TextStyle(fontFamily = JustSansFamily, fontWeight = FontWeight.SemiBold, fontSize = 57.sp, lineHeight = 64.sp),
    displayMedium = TextStyle(fontFamily = JustSansFamily, fontWeight = FontWeight.SemiBold, fontSize = 45.sp, lineHeight = 52.sp),
    displaySmall = TextStyle(fontFamily = JustSansFamily, fontWeight = FontWeight.SemiBold, fontSize = 36.sp, lineHeight = 44.sp),
    headlineLarge = TextStyle(fontFamily = JustSansFamily, fontWeight = FontWeight.SemiBold, fontSize = 32.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontFamily = JustSansFamily, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 36.sp),
    headlineSmall = TextStyle(fontFamily = JustSansFamily, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontFamily = JustSansFamily, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = JustSansFamily, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp),
    titleSmall = TextStyle(fontFamily = JustSansFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = JustSansFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = JustSansFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = JustSansFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = JustSansFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = JustSansFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = JustSansFamily, fontWeight = FontWeight.Normal, fontSize = 11.sp, lineHeight = 16.sp)
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
        typography = AppTypography
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalTextStyle provides TextStyle(fontFamily = JustSansFamily)
        ) {
            content()
        }
    }
}
