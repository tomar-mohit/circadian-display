package com.circadiandisplay.app.scheduler

import android.util.Log
import com.circadiandisplay.app.data.repository.CurveRepository
import com.circadiandisplay.app.data.repository.ExclusionRepository
import com.circadiandisplay.app.data.settings.AppSettings
import com.circadiandisplay.app.foreground.ForegroundAppProvider
import com.circadiandisplay.core.curve.CurveEngine
import com.circadiandisplay.core.curve.DisplayController
import com.circadiandisplay.core.curve.DisplayState
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Evaluates the active curve profile against the current time and applies the
 * resulting [DisplayState] to the [DisplayController].
 *
 * Shared by [SchedulerWorker] (periodic polling), [AlarmReceiver] (exact
 * alarms at curve knee points, issue #7), and the immediate one-shot work
 * triggered from the UI.
 *
 * ## Edge cases handled
 *
 * - **Disabled**: Clears the display and returns false.
 * - **Excluded app in foreground**: Clears the display and returns false.
 * - **No active profile**: Clears the display and returns false.
 * - **Empty profile**: CurveEngine returns safe default (0, 0).
 *
 * @return true when a display state was applied, false when the display was
 *   cleared (disabled / excluded / no profile). Callers use this to decide
 *   whether to keep scheduling future exact evaluations.
 */
@Singleton
class CurveEvaluator
    @Inject
    constructor(
        private val curveRepository: CurveRepository,
        private val appSettings: AppSettings,
        private val curveEngine: CurveEngine,
        private val displayController: DisplayController,
        private val exclusionRepository: ExclusionRepository,
        private val foregroundAppProvider: ForegroundAppProvider,
    ) {
        @Suppress("TooGenericExceptionCaught")
        suspend fun evaluateAndApply(): Boolean {
            // 1. Check if the master toggle is on
            val enabled = appSettings.isEnabledSnapshot()
            if (!enabled) {
                Log.d(TAG, "Disabled — clearing display")
                displayController.clear()
                appSettings.setLastEvaluatedAt(System.currentTimeMillis())
                return false
            }

            // 2. If an excluded app is in the foreground, suspend adjustments.
            //    Without Usage Access, currentForegroundPackage() returns null and
            //    exclusions fail open (the display keeps updating as normal).
            val foregroundPackage = foregroundAppProvider.currentForegroundPackage()
            if (foregroundPackage != null && exclusionRepository.isExcluded(foregroundPackage)) {
                Log.d(TAG, "Excluded app in foreground ($foregroundPackage) — clearing display")
                displayController.clear()
                appSettings.setLastEvaluatedAt(System.currentTimeMillis())
                return false
            }

            // 3. Find the active profile (tracked in Room via CurveProfile.isActive)
            val profile = curveRepository.getActiveProfile()
            if (profile == null) {
                Log.d(TAG, "No active profile — clearing display")
                displayController.clear()
                appSettings.setLastEvaluatedAt(System.currentTimeMillis())
                return false
            }

            // 4. Load points and evaluate
            val points = curveRepository.getPointsByProfileIdOnce(profile.id)
            val now = currentTimeMinutes()
            val state = curveEngine.calculateDisplayState(profile, points, now)

            Log.d(
                TAG,
                "Evaluated: time=$now (${"%02d:%02d".format(now / 60, now % 60)}), " +
                    "warmth=${"%.2f".format(state.warmth)}, dimming=${"%.2f".format(state.dimming)}",
            )

            // 5. Apply to display
            displayController.apply(state)
            appSettings.setLastEvaluatedAt(System.currentTimeMillis())
            return true
        }

        private fun currentTimeMinutes(): Int {
            val cal = Calendar.getInstance()
            return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        }

        private companion object {
            const val TAG = "CurveEvaluator"
        }
    }
