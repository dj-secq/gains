package com.example.repsgrams.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.repsgrams.data.datastore.ThemeMode

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(24.dp),
    small = RoundedCornerShape(24.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

object AppSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
}

private val LightColorScheme = lightColorScheme(
    primary = Ink,
    onPrimary = PaperLight,
    primaryContainer = Ink,
    onPrimaryContainer = PaperLight,
    secondary = Ink,
    onSecondary = PaperLight,
    secondaryContainer = CanvasLight,
    onSecondaryContainer = Ink,
    background = CanvasLight,
    onBackground = Ink,
    surface = PaperLight,
    onSurface = Ink,
    surfaceVariant = PaperLight,
    onSurfaceVariant = LabelLight,
    surfaceContainer = PaperLight,
    surfaceContainerHigh = PaperLight,
    surfaceContainerHighest = PaperLight,
    surfaceContainerLow = CanvasLight,
    outline = HairlineLight,
    outlineVariant = HairlineLight,
    error = SignalRed,
    onError = PaperLight,
    inverseSurface = Ink,
    inverseOnSurface = PaperLight,
)

private val DarkColorScheme = darkColorScheme(
    primary = PaperLight,
    onPrimary = Ink,
    primaryContainer = PaperLight,
    onPrimaryContainer = Ink,
    secondary = PaperLight,
    onSecondary = Ink,
    secondaryContainer = InkTile,
    onSecondaryContainer = PaperLight,
    background = CanvasDark,
    onBackground = PaperLight,
    surface = PaperDark,
    onSurface = PaperLight,
    surfaceVariant = InkTile,
    onSurfaceVariant = LabelDark,
    surfaceContainer = PaperDark,
    surfaceContainerHigh = PaperDark,
    surfaceContainerHighest = InkTile,
    surfaceContainerLow = CanvasDark,
    outline = HairlineDark,
    outlineVariant = HairlineDark,
    error = SignalRed,
    onError = PaperLight,
    inverseSurface = PaperLight,
    inverseOnSurface = Ink,
)

val LocalDarkTheme = staticCompositionLocalOf { false }

@Composable
fun RepsGramsTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        val window = view.context.findActivity()?.window
        if (window != null) {
            SideEffect {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(LocalDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            shapes = AppShapes,
            typography = Typography,
            content = content,
        )
    }
}

private fun Context.findActivity(): Activity? {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
