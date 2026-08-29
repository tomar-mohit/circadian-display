package com.circadiandisplay.app.scheduler

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.circadiandisplay.app.data.repository.CurveRepository
import com.circadiandisplay.app.data.settings.AppSettings
import com.circadiandisplay.core.curve.CurveEngine
import com.circadiandisplay.core.curve.DisplayController
import com.circadiandisplay.core.curve.DisplayState
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Periodic background worker that evaluates the active curve profile
 * and dispatches the resulting [DisplayState] to the [DisplayController].
 *
 * Scheduled via WorkManager with a ~15-minute interval.
 * Re-enqueues after device reboot via [BootReceiver].
 *
 * ## Edge cases handled
 *
 * - **Disabled**: Clears the display and returns success.
 * - **No active profile**: Clears the display and returns success.
 * - **Empty profile**: CurveEngine returns safe default (0, 0).
 * - **WorkManager deferral**: No special handling — the next cycle will simply
 *   evaluate against the then-current time.
 */
@HiltWorker
class SchedulerWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val curveRepository: CurveRepository,
    private val appSettings: AppSettings,
    private val curveEngine: CurveEngine,
    private val displayController: DisplayController,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            evaluateAndApply()
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Scheduler evaluation failed", e)
            Result.retry()
        }
    }

    private suspend fun evaluateAndApply() {
        // 1. Check if the master toggle is on
        val enabled = appSettings.isEnabledSnapshot()
        if (!enabled) {
            Log.d(TAG, "Disabled — clearing display")
            displayController.clear()
            appSettings.setLastEvaluatedAt(System.currentTimeMillis())
            return
        }

        // 2. Find the active profile (tracked in Room via CurveProfile.isActive)
        val profile = curveRepository.getActiveProfile()
        if (profile == null) {
            Log.d(TAG, "No active profile — clearing display")
            displayController.clear()
            appSettings.setLastEvaluatedAt(System.currentTimeMillis())
            return
        }

        // 3. Load points and evaluate
        val points = curveRepository.getPointsByProfileIdOnce(profile.id)
        val now = currentTimeMinutes()
        val state = curveEngine.calculateDisplayState(profile, points, now)

        Log.d(
            TAG,
            "Evaluated: time=$now (${"%02d:%02d".format(now / 60, now % 60)}), " +
                "warmth=${"%.2f".format(state.warmth)}, dimming=${"%.2f".format(state.dimming)}",
        )

        // 4. Apply to display
        displayController.apply(state)
        appSettings.setLastEvaluatedAt(System.currentTimeMillis())
    }

    private fun currentTimeMinutes(): Int {
        val cal = Calendar.getInstance()
        return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    }

    companion object {
        private const val TAG = "SchedulerWorker"
        private const val WORK_NAME_PERIODIC = "circadian_scheduler_periodic"
        private const val WORK_NAME_IMMEDIATE = "circadian_scheduler_immediate"

        /** Minimum interval for periodic work (WorkManager constraint). */
        private const val PERIODIC_INTERVAL_MINUTES = 15L

        // ── WorkManager helpers ────────────────────────────────────────────
        /**
         * Enqueues the repeating periodic work. Safe to call multiple times —
         * uses [ExistingPeriodicWorkPolicy.KEEP] to avoid duplicate schedules.
         */
        fun enqueuePeriodic(context: Context) {
            val request =
                PeriodicWorkRequestBuilder<SchedulerWorker>(
                    PERIODIC_INTERVAL_MINUTES,
                    TimeUnit.MINUTES,
                )
                    .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME_PERIODIC,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request,
                )

            Log.d(TAG, "Periodic work enqueued (${PERIODIC_INTERVAL_MINUTES} min interval)")
        }

        /**
         * Triggers an immediate one-shot evaluation.
         * Used on app launch, profile change, mode switch, and master toggle.
         * Uses [ExistingWorkPolicy.REPLACE] to debounce rapid successive calls.
         */
        fun triggerNow(context: Context) {
            val request =
                OneTimeWorkRequestBuilder<SchedulerWorker>()
                    .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    WORK_NAME_IMMEDIATE,
                    ExistingWorkPolicy.REPLACE,
                    request,
                )

            Log.d(TAG, "Immediate evaluation triggered")
        }

        /**
         * Cancels all scheduled work (both periodic and one-shot).
         */
        fun cancelAll(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME_PERIODIC)
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME_IMMEDIATE)
            Log.d(TAG, "All scheduled work cancelled")
        }
    }
}

