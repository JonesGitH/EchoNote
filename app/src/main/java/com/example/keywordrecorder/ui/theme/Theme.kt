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

private val EchoColorScheme = darkColorScheme(
    primary              = EchoAccent,
    onPrimary            = EchoBg,
    primaryContainer     = EchoAccentDim,
    onPrimaryContainer   = EchoTextPrimary,
    secondary            = TermPurple,
    onSecondary          = EchoBg,
    secondaryContainer   = EchoSurfaceHigh,
    onSecondaryContainer = EchoTextSecondary,
    tertiary             = EchoGreen,
    onTertiary           = EchoBg,
    background           = EchoBg,
    onBackground         = EchoTextPrimary,
    surface              = EchoSurface,
    onSurface            = EchoTextPrimary,
    surfaceVariant       = EchoSurfaceHigh,
    onSurfaceVariant     = EchoTextSecondary,
    outline              = EchoBorder,
    outlineVariant       = EchoTextTertiary,
    error                = EchoRed,
    onError              = EchoBg,
    errorContainer       = EchoSurface,
    onErrorContainer     = EchoRed,
    inverseSurface       = EchoTextPrimary,
    inverseOnSurface     = EchoBg,
    inversePrimary       = EchoAccentDeep,
    surfaceTint          = EchoAccent,
    scrim                = EchoBg
)

private val EchoShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small      = RoundedCornerShape(8.dp),
    medium     = RoundedCornerShape(12.dp),
    large      = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp)
)

@Composable
fun KeywordRecorderTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = EchoBg.toArgb()
            @Suppress("DEPRECATION")
            window.navigationBarColor = EchoSurface.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = false
            controller.isAppearanceLightNavigationBars = false
        }
    }
    MaterialTheme(
        colorScheme = EchoColorScheme,
        typography  = Typography,
        shapes      = EchoShapes,
        content     = content
    )
}
