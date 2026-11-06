package com.openselena.omniget.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
    val Orange = Color(0xFFD97757)
    val OrangeBright = Color(0xFFE5A087)
    val OnOrange = Color(0xFF2A1A12)
    val Blue = Color(0xFF4F7381)
    val Green = Color(0xFF4E7A55)
    val Red = Color(0xFFB5452A)
    val Yellow = Color(0xFF8A662E)
    val Purple = Color(0xFF86708E)
    val Teal = Color(0xFF507E7B)

    val DarkCanvas = Color(0xFF24221F)
    val DarkPane = Color(0xFF1E1C19)
    val DarkSurface = Color(0xFF2D2A25)
    val DarkSurfaceHigh = Color(0xFF211F1B)
    val DarkText = Color(0xFFECE5D8)
    val DarkMuted = Color(0xFFB8AFA0)

    val LightCanvas = Color(0xFFF7F5F0)
    val LightPane = Color(0xFFEEEAE1)
    val LightSurface = Color(0xFFF7F5F0)
    val LightSurfaceHigh = Color(0xFFEDE8DE)
    val LightText = Color(0xFF2B2A27)
    val LightMuted = Color(0xFF6E6A62)
}

private val DarkScheme = darkColorScheme(
    primary = Color(0xFFE7A184),
    onPrimary = OmniColors.OnOrange,
    primaryContainer = Color(0xFF93563F),
    onPrimaryContainer = Color(0xFFFFE9DF),
    secondary = Color(0xFFA3BBC2),
    onSecondary = Color(0xFF20343B),
    tertiary = Color(0xFF9DBDA7),
    background = OmniColors.DarkCanvas,
    onBackground = OmniColors.DarkText,
    surface = OmniColors.DarkSurface,
    onSurface = OmniColors.DarkText,
    surfaceVariant = OmniColors.DarkSurfaceHigh,
    onSurfaceVariant = OmniColors.DarkMuted,
    outline = Color(0xFF8C8171).copy(alpha = 0.32f),
    outlineVariant = Color(0xFF8C8171).copy(alpha = 0.18f),
    surfaceContainerLowest = OmniColors.DarkPane,
    surfaceContainerLow = OmniColors.DarkCanvas,
    surfaceContainer = OmniColors.DarkSurface,
    surfaceContainerHigh = Color(0xFF34302B),
    surfaceContainerHighest = Color(0xFF3B362F),
    error = Color(0xFFF0A287),
    onError = Color(0xFF451A0D),
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFFA9512F),
    onPrimary = Color.White,
    primaryContainer = OmniColors.Orange,
    onPrimaryContainer = OmniColors.OnOrange,
    secondary = OmniColors.Blue,
    onSecondary = Color.White,
    tertiary = OmniColors.Green,
    background = OmniColors.LightCanvas,
    onBackground = OmniColors.LightText,
    surface = OmniColors.LightSurface,
    onSurface = OmniColors.LightText,
    surfaceVariant = OmniColors.LightSurfaceHigh,
    onSurfaceVariant = OmniColors.LightMuted,
    outline = Color(0xFF6E5537).copy(alpha = 0.18f),
    outlineVariant = Color(0xFF6E5537).copy(alpha = 0.10f),
    surfaceContainerLowest = Color(0xFFFCFAF6),
    surfaceContainerLow = OmniColors.LightCanvas,
    surfaceContainer = OmniColors.LightPane,
    surfaceContainerHigh = OmniColors.LightSurfaceHigh,
    surfaceContainerHighest = Color(0xFFE8E3D8),
    error = OmniColors.Red,
    onError = Color.White,
)

private val OmniTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.7).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 33.sp,
        letterSpacing = (-0.4).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Serif,
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
        content = {
            CompositionLocalProvider(LocalContentColor provides if (dark) DarkScheme.onBackground else LightScheme.onBackground) {
                content()
            }
        },
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
