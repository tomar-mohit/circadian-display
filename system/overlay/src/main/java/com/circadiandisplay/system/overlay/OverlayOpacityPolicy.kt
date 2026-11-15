package com.circadiandisplay.system.overlay

import kotlin.math.max

/**
 * Pure math for computing the on-screen (presented) opacity of the overlay's
 * two layers such that their combined opacity never exceeds the touch
 * pass-through ceiling.
 *
 * Issue #8: the two layers drawn inside one window compound multiplicatively
 * — a black layer at opacity d over an amber layer at opacity w presents a
 * combined opacity of `1 - (1 - d)(1 - w)`, which exceeds the system's
 * maximum obscuring opacity for touch (0.8 by default on API 31+) whenever
 * both layers are moderately strong (e.g. d = 0.5, w = 0.5 -> 0.75 net — but
 * d = 0.6, w = 0.5 -> 0.8 net). OEM input stacks (notably realme/ColorOS on
 * Android 16) block touches when the *presented* combined opacity of an
 * overlay window exceeds the ceiling, so the combined value must be clamped.
 *
 * Policy:
 * - Dimming takes precedence: it is capped at [ceiling] directly.
 * - Warmth fills the remaining opacity budget:
 *   `budget = 1 - (1 - ceiling) / (1 - dimming)`, which is the largest warmth
 *   opacity for which `1 - (1 - dimming)(1 - warmth) <= ceiling` holds.
 * - A single-layer overlay (warmth = 0) is never clamped below [ceiling], and
 *   when [ceiling] is 1.0 (pre-Android 12, no touch-blocking mechanism) no
 *   budget logic applies at all.
 *
 * All inputs and outputs are opacities in 0..1. These are the values as
 * presented on screen (i.e. after the window-level alpha), not the alpha
 * values passed to the drawing layer.
 */
internal object OverlayOpacityPolicy {
    /**
     * Resolves the presented dimming and warmth opacities.
     *
     * @param dimming requested dimming opacity, 0..1
     * @param warmth requested warmth opacity (already includes the warmth
     *   strength cap), 0..1
     * @param ceiling maximum combined presented opacity, 0..1
     * @return presented `(dimming, warmth)` pair whose combined opacity
     *   `1 - (1 - d)(1 - w)` is at most [ceiling]
     */
    fun clamp(
        dimming: Float,
        warmth: Float,
        ceiling: Float,
    ): Pair<Float, Float> {
        val d = dimming.coerceIn(0f, 1f)
        val w = warmth.coerceIn(0f, 1f)
        val cap = ceiling.coerceIn(0f, 1f)

        if (cap >= 1f) {
            // No touch-blocking ceiling (pre-Android 12): warmth is independent
            // of dimming.
            return d to w
        }

        val presentedDimming = d.coerceAtMost(cap)
        val warmthBudget = (1f - (1f - cap) / (1f - presentedDimming)).coerceIn(0f, 1f)
        val presentedWarmth = w.coerceAtMost(warmthBudget)
        return presentedDimming to presentedWarmth
    }

    /** Combined presented opacity of two stacked translucent layers. */
    fun combinedOpacity(
        first: Float,
        second: Float,
    ): Float = 1f - (1f - max(0f, first.coerceAtMost(1f))) * (1f - max(0f, second.coerceAtMost(1f)))
}
