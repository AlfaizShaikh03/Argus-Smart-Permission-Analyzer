package com.yourname.smartpermissionanalyzer.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/* ----------  Custom security-focused color schemes ---------- */

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF80CBC4),
    secondary = Color(0xFF03DAC5),
    tertiary = Color(0xFF3700B3),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    surfaceVariant = Color(0xFF2D2D2D),
    error = Color(0xFFCF6679),
    onPrimary = Color(0xFF000000),
    onSecondary = Color(0xFF000000),
    onTertiary = Color(0xFFFFFFFF),
    onBackground = Color(0xFFFFFFFF),
    onSurface = Color(0xFFFFFFFF),
    onSurfaceVariant = Color(0xFFE0E0E0),
    onError = Color(0xFF000000),
    outline = Color(0xFF444444),
    primaryContainer = Color(0xFF004D40),
    onPrimaryContainer = Color(0xFF80CBC4),
    secondaryContainer = Color(0xFF00695C),
    onSecondaryContainer = Color(0xFF80DEEA),
    tertiaryContainer = Color(0xFF1A0E2E),
    onTertiaryContainer = Color(0xFFBB86FC),
    errorContainer = Color(0xFF5D1A1A),
    onErrorContainer = Color(0xFFFFDAD6)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF00796B),
    secondary = Color(0xFF03DAC6),
    tertiary = Color(0xFF3700B3),
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    surfaceVariant = Color(0xFFF5F5F5),
    error = Color(0xFFB00020),
    onPrimary = Color(0xFFFFFFFF),
    onSecondary = Color(0xFF000000),
    onTertiary = Color(0xFFFFFFFF),
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    onSurfaceVariant = Color(0xFF5F5F5F),
    onError = Color(0xFFFFFFFF),
    outline = Color(0xFFDDDDDD),
    primaryContainer = Color(0xFFE0F2F1),
    onPrimaryContainer = Color(0xFF00251A),
    secondaryContainer = Color(0xFFB2DFDB),
    onSecondaryContainer = Color(0xFF002117),
    tertiaryContainer = Color(0xFFEDE7F6),
    onTertiaryContainer = Color(0xFF1A0E2E),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

/* ----------  Public theme composable ---------- */

@Composable
fun SmartPermissionAnalyzerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) {
                dynamicDarkColorScheme(context).copy(
                    secondary = Color(0xFF03DAC5),
                    tertiary = Color(0xFF80CBC4),
                    error = Color(0xFFCF6679),
                    errorContainer = Color(0xFF5D1A1A),
                    onErrorContainer = Color(0xFFFFDAD6)
                )
            } else {
                dynamicLightColorScheme(context).copy(
                    secondary = Color(0xFF03DAC6),
                    tertiary = Color(0xFF00796B),
                    error = Color(0xFFB00020),
                    errorContainer = Color(0xFFFFDAD6),
                    onErrorContainer = Color(0xFF410002)
                )
            }
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

/* ----------  Re-usable responsive helpers ---------- */

object ResponsiveDimensions {
    @Composable
    fun buttonHeight() = with(LocalConfiguration.current) {
        when {
            screenWidthDp < 360 -> 48.dp
            screenWidthDp < 600 -> 56.dp
            else -> 64.dp
        }
    }

    @Composable
    fun buttonPadding() = with(LocalConfiguration.current) {
        when {
            screenWidthDp < 360 -> 8.dp
            screenWidthDp < 600 -> 12.dp
            else -> 16.dp
        }
    }

    @Composable
    fun textSize() = with(LocalConfiguration.current) {
        when {
            screenWidthDp < 360 -> 12.sp
            screenWidthDp < 600 -> 14.sp
            else -> 16.sp
        }
    }
}
