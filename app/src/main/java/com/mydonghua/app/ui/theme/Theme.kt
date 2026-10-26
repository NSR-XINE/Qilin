package com.mydonghua.app.ui.theme

import android.os.Build
import android.view.RoundedCorner
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
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
import androidx.compose.ui.graphics.luminance
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
    Font(R.font.just_sans_exbold, FontWeight.SemiBold),
    Font(R.font.just_sans_exbold, FontWeight.Bold),
    Font(R.font.just_sans_exbold, FontWeight.ExtraBold)
)

val AppTypography = Typography(
    displayLarge = TextStyle(fontFamily = JustSansFamily, fontWeight = FontWeight.Bold, fontSize = 52.sp, lineHeight = 60.sp, letterSpacing = (-0.5).sp),
    displayMedium = TextStyle(fontFamily = JustSansFamily, fontWeight = FontWeight.Bold, fontSize = 42.sp, lineHeight = 48.sp, letterSpacing = (-0.4).sp),
    displaySmall = TextStyle(fontFamily = JustSansFamily, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 40.sp, letterSpacing = (-0.3).sp),
    headlineLarge = TextStyle(fontFamily = JustSansFamily, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = (-0.2).sp),
    headlineMedium = TextStyle(fontFamily = JustSansFamily, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, lineHeight = 32.sp, letterSpacing = (-0.2).sp),
    headlineSmall = TextStyle(fontFamily = JustSansFamily, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.1).sp),
    titleLarge = TextStyle(fontFamily = JustSansFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp, letterSpacing = 0.sp),
    titleMedium = TextStyle(fontFamily = JustSansFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp, letterSpacing = 0.1.sp),
    titleSmall = TextStyle(fontFamily = JustSansFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    bodyLarge = TextStyle(fontFamily = JustSansFamily, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp, letterSpacing = 0.15.sp),
    bodyMedium = TextStyle(fontFamily = JustSansFamily, fontWeight = FontWeight.Normal, fontSize = 13.5.sp, lineHeight = 19.sp, letterSpacing = 0.15.sp),
    bodySmall = TextStyle(fontFamily = JustSansFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.2.sp),
    labelLarge = TextStyle(fontFamily = JustSansFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp, lineHeight = 18.sp, letterSpacing = 0.2.sp),
    labelMedium = TextStyle(fontFamily = JustSansFamily, fontWeight = FontWeight.Medium, fontSize = 11.5.sp, lineHeight = 16.sp, letterSpacing = 0.3.sp),
    labelSmall = TextStyle(fontFamily = JustSansFamily, fontWeight = FontWeight.Medium, fontSize = 10.5.sp, lineHeight = 14.sp, letterSpacing = 0.4.sp)
)

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(CornerRadius.xs),    // 6.dp - Micro chips, badges
    small = RoundedCornerShape(CornerRadius.sm),         // 10.dp - Buttons, small cards
    medium = RoundedCornerShape(CornerRadius.md),        // 14.dp - Text fields, menus
    large = RoundedCornerShape(CornerRadius.card),       // 22.dp - Surface cards, glass docks
    extraLarge = RoundedCornerShape(CornerRadius.dialog) // 28.dp - Dialogs, bottom sheets
)

fun getAccentColor(accentKey: String, isLight: Boolean): Color {
    return when (accentKey.lowercase()) {
        "dynamic" -> if (isLight) Color(0xFF009AA6) else Color(0xFF00F2FE)
        "purple" -> if (isLight) Color(0xFF7B2CBF) else Color(0xFFBB86FC)
        "rose" -> if (isLight) Color(0xFFD90429) else Color(0xFFFF2A5F)
        "green" -> if (isLight) Color(0xFF2EA043) else Color(0xFF4CAF50)
        "orange" -> if (isLight) Color(0xFFD97706) else Color(0xFFFF9800)
        "blue" -> if (isLight) Color(0xFF2563EB) else Color(0xFF448AFF)
        "teal" -> if (isLight) Color(0xFF0D9488) else Color(0xFF1DE9B6)
        "pink" -> if (isLight) Color(0xFFBE185D) else Color(0xFFFF69B4)
        "red" -> if (isLight) Color(0xFFE53935) else Color(0xFFFF5252)
        "amber" -> if (isLight) Color(0xFFF59E0B) else Color(0xFFFFCA28)
        "lime" -> if (isLight) Color(0xFF65A30D) else Color(0xFFBEF264)
        "emerald" -> if (isLight) Color(0xFF059669) else Color(0xFF34D399)
        "indigo" -> if (isLight) Color(0xFF4F46E5) else Color(0xFF818CF8)
        "violet" -> if (isLight) Color(0xFF7C3AED) else Color(0xFFA78BFA)
        "gold" -> if (isLight) Color(0xFFEAB308) else Color(0xFFFDE047)
        "sky" -> if (isLight) Color(0xFF0284C7) else Color(0xFF7DD3FC)
        "fuchsia" -> if (isLight) Color(0xFFC026D3) else Color(0xFFF0ABFC)
        "coral" -> if (isLight) Color(0xFFFF6B6B) else Color(0xFFFF8A80)
        "slate" -> if (isLight) Color(0xFF475569) else Color(0xFFCBD5E1)
        "cocoa" -> if (isLight) Color(0xFF795548) else Color(0xFFD7CCC8)
        else -> if (isLight) Color(0xFF009AA6) else Color(0xFF00F2FE)
    }
}

fun getSecondaryColor(accentKey: String, isLight: Boolean): Color {
    return when (accentKey.lowercase()) {
        "dynamic" -> if (isLight) Color(0xFF0077B6) else Color(0xFF00B4D8)
        "purple" -> if (isLight) Color(0xFF5A189A) else Color(0xFF9D4EDD)
        "rose" -> if (isLight) Color(0xFFA0001C) else Color(0xFFD90429)
        "green" -> if (isLight) Color(0xFF1B4332) else Color(0xFF3FB950)
        "orange" -> if (isLight) Color(0xFF9A031E) else Color(0xFFF0883E)
        "blue" -> if (isLight) Color(0xFF1D4ED8) else Color(0xFF58A6FF)
        "teal" -> if (isLight) Color(0xFF0F766E) else Color(0xFF2DD4BF)
        "pink" -> if (isLight) Color(0xFF9F1239) else Color(0xFFF472B6)
        "red" -> if (isLight) Color(0xFFD32F2F) else Color(0xFFE53935)
        "amber" -> if (isLight) Color(0xFFD97706) else Color(0xFFFFB300)
        "lime" -> if (isLight) Color(0xFF4D7C0F) else Color(0xFFA3E635)
        "emerald" -> if (isLight) Color(0xFF047857) else Color(0xFF10B981)
        "indigo" -> if (isLight) Color(0xFF4338CA) else Color(0xFF6366F1)
        "violet" -> if (isLight) Color(0xFF6D28D9) else Color(0xFF8B5CF6)
        "gold" -> if (isLight) Color(0xFFCA8A04) else Color(0xFFFACC15)
        "sky" -> if (isLight) Color(0xFF0369A1) else Color(0xFF38BDF8)
        "fuchsia" -> if (isLight) Color(0xFFA21CAF) else Color(0xFFE879F9)
        "coral" -> if (isLight) Color(0xFFF45454) else Color(0xFFFF6B6B)
        "slate" -> if (isLight) Color(0xFF334155) else Color(0xFF94A3B8)
        "cocoa" -> if (isLight) Color(0xFF6D4C41) else Color(0xFFBCAAA4)
        else -> if (isLight) Color(0xFF0077B6) else Color(0xFF00B4D8)
    }
}

fun getTertiaryColor(accentKey: String, isLight: Boolean): Color {
    return when (accentKey.lowercase()) {
        "dynamic" -> if (isLight) Color(0xFF03045E) else Color(0xFF0077B6)
        "purple" -> if (isLight) Color(0xFF3C096C) else Color(0xFF7B2CBF)
        "rose" -> if (isLight) Color(0xFF6A040F) else Color(0xFFA0001C)
        "green" -> if (isLight) Color(0xFF081C15) else Color(0xFF2EA043)
        "orange" -> if (isLight) Color(0xFF5F0F40) else Color(0xFFD97706)
        "blue" -> if (isLight) Color(0xFF1E3A8A) else Color(0xFF2563EB)
        "teal" -> if (isLight) Color(0xFF115E59) else Color(0xFF0D9488)
        "pink" -> if (isLight) Color(0xFF881337) else Color(0xFFBE185D)
        "red" -> if (isLight) Color(0xFFB71C1C) else Color(0xFFC62828)
        "amber" -> if (isLight) Color(0xFFB45309) else Color(0xFFFF8F00)
        "lime" -> if (isLight) Color(0xFF3F6212) else Color(0xFF84CC16)
        "emerald" -> if (isLight) Color(0xFF065F46) else Color(0xFF059669)
        "indigo" -> if (isLight) Color(0xFF3730A3) else Color(0xFF4F46E5)
        "violet" -> if (isLight) Color(0xFF5B21B6) else Color(0xFF7C3AED)
        "gold" -> if (isLight) Color(0xFFA16207) else Color(0xFFEAB308)
        "sky" -> if (isLight) Color(0xFF0C4A6E) else Color(0xFF0284C7)
        "fuchsia" -> if (isLight) Color(0xFF86198F) else Color(0xFFC026D3)
        "coral" -> if (isLight) Color(0xFFD93B3B) else Color(0xFFF45454)
        "slate" -> if (isLight) Color(0xFF1E293B) else Color(0xFF64748B)
        "cocoa" -> if (isLight) Color(0xFF4E342E) else Color(0xFFA1887F)
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
        isLight -> LightBg
        else -> DarkBg
    }

    val baseSurface = when {
        isAmoled -> AmoledSurface
        isLight -> LightSurface
        else -> DarkSurface
    }

    val baseSurfaceVariant = when {
        isAmoled -> AmoledSurfaceVariant
        isLight -> LightSurfaceVariant
        else -> DarkSurfaceVariant
    }

    val onPrimaryColor = if (accentColor.luminance() > 0.65f) Color(0xFF0F172A) else Color.White
    val onSecondaryColor = if (secondaryAccent.luminance() > 0.65f) Color(0xFF0F172A) else Color.White
    val onTertiaryColor = if (tertiaryAccent.luminance() > 0.65f) Color(0xFF0F172A) else Color.White

    val backgroundTinted = if (isAmoled) AmoledBg else accentColor.copy(alpha = if (isLight) 0.03f else 0.02f).compositeOver(baseBg)
    val surfaceTinted = if (isAmoled) AmoledSurface else accentColor.copy(alpha = if (isLight) 0.04f else 0.03f).compositeOver(baseSurface)
    val surfaceVariantTinted = if (isAmoled) AmoledSurfaceVariant else accentColor.copy(alpha = if (isLight) 0.06f else 0.05f).compositeOver(baseSurfaceVariant)

    return if (isLight) {
        lightColorScheme(
            primary = accentColor,
            onPrimary = onPrimaryColor,
            primaryContainer = accentColor.copy(alpha = 0.12f),
            onPrimaryContainer = accentColor,
            secondary = secondaryAccent,
            onSecondary = onSecondaryColor,
            secondaryContainer = secondaryAccent.copy(alpha = 0.12f),
            onSecondaryContainer = secondaryAccent,
            tertiary = tertiaryAccent,
            onTertiary = onTertiaryColor,
            tertiaryContainer = tertiaryAccent.copy(alpha = 0.12f),
            onTertiaryContainer = tertiaryAccent,
            background = backgroundTinted,
            surface = surfaceTinted,
            surfaceVariant = surfaceVariantTinted,
            surfaceDim = Color(0xFFE2E8F0),
            surfaceBright = Color(0xFFFFFFFF),
            surfaceContainerLowest = Color(0xFFFFFFFF),
            surfaceContainerLow = Color(0xFFF8FAFC),
            surfaceContainer = Color(0xFFF1F5F9),
            surfaceContainerHigh = Color(0xFFE2E8F0),
            surfaceContainerHighest = Color(0xFFCBD5E1),
            surfaceTint = Color.Transparent,
            outline = LightOutline,
            outlineVariant = LightOutlineVariant,
            onBackground = TextPrimaryLight,
            onSurface = TextPrimaryLight,
            onSurfaceVariant = TextSecondaryLight,
            error = Color(0xFFE53935),
            onError = Color.White,
            errorContainer = Color(0xFFFFEBEE),
            onErrorContainer = Color(0xFFB71C1C),
            scrim = Color.Black
        )
    } else {
        darkColorScheme(
            primary = accentColor,
            onPrimary = onPrimaryColor,
            primaryContainer = accentColor.copy(alpha = 0.20f),
            onPrimaryContainer = if (accentColor.luminance() > 0.40f) accentColor else Color.White,
            secondary = secondaryAccent,
            onSecondary = onSecondaryColor,
            secondaryContainer = secondaryAccent.copy(alpha = 0.18f),
            onSecondaryContainer = if (secondaryAccent.luminance() > 0.40f) secondaryAccent else Color.White,
            tertiary = tertiaryAccent,
            onTertiary = onTertiaryColor,
            tertiaryContainer = tertiaryAccent.copy(alpha = 0.18f),
            onTertiaryContainer = Color.White,
            background = backgroundTinted,
            surface = surfaceTinted,
            surfaceVariant = surfaceVariantTinted,
            surfaceDim = if (isAmoled) AmoledBg else DarkBg,
            surfaceBright = if (isAmoled) AmoledSurfaceVariant else Color(0xFF232B42),
            surfaceContainerLowest = if (isAmoled) AmoledBg else Color(0xFF07090E),
            surfaceContainerLow = if (isAmoled) AmoledSurface else Color(0xFF0F131D),
            surfaceContainer = if (isAmoled) AmoledSurfaceVariant else Color(0xFF151A28),
            surfaceContainerHigh = if (isAmoled) AmoledSurfaceTint else Color(0xFF1C2337),
            surfaceContainerHighest = if (isAmoled) Color(0xFF222838) else Color(0xFF232B42),
            surfaceTint = Color.Transparent,
            outline = if (isAmoled) AmoledOutline else DarkOutline,
            outlineVariant = if (isAmoled) AmoledOutlineVariant else DarkOutlineVariant,
            onBackground = TextPrimaryDark,
            onSurface = TextPrimaryDark,
            onSurfaceVariant = TextSecondaryDark,
            error = Color(0xFFFF5252),
            onError = Color.White,
            errorContainer = Color(0xFFFF5252).copy(alpha = 0.20f),
            onErrorContainer = Color(0xFFFF8A80),
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
        typography = AppTypography,
        shapes = AppShapes
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            LocalDisplayCornerRadius provides displayCornerRadius,
            androidx.compose.material3.LocalTextStyle provides TextStyle(fontFamily = JustSansFamily)
        ) {
            content()
        }
    }
}
