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
 * Restores the schedule after device reboot.
 *
 * Registered in AndroidManifest.xml with the [android.permission.RECEIVE_BOOT_COMPLETED]
 * permission and the `BOOT_COMPLETED` intent filter.
 *
 * Re-enqueues the periodic WorkManager job and re-arms the exact-alarm
 * schedule (issue #7) — exact alarms are cleared by a reboot, and without
 * re-arming the first post-boot update would wait for the first (potentially
 * heavily deferred) WorkManager window.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    @Inject lateinit var schedulerAlarms: SchedulerAlarms

    // Catching broadly is intentional: a receiver must never crash the app;
    // the periodic WorkManager fallback recovers if re-arming fails.
    @Suppress("TooGenericExceptionCaught")
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        Log.d(TAG, "Boot completed — re-enqueuing scheduler")
        SchedulerWorker.enqueuePeriodic(context)

        val result = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                schedulerAlarms.scheduleNext()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to re-arm exact alarm schedule", e)
            } finally {
                result.finish()
            }
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
