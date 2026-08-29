package com.circadiandisplay.core.curve

/**
 * Pure domain logic. Accepts a [CurveProfile] and a time value
 * (minutes since midnight) and returns the interpolated [DisplayState].
 *
 * This class has **zero Android framework dependencies**.
 * It is the single most critical component in the codebase for correctness
 * and must maintain 100% unit test coverage.
 *
 * ## Edge Cases (all explicitly handled)
 *
 * | Scenario | Behaviour |
 * |---|---|
 * | Empty profile (no points) | Returns safe default: `DisplayState(warmth=0f, dimming=0f)` |
 * | Single point | Always returns that point's values, regardless of current time |
 * | Time before first point | Clamps to first point's values |
 * | Time after last point | Clamps to last point's values |
 * | Time exactly on a point | Returns that point's exact values (no interpolation) |
 * | Duplicate timeMinutes | Sorts first, uses first occurrence, interpolates normally |
 * | Points provided out of order | Sorts by timeMinutes ascending before processing |
 */
class CurveEngine {
    /**
     * Calculates the [DisplayState] for the given [profile] at the given [timeMinutes].
     *
     * @param profile     The profile containing the curve points to evaluate.
     *                    Points are sorted internally — callers do not need to pre-sort.
     * @param timeMinutes Minutes since midnight (0–1439) representing the time to evaluate.
     * @return The interpolated [DisplayState].
     */
    fun calculateDisplayState(
        profile: CurveProfile,
        points: List<CurvePoint>,
        timeMinutes: Int,
    ): DisplayState {
        if (points.isEmpty()) {
            return DisplayState(warmth = 0f, dimming = 0f)
        }

        val sorted =
            points
                .filter { it.profileId == profile.id }
                .distinctBy { it.timeMinutes }
                .sortedBy { it.timeMinutes }

        if (sorted.isEmpty()) {
            return DisplayState(warmth = 0f, dimming = 0f)
        }

        // Clamp: time is before the first point
        if (timeMinutes <= sorted.first().timeMinutes) {
            return DisplayState(
                warmth = sorted.first().warmth,
                dimming = sorted.first().dimming,
            )
        }

        // Clamp: time is after the last point
        if (timeMinutes >= sorted.last().timeMinutes) {
            return DisplayState(
                warmth = sorted.last().warmth,
                dimming = sorted.last().dimming,
            )
        }

        // Find the two points that bound the current time
        val index = sorted.indexOfFirst { it.timeMinutes > timeMinutes }
        val before = sorted[index - 1]
        val after = sorted[index]

        // Linear interpolation
        val range = (after.timeMinutes - before.timeMinutes).toFloat()
        val progress = if (range == 0f) 0f else (timeMinutes - before.timeMinutes) / range

        return DisplayState(
            warmth = lerp(before.warmth, after.warmth, progress),
            dimming = lerp(before.dimming, after.dimming, progress),
        )
    }

    /**
     * Linear interpolation between [a] and [b] by [t].
     * t=0.0 returns a, t=1.0 returns b.
     */
    private fun lerp(
        a: Float,
        b: Float,
        t: Float,
    ): Float {
        return a + (b - a) * t
    }
}
