package com.circadiandisplay.app.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Re-enqueues the periodic scheduler work after device reboot.
 *
 * Registered in AndroidManifest.xml with the [android.permission.RECEIVE_BOOT_COMPLETED]
 * permission and the `BOOT_COMPLETED` intent filter.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        Log.d(TAG, "Boot completed — re-enqueuing scheduler")
        SchedulerWorker.enqueuePeriodic(context)
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
