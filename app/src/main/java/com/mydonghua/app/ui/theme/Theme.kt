package com.mydonghua.app.ui.theme

import android.os.Build
import android.view.RoundedCorner
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
    primaryContainer = AccentCyan.copy(alpha = 0.18f),
    onPrimaryContainer = AccentCyan,
    secondary = Color(0xFF00B4D8),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF00B4D8).copy(alpha = 0.15f),
    onSecondaryContainer = Color(0xFF00B4D8),
    tertiary = Color(0xFF0077B6),
    onTertiary = Color.White,
    background = DarkBg,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    surfaceTint = DarkSurfaceTint,
    outline = DarkOutline,
    outlineVariant = DarkOutline.copy(alpha = 0.5f),
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    onSurfaceVariant = TextSecondaryDark,
    scrim = Color.Black
)

private val LightColorScheme = lightColorScheme(
    primary = AccentCyanLight,
    onPrimary = Color.White,
    primaryContainer = AccentCyanLight.copy(alpha = 0.12f),
    onPrimaryContainer = AccentCyanLight,
    secondary = Color(0xFF0077B6),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF0077B6).copy(alpha = 0.10f),
    onSecondaryContainer = Color(0xFF0077B6),
    tertiary = Color(0xFF009688),
    onTertiary = Color.White,
    background = LightBg,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    surfaceTint = LightSurfaceTint,
    outline = LightOutline,
    outlineVariant = LightOutline.copy(alpha = 0.5f),
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight,
    onSurfaceVariant = TextSecondaryLight,
    scrim = Color.Black
)

private val AmoledColorScheme = darkColorScheme(
    primary = AccentCyan,
    onPrimary = Color.Black,
    primaryContainer = AccentCyan.copy(alpha = 0.20f),
    onPrimaryContainer = AccentCyan,
    secondary = Color(0xFF00B4D8),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF00B4D8).copy(alpha = 0.15f),
    onSecondaryContainer = Color(0xFF00B4D8),
    tertiary = Color(0xFF0077B6),
    onTertiary = Color.White,
    background = AmoledBg,
    surface = AmoledSurface,
    surfaceVariant = AmoledSurfaceVariant,
    surfaceTint = AmoledSurfaceTint,
    outline = AmoledOutline,
    outlineVariant = AmoledOutline.copy(alpha = 0.5f),
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    onSurfaceVariant = TextSecondaryDark,
    scrim = Color.Black
)

fun getAccentColor(accentKey: String, isLight: Boolean): Color {
    return when (accentKey.lowercase()) {
        "dynamic" -> if (isLight) Color(0xFF6750A4) else Color(0xFFD0BCFF)
        "purple" -> if (isLight) Color(0xFF7B2CBF) else Color(0xFFBB86FC)
        "rose" -> if (isLight) Color(0xFFD90429) else Color(0xFFFF2A5F)
        "green" -> if (isLight) Color(0xFF2EA043) else Color(0xFF4CAF50)
        "orange" -> if (isLight) Color(0xFFD97706) else Color(0xFFFF9800)
        "blue" -> if (isLight) Color(0xFF2563EB) else Color(0xFF448AFF)
        "teal" -> if (isLight) Color(0xFF0D9488) else Color(0xFF1DE9B6)
        "pink" -> if (isLight) Color(0xFFBE185D) else Color(0xFFFF69B4)
        else -> if (isLight) Color(0xFF009AA6) else Color(0xFF00F2FE)
    }
}

fun getSecondaryColor(accentKey: String, isLight: Boolean): Color {
    return when (accentKey.lowercase()) {
        "dynamic" -> if (isLight) Color(0xFF625B71) else Color(0xFFCCC2DC)
        "purple" -> if (isLight) Color(0xFF5A189A) else Color(0xFF9D4EDD)
        "rose" -> if (isLight) Color(0xFFA0001C) else Color(0xFFD90429)
        "green" -> if (isLight) Color(0xFF1B4332) else Color(0xFF3FB950)
        "orange" -> if (isLight) Color(0xFF9A031E) else Color(0xFFF0883E)
        "blue" -> if (isLight) Color(0xFF1D4ED8) else Color(0xFF58A6FF)
        "teal" -> if (isLight) Color(0xFF0F766E) else Color(0xFF2DD4BF)
        "pink" -> if (isLight) Color(0xFF9F1239) else Color(0xFFF472B6)
        else -> if (isLight) Color(0xFF0077B6) else Color(0xFF00B4D8)
    }
}

fun getTertiaryColor(accentKey: String, isLight: Boolean): Color {
    return when (accentKey.lowercase()) {
        "dynamic" -> if (isLight) Color(0xFF7D5260) else Color(0xFFEFB8C8)
        "purple" -> if (isLight) Color(0xFF3C096C) else Color(0xFF7B2CBF)
        "rose" -> if (isLight) Color(0xFF6A040F) else Color(0xFFA0001C)
        "green" -> if (isLight) Color(0xFF081C15) else Color(0xFF2EA043)
        "orange" -> if (isLight) Color(0xFF5F0F40) else Color(0xFFD97706)
        "blue" -> if (isLight) Color(0xFF1E3A8A) else Color(0xFF2563EB)
        "teal" -> if (isLight) Color(0xFF115E59) else Color(0xFF0D9488)
        "pink" -> if (isLight) Color(0xFF881337) else Color(0xFFBE185D)
        else -> if (isLight) Color(0xFF03045E) else Color(0xFF0077B6)
    }
}

fun buildM3ColorSchemeWithAccents(
    accentColor: Color,
    secondaryAccent: Color,
    tertiaryAccent: Color,
    themeMode: String
): ColorScheme {
    val isLight = themeMode.lowercase() == "light"
    val isAmoled = themeMode.lowercase() == "amoled"

    val baseBg = when {
        isAmoled -> AmoledBg
        isLight -> Color(0xFFF6F7F9)
        else -> DarkBg
    }

    val baseSurface = when {
        isAmoled -> AmoledSurface
        isLight -> Color(0xFFFFFFFF)
        else -> DarkSurface
    }

    val backgroundTinted = if (isAmoled) AmoledBg else accentColor.copy(alpha = if (isLight) 0.05f else 0.03f).compositeOver(baseBg)
    val surfaceTinted = if (isAmoled) AmoledSurface else accentColor.copy(alpha = if (isLight) 0.08f else 0.04f).compositeOver(baseSurface)
    val surfaceVariantTinted = if (isAmoled) AmoledSurfaceVariant else accentColor.copy(alpha = if (isLight) 0.12f else 0.06f).compositeOver(if (isLight) LightSurfaceVariant else DarkSurfaceVariant)

    return if (isLight) {
        lightColorScheme(
            primary = accentColor,
            onPrimary = Color.White,
            primaryContainer = accentColor.copy(alpha = 0.22f),
            onPrimaryContainer = accentColor,
            secondary = secondaryAccent,
            onSecondary = Color.White,
            secondaryContainer = secondaryAccent.copy(alpha = 0.18f),
            onSecondaryContainer = secondaryAccent,
            tertiary = tertiaryAccent,
            onTertiary = Color.White,
            background = backgroundTinted,
            surface = surfaceTinted,
            surfaceVariant = surfaceVariantTinted,
            surfaceTint = accentColor,
            outline = LightOutline,
            outlineVariant = LightOutline.copy(alpha = 0.5f),
            onBackground = TextPrimaryLight,
            onSurface = TextPrimaryLight,
            onSurfaceVariant = TextSecondaryLight,
            scrim = Color.Black
        )
    } else {
        darkColorScheme(
            primary = accentColor,
            onPrimary = Color.Black,
            primaryContainer = accentColor.copy(alpha = 0.25f),
            onPrimaryContainer = accentColor,
            secondary = secondaryAccent,
            onSecondary = Color.Black,
            secondaryContainer = secondaryAccent.copy(alpha = 0.20f),
            onSecondaryContainer = secondaryAccent,
            tertiary = tertiaryAccent,
            onTertiary = Color.White,
            background = backgroundTinted,
            surface = surfaceTinted,
            surfaceVariant = surfaceVariantTinted,
            surfaceTint = accentColor,
            outline = if (isAmoled) AmoledOutline else DarkOutline,
            outlineVariant = (if (isAmoled) AmoledOutline else DarkOutline).copy(alpha = 0.5f),
            onBackground = TextPrimaryDark,
            onSurface = TextPrimaryDark,
            onSurfaceVariant = TextSecondaryDark,
            scrim = Color.Black
        )
    }
}

fun buildM3ColorScheme(
    accentKey: String,
    themeMode: String
): ColorScheme {
    val isLight = themeMode.lowercase() == "light"
    val accentColor = getAccentColor(accentKey, isLight)
    val secondaryAccent = getSecondaryColor(accentKey, isLight)
    val tertiaryAccent = getTertiaryColor(accentKey, isLight)
    return buildM3ColorSchemeWithAccents(accentColor, secondaryAccent, tertiaryAccent, themeMode)
}

val LocalDisplayCornerRadius = staticCompositionLocalOf<Dp> { 24.dp }

@Composable
fun rememberDisplayCornerRadius(): Dp {
    val density = LocalDensity.current
    val view = LocalView.current
    return remember(density, view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val insets = view.rootWindowInsets
            val topLeft = insets?.getRoundedCorner(RoundedCorner.POSITION_TOP_LEFT)
            val radiusPx = topLeft?.radius ?: 0
            if (radiusPx > 0) {
                with(density) { radiusPx.toDp() }
            } else 24.dp
        } else {
            24.dp
        }
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
    val displayCornerRadius = rememberDisplayCornerRadius()

    val finalScheme = if (accentColor.lowercase() == "dynamic" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val dynamicSys = if (isLight) {
            dynamicLightColorScheme(context)
        } else {
            dynamicDarkColorScheme(context)
        }
        buildM3ColorSchemeWithAccents(
            accentColor = dynamicSys.primary,
            secondaryAccent = dynamicSys.secondary,
            tertiaryAccent = dynamicSys.tertiary,
            themeMode = themeMode
        )
    } else {
        buildM3ColorScheme(accentColor, themeMode)
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        androidx.compose.runtime.SideEffect {
            val window = (view.context as? android.app.Activity)?.window
            if (window != null) {
                val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = isLight
                insetsController.isAppearanceLightNavigationBars = isLight
            }
        }
    }

    MaterialTheme(
        colorScheme = finalScheme,
        typography = AppTypography
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            LocalDisplayCornerRadius provides displayCornerRadius,
            androidx.compose.material3.LocalTextStyle provides TextStyle(fontFamily = JustSansFamily)
        ) {
            content()
        }
    }
}
