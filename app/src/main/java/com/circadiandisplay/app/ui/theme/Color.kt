package com.circadiandisplay.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Brand and scheme colours.
 *
 * The app's identity is amber: the curve itself is drawn in [Amber]. The dark scheme is built on
 * warm neutrals derived from that same hue so the chrome reads as low evening light rather than
 * Material's cool default greys. The light scheme deliberately stays on Material's baseline
 * palette — 2.0.0 introduces a theme *system*, not a restyle, and a light-mode repaint would
 * change the appearance of every existing user (DECISIONS.md #010).
 */

/** The curve's warmth accent. Shared by the curve graph, its legend, and the warmth indicators. */
val Amber = Color(0xFFFF9800)

// ── Dark scheme: warm neutrals ────────────────────────────────────────────────

internal val WarmBlack = Color(0xFF130F0D)
internal val WarmSurface = Color(0xFF1A1613)
internal val WarmSurfaceContainerLowest = Color(0xFF160F0C)
internal val WarmSurfaceContainerLow = Color(0xFF221D19)
internal val WarmSurfaceContainer = Color(0xFF26201C)
internal val WarmSurfaceContainerHigh = Color(0xFF312A25)
internal val WarmSurfaceContainerHighest = Color(0xFF3C342E)
internal val WarmSurfaceBright = Color(0xFF3B342E)
internal val WarmSurfaceVariant = Color(0xFF4C443C)
internal val WarmOnSurface = Color(0xFFEDE2D8)
internal val WarmOnSurfaceVariant = Color(0xFFCFC1B4)
internal val WarmOutline = Color(0xFF9A8B7D)
internal val WarmOutlineVariant = Color(0xFF4C443C)

// ── Dark scheme: amber accents (light tones, per Material's dark-scheme guidance) ──

internal val AmberLight = Color(0xFFFFB96E)
internal val OnAmberLight = Color(0xFF4A2800)
internal val AmberContainerDark = Color(0xFF6B3F00)
internal val OnAmberContainerDark = Color(0xFFFFDDB8)
internal val SandLight = Color(0xFFE2C18F)
internal val OnSandLight = Color(0xFF412D05)
internal val SandContainerDark = Color(0xFF5A4218)
internal val OnSandContainerDark = Color(0xFFFFDDB1)
internal val RoseLight = Color(0xFFD8B4A0)
internal val OnRoseLight = Color(0xFF3E2C20)
internal val RoseContainerDark = Color(0xFF574033)
internal val OnRoseContainerDark = Color(0xFFF5DDD0)

// ── Graph ─────────────────────────────────────────────────────────────────────

/** Dimming is deliberately neutral so it never competes with the warmth curve's amber. */
internal val DimLineLight = Color(0xFF757575)
internal val DimLineDark = Color(0xFFB9AC9E)
internal val GridLight = Color(0x22000000)
internal val GridDark = Color(0x2EFFFFFF)
