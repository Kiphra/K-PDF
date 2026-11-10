package com.kiphra.kpdf.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================================
// JUT CONNECT INSPIRED OBSIDIAN & CRIMSON DESIGN SYSTEM
// ============================================================================

// Brand Accent Colors (Crimson & Scarlet matching K-PDF Orbit Logo)
val JutCrimsonPrimary = Color(0xFFE53935)       // Vibrant Crimson
val JutCrimsonDeep = Color(0xFFAD0908)          // Signature JUT Red
val JutDarkRed = Color(0xFF8B0000)              // Deep Scarlet Accent

// Status & Indicator Accents
val JutEmeraldOnline = Color(0xFF10B981)        // Live / Online / Complete
val JutAmberWarning = Color(0xFFF59E0B)         // Warning / Attention
val JutSkyBlue = Color(0xFF0284C7)              // Info / Link

// ----------------------------------------------------------------------------
// Dark Theme Colors (Refined Obsidian Palette - Zero AI-slop purple)
// ----------------------------------------------------------------------------
val JutDarkScaffoldBg = Color(0xFF0D0E12)       // Pure pitch obsidian
val JutDarkCardBg = Color(0xFF16171D)           // Primary card container
val JutDarkCardElevated = Color(0xFF1C1D24)     // Elevated surface / Pill chip
val JutDarkAppBarBg = Color(0xFF13141A)         // Top / Bottom app bar
val JutDarkTextColor = Color(0xFFF3F4F6)        // Crisp readable white
val JutDarkTextMuted = Color(0xFF8E91A0)        // Technical slate muted text
val JutDarkBorderColor = Color(0xFF262732)      // Subtle hairline border (0.75dp)

// ----------------------------------------------------------------------------
// Light Theme Colors (Crisp Paper & Hairline Grays)
// ----------------------------------------------------------------------------
val JutLightScaffoldBg = Color(0xFFF8F9FA)      // Clean paper white
val JutLightCardBg = Color(0xFFFFFFFF)          // Crisp white card
val JutLightCardElevated = Color(0xFFF3F4F6)    // Subtle gray elevated
val JutLightAppBarBg = Color(0xFFFFFFFF)        // App bar
val JutLightTextColor = Color(0xFF111827)       // Deep slate black
val JutLightTextMuted = Color(0xFF6B7280)       // Muted text
val JutLightBorderColor = Color(0xFFE5E7EB)     // Hairline light border

// ----------------------------------------------------------------------------
// Material 3 Color Tokens
// ----------------------------------------------------------------------------
val KiphraDarkPrimary = JutCrimsonPrimary
val KiphraDarkOnPrimary = Color(0xFFFFFFFF)
val KiphraDarkPrimaryContainer = Color(0xFF3B0000)
val KiphraDarkOnPrimaryContainer = Color(0xFFFFDAD6)

val KiphraDarkSecondary = JutEmeraldOnline
val KiphraDarkOnSecondary = Color(0xFFFFFFFF)
val KiphraDarkSecondaryContainer = Color(0xFF003920)
val KiphraDarkOnSecondaryContainer = Color(0xFFA6F5C9)

val KiphraDarkTertiary = JutAmberWarning
val KiphraDarkOnTertiary = Color(0xFFFFFFFF)
val KiphraDarkTertiaryContainer = Color(0xFF3F2E00)
val KiphraDarkOnTertiaryContainer = Color(0xFFFFDF9E)

val KiphraDarkBackground = JutDarkScaffoldBg
val KiphraDarkOnBackground = JutDarkTextColor
val KiphraDarkSurface = JutDarkCardBg
val KiphraDarkOnSurface = JutDarkTextColor
val KiphraDarkSurfaceVariant = JutDarkCardElevated
val KiphraDarkOnSurfaceVariant = JutDarkTextMuted
val KiphraDarkOutline = JutDarkBorderColor
val KiphraDarkOutlineVariant = Color(0xFF1F202B)

// Light Theme Tokens
val KiphraLightPrimary = JutCrimsonDeep
val KiphraLightOnPrimary = Color(0xFFFFFFFF)
val KiphraLightPrimaryContainer = Color(0xFFFFDAD6)
val KiphraLightOnPrimaryContainer = Color(0xFF410002)

val KiphraLightSecondary = Color(0xFF047857)
val KiphraLightOnSecondary = Color(0xFFFFFFFF)
val KiphraLightSecondaryContainer = Color(0xFFA6F5C9)
val KiphraLightOnSecondaryContainer = Color(0xFF002111)

val KiphraLightTertiary = Color(0xFFD97706)
val KiphraLightOnTertiary = Color(0xFFFFFFFF)
val KiphraLightTertiaryContainer = Color(0xFFFFDF9E)
val KiphraLightOnTertiaryContainer = Color(0xFF261900)

val KiphraLightBackground = JutLightScaffoldBg
val KiphraLightOnBackground = JutLightTextColor
val KiphraLightSurface = JutLightCardBg
val KiphraLightOnSurface = JutLightTextColor
val KiphraLightSurfaceVariant = JutLightCardElevated
val KiphraLightOnSurfaceVariant = JutLightTextMuted
val KiphraLightOutline = JutLightBorderColor

// Backward-compatibility aliases
val KiphraBluePrimary = JutCrimsonDeep
val KiphraBlueOnPrimary = Color(0xFFFFFFFF)
val KiphraBlueContainer = Color(0xFFFFDAD6)
val KiphraOnBlueContainer = Color(0xFF410002)
val KiphraSecondary = JutEmeraldOnline
val KiphraOnSecondary = Color(0xFFFFFFFF)
val KiphraSecondaryContainer = Color(0xFFA6F5C9)
val KiphraOnSecondaryContainer = Color(0xFF002111)
val KiphraTertiary = JutAmberWarning
val KiphraOnTertiary = Color(0xFFFFFFFF)
val KiphraTertiaryContainer = Color(0xFFFFDF9E)
val KiphraOnTertiaryContainer = Color(0xFF261900)
val Purple80 = KiphraDarkPrimary
val PurpleGrey80 = KiphraDarkSecondary
val Pink80 = KiphraDarkTertiary
val Purple40 = KiphraLightPrimary
val PurpleGrey40 = KiphraLightSecondary
val Pink40 = KiphraLightTertiary