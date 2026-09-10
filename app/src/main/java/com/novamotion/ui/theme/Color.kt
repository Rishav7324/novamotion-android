package com.novamotion.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ─── Apple iOS 18 OLED & Liquid Glass Palette ────────────────────────────────
val IosSystemBackground = Color(0xFF000000)        // Pure OLED Black
val IosSecondaryBackground = Color(0xFF141416)     // Deep Dark System Chrome
val IosTertiaryBackground = Color(0xFF1C1C1E)      // Surface Card Base

// Liquid Glass Translucency Materials
val IosGlassSurface = Color(0xB81C1C1E)            // 72% Translucent Dark Glass
val IosGlassSurfaceLight = Color(0x33FFFFFF)       // 20% Specular Reflection
val IosGlassSurfaceDim = Color(0x66141416)         // 40% Low-opacity Glass Underlay

// Specular Glass Border Gradients (Top highlight -> bottom shadow)
val IosGlassBorder = Brush.verticalGradient(
    colors = listOf(
        Color(0x4DFFFFFF), // 30% White specular highlight at top edge
        Color(0x14FFFFFF), // 8% Ambient rim
        Color(0x08FFFFFF)  // 3% Soft shadow at bottom
    )
)

val IosActiveGlowBorder = Brush.verticalGradient(
    colors = listOf(
        Color(0xCC64D2FF), // Vibrant Neon Cyan Top
        Color(0x4D5E5CE6)  // Indigo Rim
    )
)

// iOS Vibrant System Accents
val IosPurple = Color(0xFFAF52DE)                  // Apple System Purple
val IosIndigo = Color(0xFF5E5CE6)                  // Apple System Indigo
val IosCyan = Color(0xFF64D2FF)                    // Apple System Cyan (Diamond Glow)
val IosMint = Color(0xFF63E6E2)                    // Apple System Mint
val IosBlue = Color(0xFF0A84FF)                    // Apple System Blue
val IosRed = Color(0xFFFF453A)                     // Apple System Red (Playhead)
val IosGreen = Color(0xFF30D158)                   // Apple System Green (Audio)
val IosOrange = Color(0xFFFF9F0A)                  // Apple System Orange
val IosPink = Color(0xFFFF375F)                    // Apple System Pink
val IosYellow = Color(0xFFFFD60A)                  // Apple System Yellow

// iOS Typography & Vibrancy
val IosLabelPrimary = Color(0xFFFFFFFF)            // 100% White
val IosLabelSecondary = Color(0x99EBEBF5)          // 60% Vibrancy
val IosLabelTertiary = Color(0x4DEBEBF5)           // 30% Muted
val IosLabelQuaternary = Color(0x2EEBEBF5)         // 18% Subtle

// ─── Studio Compatibility Aliases ───────────────────────────────────────────
val StudioBackground = IosSystemBackground
val StudioSurface = IosSecondaryBackground
val StudioSurfaceVariant = IosTertiaryBackground
val StudioBorder = Color(0x26FFFFFF)

val ElectricIndigo = IosIndigo
val ElectricIndigoLight = Color(0xFF7D7AFF)
val NeonCyan = IosCyan
val PlayheadRed = IosRed
val EmeraldAudio = IosGreen
val GreenAudio = EmeraldAudio
val AmberText = IosOrange
val CyanShape = IosCyan
val PurpleVideo = IosIndigo
val OrangeAdjustment = IosOrange

val TextPrimary = IosLabelPrimary
val TextSecondary = Color(0xFF9898A0)
val TextMuted = Color(0xFF686873)
