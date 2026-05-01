package com.example.keywordrecorder.ui.theme

import androidx.compose.ui.graphics.Color

// Echo.Notes — deep navy premium dark palette
val EchoBg          = Color(0xFF0D1117)   // deep navy background
val EchoSurface     = Color(0xFF161B22)   // card / panel surface
val EchoSurfaceHigh = Color(0xFF21262D)   // elevated surface
val EchoBorder      = Color(0xFF30363D)   // subtle border
val EchoAccent      = Color(0xFF6C8FFF)   // indigo-blue primary accent
val EchoAccentDeep  = Color(0xFF4361D8)   // deeper accent for pressed states
val EchoAccentDim   = Color(0xFF1E2A52)   // muted accent for chip backgrounds
val EchoAccentGlow  = Color(0x336C8FFF)   // accent glow / translucent

val EchoTextPrimary   = Color(0xFFE6EDF3) // primary text — bright white-blue
val EchoTextSecondary = Color(0xFF8B949E) // secondary / muted text
val EchoTextTertiary  = Color(0xFF484F58) // very dim / hint text

val EchoGreen  = Color(0xFF3FB950)        // success / completed
val EchoRed    = Color(0xFFF85149)        // error / delete
val EchoAmber  = Color(0xFFD29922)        // warning / pending

// Waveform and playback
val EchoWaveActive  = Color(0xFF6C8FFF)   // active / played portion
val EchoWaveInactive = Color(0xFF2D3A4A)  // unplayed portion
val EchoScrubber    = Color(0xFF6C8FFF)   // playhead dot

// Legacy terminal colors kept for TermComponents interop
val TermBg          = EchoBg
val TermSurface     = EchoSurface
val TermSurfaceAlt  = EchoSurfaceHigh
val TermBorder      = EchoBorder
val TermSelected    = EchoAccentDim
val TermTextNormal  = EchoTextPrimary
val TermTextHeader  = EchoTextPrimary
val TermTextDim     = EchoTextTertiary
val TermGreen       = EchoGreen
val TermRed         = EchoRed
val TermPink        = Color(0xFFFF6B6B)
val TermPurpleBar   = EchoAccentDeep
val TermYellow      = EchoAmber
val TermCyan        = EchoAccent
val TermPurple      = Color(0xFF9B8EC4)
