package com.circadiandisplay.app.di

import android.util.Log
import com.circadiandisplay.app.data.settings.AppSettings
import com.circadiandisplay.core.curve.DisplayController
import com.circadiandisplay.core.curve.DisplayMode
import com.circadiandisplay.core.curve.DisplayState
import com.circadiandisplay.system.nativemode.NativeDisplayController
import com.circadiandisplay.system.overlay.OverlayDisplayController
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Runtime-delegating [DisplayController] that routes [apply] and [clear] calls
 * to the active implementation based on the user's [DisplayMode] preference.
 *
 * This avoids compile-time binding to a single controller and allows the user
 * to switch between [DisplayMode.OVERLAY] and [DisplayMode.NATIVE] without
 * restarting the app.
 *
 * [isSupported] delegates to the currently selected controller so the UI can
 * warn the user when Native Mode is unavailable (e.g., missing
 * `WRITE_SECURE_SETTINGS` permission).
 */
@Singleton
class CompositeDisplayController @Inject constructor(
    private val overlay: OverlayDisplayController,
    private val native: NativeDisplayController,
    private val appSettings: AppSettings,
) : DisplayController {

    /**
     * Resolve the active controller for the current call.
     * Uses [runBlocking] to bridge the suspend [AppSettings.getDisplayModeSnapshot]
     * into the non-suspend [DisplayController] contract.
     *
     * DataStore reads are in-memory after the first access, so this is fast.
     */
    private fun activeController(): DisplayController {
        val mode = runBlocking { appSettings.getDisplayModeSnapshot() }
        return when (mode) {
            DisplayMode.OVERLAY -> overlay
            DisplayMode.NATIVE -> native
        }
    }

    override fun apply(state: DisplayState) {
        val mode = runBlocking { appSettings.getDisplayModeSnapshot() }
        when (mode) {
            DisplayMode.OVERLAY -> {
                // Clear any leftover native night display so they don't combine
                native.clear()
                Log.d(TAG, "Dispatching apply() → OverlayDisplayController")
                overlay.apply(state)
            }
            DisplayMode.NATIVE -> {
                // Clear any lingering overlay layer
                overlay.clear()
                if (native.isSupported()) {
                    Log.d(TAG, "Dispatching apply() → NativeDisplayController")
                    native.apply(state)
                } else {
                    Log.w(TAG, "Native mode requested but not supported; falling back to Overlay")
                    overlay.apply(state)
                }
            }
        }
    }

    override fun clear() {
        Log.d(TAG, "Clearing both Overlay and Native controllers")
        overlay.clear()
        native.clear()
    }

    override fun isSupported(): Boolean {
        val controller = activeController()
        return controller.isSupported()
    }

    companion object {
        private const val TAG = "CompositeDisplayCtrl"
    }
}
