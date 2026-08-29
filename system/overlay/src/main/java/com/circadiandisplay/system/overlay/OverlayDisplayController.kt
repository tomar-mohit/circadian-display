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
 * The overlay is two stacked full-screen [View]s that sit on top of all other
 * windows but do not consume touch events. One layer carries the warmth tint
 * (amber) and the other carries the dimming (black).
 *
 * Requires [android.permission.SYSTEM_ALERT_WINDOW]. The caller (app module)
 * is responsible for checking and requesting this permission before calling [apply].
 *
 * ## Color mapping
 *
 * Two independent full-screen layers are stacked:
 *
 * - **Dimming layer** (bottom): opaque black with alpha = [DisplayState.dimming].
 *   Dimming 0.0 → fully transparent (no effect), 1.0 → fully opaque black.
 * - **Warmth layer** (top): amber (#FFB300) with alpha scaled by
 *   [DisplayState.warmth] up to [MAX_WARMTH_ALPHA]. Warmth 0.0 → fully
 *   transparent (no effect), 1.0 → amber at maximum tint strength.
 *
 * Each layer's opacity is driven solely by its own value, so warmth and
 * dimming are fully independent: dimming darkens without washing out the
 * screen, and warmth tints without being suppressed by low dimming.
 *
 * ## Note on warmth strength
 *
 * A WindowManager overlay can only *add* color on top of the screen; it cannot
 * shift color temperature like Android's native night-display path. To keep the
 * screen readable at maximum warmth, the amber layer's opacity is capped at
 * [MAX_WARMTH_ALPHA].
 */
@Singleton
class OverlayDisplayController @Inject constructor(
    @ApplicationContext private val context: Context,
) : DisplayController {

    private val windowManager: WindowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private val mainHandler = Handler(Looper.getMainLooper())

    private var dimmingView: View? = null
    private var warmthView: View? = null

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

        val dimmingColor = Color.argb((dimming * 255).toInt(), 0, 0, 0)
        val warmthColor = Color.argb(
            (warmth * MAX_WARMTH_ALPHA * 255).toInt(),
            AMBER_RED,
            AMBER_GREEN,
            AMBER_BLUE,
        )

        getOrCreateDimmingView().setBackgroundColor(dimmingColor)
        getOrCreateWarmthView().setBackgroundColor(warmthColor)
    }

    private fun clearOnMainThread() {
        dimmingView?.let { removeViewSafely(it) }
        warmthView?.let { removeViewSafely(it) }
        dimmingView = null
        warmthView = null
    }

    // ── Internals ─────────────────────────────────────────────────────────

    private fun getOrCreateDimmingView(): View =
        dimmingView ?: createOverlayView().also { dimmingView = it }

    private fun getOrCreateWarmthView(): View =
        warmthView ?: createOverlayView().also { warmthView = it }

    private fun createOverlayView(): View {
        val view = View(context)

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
        return view
    }

    private fun removeViewSafely(view: View) {
        try {
            windowManager.removeView(view)
        } catch (_: IllegalArgumentException) { }
    }

    companion object {
        private const val TAG = "OverlayDisplayCtrl"

        /**
         * Maximum opacity of the warmth layer. At warmth 1.0 the amber tint is
         * strong but still translucent, keeping the screen readable. A purely
         * additive overlay cannot shift color temperature like the native path,
         * so this cap approximates a comfortable maximum warm tint.
         */
        private const val MAX_WARMTH_ALPHA = 0.5f

        /** Amber tint RGB channels (#FFB300) for the warmth layer. */
        private const val AMBER_RED = 0xFF
        private const val AMBER_GREEN = 0xB3
        private const val AMBER_BLUE = 0x00
    }
}
