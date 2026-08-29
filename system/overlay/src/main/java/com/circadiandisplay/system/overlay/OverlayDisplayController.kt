package com.circadiandisplay.system.overlay

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import com.circadiandisplay.core.curve.DisplayController
import com.circadiandisplay.core.curve.DisplayState
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Applies warmth and dimming via a full-screen software overlay using [WindowManager].
 *
 * The overlay is a single full-screen [View] that sits on top of all other windows
 * but does not consume touch events. Warmth is mapped to an amber color filter;
 * dimming is mapped to the overlay's alpha (opacity).
 *
 * Requires [android.permission.SYSTEM_ALERT_WINDOW]. The caller (app module)
 * is responsible for checking and requesting this permission before calling [apply].
 *
 * ## Color mapping
 *
 * Warmth 0.0 → neutral transparent gray (no visible effect).
 * Warmth 1.0 → deep amber (#FFB300) at full intensity.
 * Intermediate values are linearly interpolated.
 *
 * Dimming does not change the overlay color — it only adjusts alpha.
 * Dimming 0.0 → overlay is fully transparent.
 * Dimming 1.0 → overlay is fully opaque at the current warmth color.
 */
@Singleton
class OverlayDisplayController @Inject constructor(
    @ApplicationContext private val context: Context,
) : DisplayController {

    private val windowManager: WindowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private val mainHandler = Handler(Looper.getMainLooper())

    private var overlayView: View? = null

    override fun apply(state: DisplayState) {
        if (!isSupported()) {
            Log.w(TAG, "SYSTEM_ALERT_WINDOW not granted — skipping overlay apply")
            return
        }
        mainHandler.post {
            applyOnMainThread(state)
        }
    }

    override fun clear() {
        mainHandler.post {
            clearOnMainThread()
        }
    }

    override fun isSupported(): Boolean = Settings.canDrawOverlays(context)

    // ── Main-thread operations ────────────────────────────────────────────

    private fun applyOnMainThread(state: DisplayState) {
        val warmth = state.warmth.coerceIn(0f, 1f)
        val dimming = state.dimming.coerceIn(0f, 1f)

        val color = lerpColor(NEUTRAL_COLOR, AMBER_COLOR, warmth)

        val view = getOrCreateOverlay()
        view.setBackgroundColor(color)
        view.alpha = dimming
    }

    private fun clearOnMainThread() {
        overlayView?.let { view ->
            try {
                windowManager.removeView(view)
            } catch (_: IllegalArgumentException) { }
        }
        overlayView = null
    }

    // ── Internals ─────────────────────────────────────────────────────────

    private fun getOrCreateOverlay(): View {
        overlayView?.let { return it }

        val view = View(context)
        view.setBackgroundColor(NEUTRAL_COLOR)
        view.alpha = 0f

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT,
        )

        params.gravity = Gravity.TOP or Gravity.START
        params.x = 0
        params.y = 0

        windowManager.addView(view, params)
        overlayView = view
        return view
    }

    private fun lerpColor(from: Int, to: Int, t: Float): Int {
        val r = Color.red(from) + ((Color.red(to) - Color.red(from)) * t).toInt()
        val g = Color.green(from) + ((Color.green(to) - Color.green(from)) * t).toInt()
        val b = Color.blue(from) + ((Color.blue(to) - Color.blue(from)) * t).toInt()
        return Color.rgb(r, g, b)
    }

    companion object {
        private const val TAG = "OverlayDisplayCtrl"

        /** Neutral transparency — used when warmth is 0.0. */
        private const val NEUTRAL_COLOR = 0x00FFFFFF.toInt()

        /** Deep amber at warmth 1.0. */
        private const val AMBER_COLOR = 0xFFFFB300.toInt()
    }
}
