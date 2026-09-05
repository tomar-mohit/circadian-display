package com.circadiandisplay.app.foreground

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.Process
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Determines the current foreground app via [UsageStatsManager].
 *
 * Requires the "Usage Access" special permission (`PACKAGE_USAGE_STATS`),
 * granted by the user in Settings → Apps → Special access → Usage access.
 * Without it, [isAvailable] returns `false` and [currentForegroundPackage]
 * returns `null` (fail-open: exclusions do not apply).
 */
@Singleton
class UsageStatsForegroundAppProvider
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : ForegroundAppProvider {
        private val usageStatsManager: UsageStatsManager? =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager

        override fun isAvailable(): Boolean = usageStatsManager != null && hasUsageAccess()

        override fun currentForegroundPackage(): String? {
            if (!isAvailable()) return null
            return queryForegroundPackage()
        }

        private fun hasUsageAccess(): Boolean {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    appOps.unsafeCheckOpNoThrow(
                        AppOpsManager.OPSTR_GET_USAGE_STATS,
                        Process.myUid(),
                        context.packageName,
                    )
                } else {
                    @Suppress("DEPRECATION")
                    appOps.checkOpNoThrow(
                        AppOpsManager.OPSTR_GET_USAGE_STATS,
                        Process.myUid(),
                        context.packageName,
                    )
                }
            return mode == AppOpsManager.MODE_ALLOWED
        }

        /**
         * Returns the package of the most recent foreground event within
         * [FOREGROUND_WINDOW_MS]. `MOVE_TO_FOREGROUND` (API 21+, deprecated in
         * API 29) and `ACTIVITY_RESUMED` (API 29+) are both compile-time int
         * constants, so referencing them is safe on all supported API levels.
         */
        @Suppress("DEPRECATION")
        private fun queryForegroundPackage(): String? {
            val manager = usageStatsManager ?: return null
            val end = System.currentTimeMillis()
            val begin = end - FOREGROUND_WINDOW_MS

            val events =
                try {
                    manager.queryEvents(begin, end)
                } catch (_: Exception) {
                    return null
                }

            var foregroundPackage: String? = null
            var latestTimestamp = Long.MIN_VALUE
            val event = UsageEvents.Event()
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                val isForegroundEvent =
                    event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND ||
                        event.eventType == UsageEvents.Event.ACTIVITY_RESUMED
                if (isForegroundEvent && event.timeStamp >= latestTimestamp) {
                    latestTimestamp = event.timeStamp
                    foregroundPackage = event.packageName
                }
            }
            return foregroundPackage
        }

        companion object {
            private const val FOREGROUND_WINDOW_MS = 60_000L
        }
    }
