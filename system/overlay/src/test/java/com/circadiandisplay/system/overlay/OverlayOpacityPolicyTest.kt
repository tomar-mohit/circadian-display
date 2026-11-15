package com.circadiandisplay.system.overlay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the overlay opacity clamp that keeps the combined presented
 * opacity of the dimming and warmth layers below the touch pass-through
 * ceiling (issue #8).
 */
class OverlayOpacityPolicyTest {
    private val ceiling = 0.76f // 0.8 system ceiling with the 0.95 safety margin

    @Test
    fun `zero dimming leaves warmth untouched when within ceiling`() {
        val (d, w) = OverlayOpacityPolicy.clamp(dimming = 0f, warmth = 0.5f, ceiling = ceiling)
        assertEquals(0f, d, EPSILON)
        assertEquals(0.5f, w, EPSILON)
    }

    @Test
    fun `zero warmth leaves dimming untouched when within ceiling`() {
        val (d, w) = OverlayOpacityPolicy.clamp(dimming = 0.4f, warmth = 0f, ceiling = ceiling)
        assertEquals(0.4f, d, EPSILON)
        assertEquals(0f, w, EPSILON)
    }

    @Test
    fun `typical night state is not clamped`() {
        // dimming 0.5 + warmth 0.5 presents a combined opacity of 0.75, which
        // is within the margined ceiling and must render as requested.
        val (d, w) = OverlayOpacityPolicy.clamp(dimming = 0.5f, warmth = 0.5f, ceiling = ceiling)
        assertEquals(0.5f, d, EPSILON)
        assertEquals(0.5f, w, EPSILON)
    }

    @Test
    fun `combined opacity never exceeds the ceiling`() {
        val dimmings = listOf(0f, 0.25f, 0.4f, 0.5f, 0.6f, 0.75f, 0.9f, 1f)
        val warms = listOf(0f, 0.25f, 0.5f, 0.75f, 1f)
        for (dimming in dimmings) {
            for (warmth in warms) {
                val (d, w) = OverlayOpacityPolicy.clamp(dimming, warmth, ceiling)
                val net = OverlayOpacityPolicy.combinedOpacity(d, w)
                assertTrue(
                    "net $net exceeds ceiling $ceiling for (dimming=$dimming, warmth=$warmth)",
                    net <= ceiling + EPSILON,
                )
            }
        }
    }

    @Test
    fun `full black dimming consumes the whole budget`() {
        val (d, w) = OverlayOpacityPolicy.clamp(dimming = 1f, warmth = 1f, ceiling = ceiling)
        assertEquals(ceiling, d, EPSILON)
        assertEquals(0f, w, EPSILON)
    }

    @Test
    fun `warmth shrinks before dimming does`() {
        // dimming 0.6 leaves a warmth budget of 1 - 0.24/0.4 = 0.4, so warmth
        // (0.5) is reduced while dimming stays at the requested 0.6.
        val (d, w) = OverlayOpacityPolicy.clamp(dimming = 0.6f, warmth = 0.5f, ceiling = ceiling)
        assertEquals(0.6f, d, EPSILON)
        assertEquals(0.4f, w, EPSILON)
    }

    @Test
    fun `lone warmth above the ceiling is capped`() {
        val (d, w) = OverlayOpacityPolicy.clamp(dimming = 0f, warmth = 1f, ceiling = ceiling)
        assertEquals(0f, d, EPSILON)
        assertEquals(ceiling, w, EPSILON)
    }

    @Test
    fun `unit ceiling disables budget logic`() {
        // Pre-Android 12 behaviour: no touch-blocking mechanism, warmth is
        // independent of dimming.
        val (d, w) = OverlayOpacityPolicy.clamp(dimming = 1f, warmth = 1f, ceiling = 1f)
        assertEquals(1f, d, EPSILON)
        assertEquals(1f, w, EPSILON)
    }

    @Test
    fun `inputs are coerced into range`() {
        val (d, w) = OverlayOpacityPolicy.clamp(dimming = -0.5f, warmth = 1.5f, ceiling = ceiling)
        assertEquals(0f, d, EPSILON)
        // Coerced to 1.0 first, then capped at the ceiling.
        assertEquals(ceiling, w, EPSILON)
    }

    @Test
    fun `combined opacity formula matches manual calculation`() {
        assertEquals(0f, OverlayOpacityPolicy.combinedOpacity(0f, 0f), EPSILON)
        assertEquals(0.5f, OverlayOpacityPolicy.combinedOpacity(0.5f, 0f), EPSILON)
        assertEquals(0.75f, OverlayOpacityPolicy.combinedOpacity(0.5f, 0.5f), EPSILON)
        assertEquals(1f, OverlayOpacityPolicy.combinedOpacity(1f, 0.5f), EPSILON)
    }

    private companion object {
        const val EPSILON = 0.001f
    }
}
