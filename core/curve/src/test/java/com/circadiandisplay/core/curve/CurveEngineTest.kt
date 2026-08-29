package com.circadiandisplay.core.curve

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Full test suite for [CurveEngine].
 *
 * Covers every edge case specified in the architecture documentation.
 * These tests are the contract for interpolation correctness.
 */
class CurveEngineTest {
    private lateinit var engine: CurveEngine

    @Before
    fun setUp() {
        engine = CurveEngine()
    }

    // ── Empty / Edge ──────────────────────────────────────────────────────

    @Test
    fun `empty profile returns safe default`() {
        val profile = CurveProfile(id = 1, name = "Test")
        val result = engine.calculateDisplayState(profile, emptyList(), 1200)

        assertEquals(0f, result.warmth)
        assertEquals(0f, result.dimming)
    }

    @Test
    fun `points with non-matching profileId are filtered out`() {
        val profile = CurveProfile(id = 1, name = "Test")
        val points =
            listOf(
                CurvePoint(id = 1, profileId = 999, timeMinutes = 1200, warmth = 0.5f, dimming = 0.5f),
            )

        val result = engine.calculateDisplayState(profile, points, 1200)

        // No points belong to this profile → safe default
        assertEquals(0f, result.warmth)
        assertEquals(0f, result.dimming)
    }

    // ── Single Point ──────────────────────────────────────────────────────

    @Test
    fun `single point returns its values regardless of time`() {
        val profile = CurveProfile(id = 1, name = "Test")
        val points =
            listOf(
                CurvePoint(id = 1, profileId = 1, timeMinutes = 1200, warmth = 0.7f, dimming = 0.3f),
            )

        // different time
        val result = engine.calculateDisplayState(profile, points, 1320)

        assertEquals(0.7f, result.warmth)
        assertEquals(0.3f, result.dimming)
    }

    // ── Exact Match ───────────────────────────────────────────────────────

    @Test
    fun `time exactly on a point returns its exact values`() {
        val profile = CurveProfile(id = 1, name = "Test")
        val points =
            listOf(
                CurvePoint(id = 1, profileId = 1, timeMinutes = 1080, warmth = 0.0f, dimming = 0.0f),
                CurvePoint(id = 2, profileId = 1, timeMinutes = 1200, warmth = 0.4f, dimming = 0.1f),
                CurvePoint(id = 3, profileId = 1, timeMinutes = 1320, warmth = 0.8f, dimming = 0.3f),
            )

        val result = engine.calculateDisplayState(profile, points, 1200)

        assertEquals(0.4f, result.warmth)
        assertEquals(0.1f, result.dimming)
    }

    // ── Midpoint Interpolation ────────────────────────────────────────────

    @Test
    fun `time exactly halfway between two points interpolates at 0_5`() {
        val profile = CurveProfile(id = 1, name = "Test")
        val points =
            listOf(
                CurvePoint(id = 1, profileId = 1, timeMinutes = 1200, warmth = 0.2f, dimming = 0.0f),
                CurvePoint(id = 2, profileId = 1, timeMinutes = 1320, warmth = 0.8f, dimming = 1.0f),
            )

        // exactly halfway
        val result = engine.calculateDisplayState(profile, points, 1260)

        assertEquals(0.5f, result.warmth)
        assertEquals(0.5f, result.dimming)
    }

    @Test
    fun `time off-center between two points interpolates correctly`() {
        val profile = CurveProfile(id = 1, name = "Test")
        val points =
            listOf(
                CurvePoint(id = 1, profileId = 1, timeMinutes = 1200, warmth = 0.2f, dimming = 0.0f),
                CurvePoint(id = 2, profileId = 1, timeMinutes = 1320, warmth = 0.8f, dimming = 1.0f),
            )

        // 1230 is 25% of the way from 1200 to 1320
        val result = engine.calculateDisplayState(profile, points, 1230)

        assertEquals(0.35f, result.warmth, 0.0001f)
        assertEquals(0.25f, result.dimming, 0.0001f)
    }

    // ── Clamping ──────────────────────────────────────────────────────────

    @Test
    fun `time before first point clamps to first point values`() {
        val profile = CurveProfile(id = 1, name = "Test")
        val points =
            listOf(
                CurvePoint(id = 1, profileId = 1, timeMinutes = 1200, warmth = 0.4f, dimming = 0.1f),
                CurvePoint(id = 2, profileId = 1, timeMinutes = 1320, warmth = 0.8f, dimming = 0.3f),
            )

        // before first point
        val result = engine.calculateDisplayState(profile, points, 1080)

        assertEquals(0.4f, result.warmth)
        assertEquals(0.1f, result.dimming)
    }

    @Test
    fun `time after last point clamps to last point values`() {
        val profile = CurveProfile(id = 1, name = "Test")
        val points =
            listOf(
                CurvePoint(id = 1, profileId = 1, timeMinutes = 1200, warmth = 0.4f, dimming = 0.1f),
                CurvePoint(id = 2, profileId = 1, timeMinutes = 1320, warmth = 0.8f, dimming = 0.3f),
            )

        // after last point
        val result = engine.calculateDisplayState(profile, points, 1380)

        assertEquals(0.8f, result.warmth)
        assertEquals(0.3f, result.dimming)
    }

    @Test
    fun `time exactly on first point returns first point values`() {
        val profile = CurveProfile(id = 1, name = "Test")
        val points =
            listOf(
                CurvePoint(id = 1, profileId = 1, timeMinutes = 1200, warmth = 0.4f, dimming = 0.1f),
                CurvePoint(id = 2, profileId = 1, timeMinutes = 1320, warmth = 0.8f, dimming = 0.3f),
            )

        val result = engine.calculateDisplayState(profile, points, 1200)

        assertEquals(0.4f, result.warmth)
        assertEquals(0.1f, result.dimming)
    }

    @Test
    fun `time exactly on last point returns last point values`() {
        val profile = CurveProfile(id = 1, name = "Test")
        val points =
            listOf(
                CurvePoint(id = 1, profileId = 1, timeMinutes = 1200, warmth = 0.4f, dimming = 0.1f),
                CurvePoint(id = 2, profileId = 1, timeMinutes = 1320, warmth = 0.8f, dimming = 0.3f),
            )

        val result = engine.calculateDisplayState(profile, points, 1320)

        assertEquals(0.8f, result.warmth)
        assertEquals(0.3f, result.dimming)
    }

    // ── Out of Order ──────────────────────────────────────────────────────

    @Test
    fun `points given out of order produce same result as sorted`() {
        val profile = CurveProfile(id = 1, name = "Test")
        val unsorted =
            listOf(
                CurvePoint(id = 3, profileId = 1, timeMinutes = 1320, warmth = 1.0f, dimming = 0.5f),
                CurvePoint(id = 1, profileId = 1, timeMinutes = 1080, warmth = 0.0f, dimming = 0.0f),
                CurvePoint(id = 2, profileId = 1, timeMinutes = 1200, warmth = 0.5f, dimming = 0.2f),
            )

        // halfway between 1080 and 1200
        val result = engine.calculateDisplayState(profile, unsorted, 1140)

        assertEquals(0.25f, result.warmth)
        assertEquals(0.10f, result.dimming)
    }

    // ── Duplicate timeMinutes ─────────────────────────────────────────────

    @Test
    fun `duplicate timeMinutes uses first occurrence and does not crash`() {
        val profile = CurveProfile(id = 1, name = "Test")
        val points =
            listOf(
                CurvePoint(id = 1, profileId = 1, timeMinutes = 1200, warmth = 0.3f, dimming = 0.1f),
                // duplicate time
                CurvePoint(id = 2, profileId = 1, timeMinutes = 1200, warmth = 0.9f, dimming = 0.9f),
                CurvePoint(id = 3, profileId = 1, timeMinutes = 1320, warmth = 0.8f, dimming = 0.3f),
            )

        // At exactly 1200, should get first occurrence
        val result = engine.calculateDisplayState(profile, points, 1200)

        assertEquals(0.3f, result.warmth)
        assertEquals(0.1f, result.dimming)
    }

    // ── Independent Warmth/Dimming Interpolation ──────────────────────────

    @Test
    fun `warmth and dimming interpolate independently`() {
        val profile = CurveProfile(id = 1, name = "Test")
        val points =
            listOf(
                // warmth goes 0.0 → 1.0, dimming goes 0.0 → 0.5 over the same interval
                CurvePoint(id = 1, profileId = 1, timeMinutes = 1200, warmth = 0.0f, dimming = 0.0f),
                CurvePoint(id = 2, profileId = 1, timeMinutes = 1320, warmth = 1.0f, dimming = 0.5f),
            )

        // midpoint
        val result = engine.calculateDisplayState(profile, points, 1260)

        assertEquals(0.5f, result.warmth)
        assertEquals(0.25f, result.dimming)
    }

    // ── Boundary Values ───────────────────────────────────────────────────

    @Test
    fun `boundary values 0_0 and 1_0 are returned exactly`() {
        val profile = CurveProfile(id = 1, name = "Test")
        val points =
            listOf(
                CurvePoint(id = 1, profileId = 1, timeMinutes = 0, warmth = 0.0f, dimming = 0.0f),
                CurvePoint(id = 2, profileId = 1, timeMinutes = 1439, warmth = 1.0f, dimming = 1.0f),
            )

        val resultStart = engine.calculateDisplayState(profile, points, 0)
        assertEquals(0.0f, resultStart.warmth)
        assertEquals(0.0f, resultStart.dimming)

        val resultEnd = engine.calculateDisplayState(profile, points, 1439)
        assertEquals(1.0f, resultEnd.warmth)
        assertEquals(1.0f, resultEnd.dimming)
    }

    // ── Full Default Profile ──────────────────────────────────────────────

    @Test
    fun `default seed profile interpolates correctly at 2100`() {
        val profile = CurveProfile(id = 1, name = "Evening")
        val points =
            listOf(
                // 18:00
                CurvePoint(id = 1, profileId = 1, timeMinutes = 1080, warmth = 0.0f, dimming = 0.0f),
                // 20:00
                CurvePoint(id = 2, profileId = 1, timeMinutes = 1200, warmth = 0.4f, dimming = 0.1f),
                // 22:00
                CurvePoint(id = 3, profileId = 1, timeMinutes = 1320, warmth = 0.8f, dimming = 0.3f),
                // 23:00
                CurvePoint(id = 4, profileId = 1, timeMinutes = 1380, warmth = 1.0f, dimming = 0.5f),
            )

        // 21:00 (1260) is halfway between 20:00 (1200) and 22:00 (1320)
        val result = engine.calculateDisplayState(profile, points, 1260)

        assertEquals(0.6f, result.warmth)
        assertEquals(0.2f, result.dimming, 0.0001f)
    }

    // ── Validation ────────────────────────────────────────────────────────

    @Test(expected = IllegalArgumentException::class)
    fun `CurvePoint with warmth below 0 throws`() {
        CurvePoint(timeMinutes = 720, warmth = -0.1f, dimming = 0.5f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `CurvePoint with warmth above 1 throws`() {
        CurvePoint(timeMinutes = 720, warmth = 1.1f, dimming = 0.5f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `CurvePoint with dimming below 0 throws`() {
        CurvePoint(timeMinutes = 720, warmth = 0.5f, dimming = -0.01f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `CurvePoint with dimming above 1 throws`() {
        CurvePoint(timeMinutes = 720, warmth = 0.5f, dimming = 1.01f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `CurvePoint with timeMinutes below 0 throws`() {
        CurvePoint(timeMinutes = -1, warmth = 0.5f, dimming = 0.5f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `CurvePoint with timeMinutes above 1439 throws`() {
        CurvePoint(timeMinutes = 1440, warmth = 0.5f, dimming = 0.5f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `CurveProfile with blank name throws`() {
        CurveProfile(name = "   ")
    }
}
