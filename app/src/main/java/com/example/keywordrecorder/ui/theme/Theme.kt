package com.example.keywordrecorder.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

// Terminal color scheme — always dark, no dynamic color
private val TermColorScheme = darkColorScheme(
    primary              = TermCyan,
    onPrimary            = TermBg,
    primaryContainer     = TermSelected,
    onPrimaryContainer   = TermTextHeader,
    secondary            = TermPurple,
    onSecondary          = TermBg,
    secondaryContainer   = TermSurfaceAlt,
    onSecondaryContainer = TermTextHeader,
    tertiary             = TermGreen,
    onTertiary           = TermBg,
    tertiaryContainer    = TermSurface,
    onTertiaryContainer  = TermGreen,
    background           = TermBg,
    onBackground         = TermTextNormal,
    surface              = TermSurface,
    onSurface            = TermTextNormal,
    surfaceVariant       = TermSurfaceAlt,
    onSurfaceVariant     = TermTextHeader,
    outline              = TermBorder,
    outlineVariant       = TermTextDim,
    error                = TermRed,
    onError              = TermBg,
    errorContainer       = TermSurface,
    onErrorContainer     = TermPink,
    inverseSurface       = TermTextNormal,
    inverseOnSurface     = TermBg,
    inversePrimary       = TermBg,
    surfaceTint          = TermCyan,
    scrim                = TermBg
)

// Tight rectangular shapes for the terminal look
private val TermShapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small      = RoundedCornerShape(2.dp),
    medium     = RoundedCornerShape(2.dp),
    large      = RoundedCornerShape(2.dp),
    extraLarge = RoundedCornerShape(2.dp)
)

@Composable
fun KeywordRecorderTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = TermBg.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }
    MaterialTheme(
        colorScheme = TermColorScheme,
        typography  = Typography,
        shapes      = TermShapes,
        content     = content
    )
}
