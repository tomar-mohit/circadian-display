package com.circadiandisplay.app.scheduler

import com.circadiandisplay.core.curve.CurvePoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneId

/**
 * Tests for the next-change ("knee point") computation used to schedule exact
 * alarms (issue #7).
 */
class CurveScheduleTest {
    private val zone = ZoneId.of("UTC")

    private fun point(
        timeMinutes: Int,
        warmth: Float,
        dimming: Float,
    ): CurvePoint =
        CurvePoint(
            id = 0L,
            profileId = 1L,
            timeMinutes = timeMinutes,
            warmth = warmth,
            dimming = dimming,
        )

    private fun expectedEpoch(
        day: Int,
        hour: Int,
        minute: Int,
    ): Long = (day * 24 * 60 + hour * 60 + minute) * 60_000L

    @Test
    fun `empty curve schedules nothing`() {
        assertNull(CurveSchedule.nextChangeEpochMillis(emptyList(), nowMillis = 0L, zoneId = zone))
    }

    @Test
    fun `constant curve schedules nothing`() {
        val points =
            listOf(
                point(600, 0.2f, 0.1f),
                point(1200, 0.2f, 0.1f),
            )
        assertNull(CurveSchedule.nextChangeEpochMillis(points, nowMillis = 0L, zoneId = zone))
    }

    @Test
    fun `next knee later today is returned`() {
        // Day 0, now = 12:00. Knees at 10:00 (unchanged after), 18:00, 22:00.
        val points =
            listOf(
                point(600, 0f, 0f),
                point(1080, 0.5f, 0.2f),
                point(1320, 1f, 0.5f),
            )
        val now = expectedEpoch(0, 12, 0)
        val next = CurveSchedule.nextChangeEpochMillis(points, now, zone)
        assertEquals(expectedEpoch(0, 18, 0), next)
    }

    @Test
    fun `wraps to tomorrow's first knee after the last one`() {
        val points =
            listOf(
                point(600, 0f, 0f),
                point(1080, 0.5f, 0.2f),
            )
        val now = expectedEpoch(0, 20, 0)
        val next = CurveSchedule.nextChangeEpochMillis(points, now, zone)
        assertEquals(expectedEpoch(1, 10, 0), next)
    }

    @Test
    fun `points with duplicate values are not knees`() {
        // 18:00 carries the same value as 20:00, so the next real change after
        // 19:00 is at 22:00.
        val points =
            listOf(
                point(1080, 0.5f, 0.2f),
                point(1200, 0.5f, 0.2f),
                point(1320, 1f, 0.5f),
            )
        val now = expectedEpoch(0, 19, 0)
        val next = CurveSchedule.nextChangeEpochMillis(points, now, zone)
        assertEquals(expectedEpoch(0, 22, 0), next)
    }

    @Test
    fun `unsorted input is handled`() {
        val points =
            listOf(
                point(1320, 1f, 0.5f),
                point(600, 0f, 0f),
                point(1080, 0.5f, 0.2f),
            )
        val now = expectedEpoch(0, 7, 0)
        val next = CurveSchedule.nextChangeEpochMillis(points, now, zone)
        assertEquals(expectedEpoch(0, 10, 0), next)
    }

    @Test
    fun `first knee may be at midnight`() {
        // All knees are in the past; the first knee (00:00) is scheduled for
        // tomorrow.
        val points =
            listOf(
                point(0, 0f, 0f),
                point(600, 0.5f, 0.2f),
            )
        val now = expectedEpoch(0, 12, 0)
        val next = CurveSchedule.nextChangeEpochMillis(points, now, zone)
        assertEquals(expectedEpoch(1, 0, 0), next)
    }

    @Test
    fun `knee exactly now is skipped`() {
        val points =
            listOf(
                point(600, 0f, 0f),
                point(720, 0.5f, 0.2f),
            )
        val now = expectedEpoch(0, 12, 0)
        val next = CurveSchedule.nextChangeEpochMillis(points, now, zone)
        // 12:00 itself is in the past; next is tomorrow 10:00.
        assertEquals(expectedEpoch(1, 10, 0), next)
    }
}
