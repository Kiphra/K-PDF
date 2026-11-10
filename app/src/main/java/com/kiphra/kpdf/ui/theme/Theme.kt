package com.kiphra.kpdf.ui.theme

import android.app.Activity
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = KiphraDarkPrimary,
    onPrimary = KiphraDarkOnPrimary,
    primaryContainer = KiphraDarkPrimaryContainer,
    onPrimaryContainer = KiphraDarkOnPrimaryContainer,
    secondary = KiphraDarkSecondary,
    onSecondary = KiphraDarkOnSecondary,
    secondaryContainer = KiphraDarkSecondaryContainer,
    onSecondaryContainer = KiphraDarkOnSecondaryContainer,
    tertiary = KiphraDarkTertiary,
    onTertiary = KiphraDarkOnTertiary,
    tertiaryContainer = KiphraDarkTertiaryContainer,
    onTertiaryContainer = KiphraDarkOnTertiaryContainer,
    background = KiphraDarkBackground,
    onBackground = KiphraDarkOnBackground,
    surface = KiphraDarkSurface,
    onSurface = KiphraDarkOnSurface,
    surfaceVariant = KiphraDarkSurfaceVariant,
    onSurfaceVariant = KiphraDarkOnSurfaceVariant,
    outline = KiphraDarkOutline,
    outlineVariant = KiphraDarkOutlineVariant
)

private val LightColorScheme = lightColorScheme(
    primary = KiphraLightPrimary,
    onPrimary = KiphraLightOnPrimary,
    primaryContainer = KiphraLightPrimaryContainer,
    onPrimaryContainer = KiphraLightOnPrimaryContainer,
    secondary = KiphraLightSecondary,
    onSecondary = KiphraLightOnSecondary,
    secondaryContainer = KiphraLightSecondaryContainer,
    onSecondaryContainer = KiphraLightOnSecondaryContainer,
    tertiary = KiphraLightTertiary,
    onTertiary = KiphraLightOnTertiary,
    tertiaryContainer = KiphraLightTertiaryContainer,
    onTertiaryContainer = KiphraLightOnTertiaryContainer,
    background = KiphraLightBackground,
    onBackground = KiphraLightOnBackground,
    surface = KiphraLightSurface,
    onSurface = KiphraLightOnSurface,
    surfaceVariant = KiphraLightSurfaceVariant,
    onSurfaceVariant = KiphraLightOnSurfaceVariant,
    outline = KiphraLightOutline
)

@Composable
fun KPdfTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // Determine the actual dark theme state based on AppCompatDelegate mode
    val actualDarkTheme = when (AppCompatDelegate.getDefaultNightMode()) {
        AppCompatDelegate.MODE_NIGHT_YES -> true
        AppCompatDelegate.MODE_NIGHT_NO -> false
        else -> darkTheme // MODE_NIGHT_FOLLOW_SYSTEM or unspecified
    }
    
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (actualDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        actualDarkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val statusBarColor = if (actualDarkTheme) JutDarkAppBarBg else JutLightAppBarBg
            val navBarColor = if (actualDarkTheme) JutDarkAppBarBg else JutLightAppBarBg
            window.statusBarColor = statusBarColor.toArgb()
            window.navigationBarColor = navBarColor.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !actualDarkTheme
            controller.isAppearanceLightNavigationBars = !actualDarkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Backward compatibility alias for PDFToolkitTheme
@Composable
fun PDFToolkitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) = KPdfTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)