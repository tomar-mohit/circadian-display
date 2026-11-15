package com.circadiandisplay.app.scheduler

import com.circadiandisplay.core.curve.CurvePoint
import java.time.Instant
import java.time.ZoneId

/**
 * Pure math for event-driven scheduling (issue #7).
 *
 * The periodic WorkManager job polls every ~15 minutes, but JobScheduler
 * batches background work into Doze maintenance windows, so on aggressive
 * ROMs (notably GrapheneOS) updates can be deferred for hours and the display
 * gets stuck until the app is opened. Because a curve is fully predictable,
 * the exact moment the rendered output next changes can be computed and
 * scheduled with an exact alarm instead of polling.
 *
 * Between curve points [CurveEngine] linearly interpolates, so the rendered
 * state only changes at point times ("knees"). Points that carry the same
 * warmth/dimming value as the preceding point produce no visual change and are
 * not knees.
 */
object CurveSchedule {
    private const val MILLIS_PER_MINUTE = 60_000L
    private const val MILLIS_PER_DAY = 24 * 60 * 60 * 1000L

    /**
     * Returns the epoch millisecond instant at which the rendered display
     * state next changes for the given curve, or `null` when no exact alarm
     * should be scheduled (empty curve, or a constant curve that renders the
     * same value forever).
     *
     * Times are resolved against [zoneId]; a day is assumed to be exactly 24h,
     * so DST transitions are tolerated until the next timezone/time change
     * broadcast re-anchors the schedule.
     *
     * @param points the active profile's points (any order)
     * @param nowMillis current wall-clock time, epoch milliseconds
     */
    fun nextChangeEpochMillis(
        points: List<CurvePoint>,
        nowMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Long? {
        if (points.isEmpty()) return null

        val knees = mutableListOf<CurvePoint>()
        var previousValue: Pair<Float, Float>? = null
        for (point in points.sortedBy { it.timeMinutes }) {
            val value = point.warmth to point.dimming
            if (value != previousValue) {
                knees.add(point)
                previousValue = value
            }
        }
        if (knees.size <= 1) return null

        val now = Instant.ofEpochMilli(nowMillis).atZone(zoneId)
        val todayStart =
            now.toLocalDate().atStartOfDay(zoneId).toInstant().toEpochMilli()
        val minuteOfDay = (nowMillis - todayStart) / MILLIS_PER_MINUTE

        val nextKneeToday = knees.map { it.timeMinutes }.firstOrNull { it > minuteOfDay }
        val targetMinutes = nextKneeToday ?: knees.first().timeMinutes
        val targetDayStart = if (nextKneeToday != null) todayStart else todayStart + MILLIS_PER_DAY
        return targetDayStart + targetMinutes * MILLIS_PER_MINUTE
    }
}
