package com.openselena.omniget.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.openselena.omniget.data.ThemeMode

object OmniColors {
    val Orange = Color(0xFFFF9F0A)
    val OrangeBright = Color(0xFFFFB340)
    val OnOrange = Color(0xFF1E1200)
    val Blue = Color(0xFF0A84FF)
    val Green = Color(0xFF30D158)
    val Red = Color(0xFFFF453A)
    val Yellow = Color(0xFFFFD60A)
    val Purple = Color(0xFFBF5AF2)
    val Teal = Color(0xFF40C8E0)

    val DarkCanvas = Color(0xFF1C1C1E)
    val DarkPane = Color(0xFF232325)
    val DarkSurface = Color(0xFF2A2A2D)
    val DarkSurfaceHigh = Color(0xFF38383B)
    val DarkText = Color(0xFFF5F5F7)
    val DarkMuted = Color(0xFF98989D)

    val LightCanvas = Color(0xFFF3F1EC)
    val LightPane = Color(0xFFFBFAF7)
    val LightSurface = Color(0xFFFFFFFF)
    val LightSurfaceHigh = Color(0xFFF5F3EE)
    val LightText = Color(0xFF1D1D1F)
    val LightMuted = Color(0xFF6E6A64)
}

private val DarkScheme = darkColorScheme(
    primary = OmniColors.Orange,
    onPrimary = OmniColors.OnOrange,
    primaryContainer = OmniColors.OrangeBright,
    onPrimaryContainer = OmniColors.OnOrange,
    secondary = OmniColors.Blue,
    onSecondary = Color.White,
    tertiary = OmniColors.Teal,
    background = OmniColors.DarkCanvas,
    onBackground = OmniColors.DarkText,
    surface = OmniColors.DarkSurface,
    onSurface = OmniColors.DarkText,
    surfaceVariant = OmniColors.DarkSurfaceHigh,
    onSurfaceVariant = OmniColors.DarkMuted,
    outline = Color.White.copy(alpha = 0.14f),
    error = OmniColors.Red,
    onError = Color.White,
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFFFF9500),
    onPrimary = OmniColors.OnOrange,
    primaryContainer = Color(0xFFFFE1B3),
    onPrimaryContainer = Color(0xFF3A2400),
    secondary = Color(0xFF007AFF),
    onSecondary = Color.White,
    tertiary = Color(0xFF0891B2),
    background = OmniColors.LightCanvas,
    onBackground = OmniColors.LightText,
    surface = OmniColors.LightSurface,
    onSurface = OmniColors.LightText,
    surfaceVariant = OmniColors.LightSurfaceHigh,
    onSurfaceVariant = OmniColors.LightMuted,
    outline = Color.Black.copy(alpha = 0.14f),
    error = Color(0xFFD70015),
    onError = Color.White,
)

private val OmniTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.7).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 33.sp,
        letterSpacing = (-0.4).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.2).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp,
        lineHeight = 24.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 21.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 23.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.2.sp,
    ),
)

private val OmniShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

@Composable
fun OmniGetTheme(
    mode: ThemeMode,
    content: @Composable () -> Unit,
) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context.findActivity() ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(activity.window, view)
            controller.isAppearanceLightStatusBars = !dark
            controller.isAppearanceLightNavigationBars = !dark
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                activity.window.isNavigationBarContrastEnforced = false
            }
        }
    }
    MaterialTheme(
        colorScheme = if (dark) DarkScheme else LightScheme,
        typography = OmniTypography,
        shapes = OmniShapes,
        content = content,
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
