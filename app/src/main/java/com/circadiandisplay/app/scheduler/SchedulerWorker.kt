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
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * Periodic background worker that evaluates the active curve profile
 * and dispatches the resulting [DisplayState] to the [DisplayController].
 *
 * Scheduled via WorkManager with a ~15-minute interval.
 * Re-enqueues after device reboot via [BootReceiver].
 *
 * WorkManager defers periodic work for hours under Doze (issue #7), so this
 * job doubles as a safety net: the precise, event-driven updates come from
 * [SchedulerAlarms] exact alarms at curve knee points. After every successful
 * evaluation the next exact alarm is re-armed here, which also covers UI
 * triggers (toggle, profile edit, mode switch) that run through this worker.
 */
@HiltWorker
class SchedulerWorker
    @AssistedInject
    constructor(
        @Assisted appContext: Context,
        @Assisted workerParams: WorkerParameters,
        private val curveEvaluator: CurveEvaluator,
        private val schedulerAlarms: SchedulerAlarms,
    ) : CoroutineWorker(appContext, workerParams) {
        @Suppress("TooGenericExceptionCaught")
        override suspend fun doWork(): Result {
            return try {
                // Re-arm the exact alarm after every evaluation so schedule
                // changes made via this worker (toggle, profile activation,
                // curve edits, mode switch) are picked up immediately.
                // scheduleNext() cancels itself when the display was cleared.
                curveEvaluator.evaluateAndApply()
                schedulerAlarms.scheduleNext()
                Result.success()
            } catch (e: Exception) {
                // Catching broadly here is intentional: WorkManager workers must
                // not crash; any failure should surface as a retry instead.
                Log.e(TAG, "Scheduler evaluation failed", e)
                Result.retry()
            }
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
