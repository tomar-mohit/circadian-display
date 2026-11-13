package com.circadiandisplay.app.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Fires the evaluation scheduled by [SchedulerAlarms] and re-anchors the
 * schedule after time/timezone changes (issue #7).
 *
 * Actions handled:
 * - [ACTION_EVALUATE]: the exact alarm fired — evaluate, then schedule the
 *   next knee.
 * - [Intent.ACTION_TIMEZONE_CHANGED] / `android.intent.action.TIME_SET`:
 *   knee times are anchored to midnight in the local zone, so re-schedule
 *   after any clock or timezone change. Both are protected system broadcasts
 *   and are on the implicit-broadcast exemption list, so manifest delivery is
 *   reliable.
 */
@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {
    @Inject lateinit var evaluator: CurveEvaluator

    @Inject lateinit var schedulerAlarms: SchedulerAlarms

    // Catching broadly is intentional: a receiver must never crash the app;
    // failures are logged and the next evaluation or the periodic WorkManager
    // fallback recovers.
    @Suppress("TooGenericExceptionCaught")
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val result = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                when (intent.action) {
                    ACTION_EVALUATE -> {
                        Log.d(TAG, "Exact alarm fired — evaluating")
                        evaluator.evaluateAndApply()
                        schedulerAlarms.scheduleNext()
                    }

                    Intent.ACTION_TIMEZONE_CHANGED, ACTION_TIME_SET -> {
                        Log.d(TAG, "Clock changed (${intent.action}) — re-anchoring schedule")
                        schedulerAlarms.scheduleNext()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Alarm handling failed", e)
            } finally {
                result.finish()
            }
        }
    }

    companion object {
        private const val TAG = "AlarmReceiver"
        const val ACTION_EVALUATE = "com.circadiandisplay.app.action.EVALUATE_ALARM"

        /** Not exposed as an [Intent] constant. */
        const val ACTION_TIME_SET = "android.intent.action.TIME_SET"
    }
}
