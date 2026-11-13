package com.circadiandisplay.app.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.circadiandisplay.app.data.repository.CurveRepository
import com.circadiandisplay.app.data.settings.AppSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules exact, event-driven evaluations at the next curve "knee point"
 * (the next instant the rendered display state actually changes).
 *
 * ## Why exact alarms? (issue #7)
 *
 * The periodic WorkManager job is subject to JobScheduler/Doze batching: on
 * strict ROMs (GrapheneOS, aggressive OEM power management) a 15-minute
 * periodic job can be deferred for hours, leaving the display stuck at a stale
 * temperature until the app is opened. A curve is fully predictable, so the
 * next change is scheduled with [AlarmManager.setExactAndAllowWhileIdle],
 * which fires even in Doze. Each fired alarm evaluates and then re-schedules
 * the following knee, so the schedule stays correct across profile edits and
 * master-toggle changes.
 *
 * The periodic WorkManager job is kept as a fallback (e.g. if the user revokes
 * the exact-alarm permission, the fallback still updates the display within
 * its window).
 *
 * ## Permissions
 *
 * - API < 31: exact alarms need only the normal
 *   [android.Manifest.permission.SCHEDULE_EXACT_ALARM] manifest permission.
 * - API 31+: additionally requires the user to allow "Alarms & reminders" for
 *   the app; when revoked, the inexact `setAndAllowWhileIdle` fallback is used
 *   and the Settings screen shows a grant row.
 */
@Singleton
class SchedulerAlarms
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val appSettings: AppSettings,
        private val curveRepository: CurveRepository,
    ) {
        private val alarmManager =
            context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        /**
         * Computes the next knee of the active profile and (re)schedules the
         * exact evaluation alarm. Cancels any pending alarm when scheduling
         * is not applicable (disabled, no profile, or a constant curve).
         *
         * Safe to call after every evaluation and after every curve/profile
         * change — it always converges to the correct next alarm.
         */
        suspend fun scheduleNext() {
            val pendingIntent = pendingIntent()

            if (!appSettings.isEnabledSnapshot()) {
                alarmManager.cancel(pendingIntent)
                Log.d(TAG, "Disabled — exact alarm cancelled")
                return
            }

            val profile = curveRepository.getActiveProfile()
            val points = profile?.let { curveRepository.getPointsByProfileIdOnce(it.id) }
            val nextChange =
                points?.let { CurveSchedule.nextChangeEpochMillis(it, System.currentTimeMillis()) }

            if (nextChange == null) {
                alarmManager.cancel(pendingIntent)
                Log.d(TAG, "No upcoming curve change — exact alarm cancelled")
                return
            }

            val canExact = canScheduleExactAlarms()
            if (canExact) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextChange, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextChange, pendingIntent)
            }
            Log.d(
                TAG,
                "Exact alarm scheduled at $nextChange " +
                    "(${java.text.SimpleDateFormat.getDateTimeInstance().format(nextChange)}, " +
                    if (canExact) "exact" else "inexact fallback",
            )
        }

        /** Cancels the pending evaluation alarm. */
        fun cancel() {
            alarmManager.cancel(pendingIntent())
            Log.d(TAG, "Exact alarm cancelled")
        }

        private fun pendingIntent(): PendingIntent {
            val intent =
                Intent(context, AlarmReceiver::class.java).setAction(AlarmReceiver.ACTION_EVALUATE)
            return PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        private fun canScheduleExactAlarms(): Boolean =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

        private companion object {
            const val TAG = "SchedulerAlarms"
            const val REQUEST_CODE = 1001
        }
    }
