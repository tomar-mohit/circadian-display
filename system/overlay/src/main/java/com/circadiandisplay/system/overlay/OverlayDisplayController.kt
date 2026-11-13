package com.circadiandisplay.system.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PixelFormat
import android.hardware.input.InputManager
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
 * Applies warmth and dimming via a single full-screen software overlay.
 *
 * The overlay is one full-screen [View] that draws the dimming (black) and
 * warmth (amber) layers in a single pass. It sits on top of all other windows
 * but never consumes touch events.
 *
 * Requires [android.permission.SYSTEM_ALERT_WINDOW]. The caller (app module)
 * is responsible for checking and requesting this permission before calling
 * [apply].
 *
 * ## Why a single window?
 *
 * Android 12+ (API 31) blocks touches that pass through `FLAG_NOT_TOUCHABLE`
 * `TYPE_APPLICATION_OVERLAY` windows when the *combined* obscuring opacity of
 * the overlay windows exceeds
 * `InputManager.getMaximumObscuringOpacityForTouch()` (0.8 by default). Two
 * stacked overlay windows each at alpha 0.8 combine to 0.96 and are therefore
 * treated as occluding, which is what broke system navigation gestures
 * (swipe-up-to-home) and taps on apps beneath the overlay.
 *
 * Using a single window keeps the obscuring opacity at or below the
 * pass-through ceiling, so touches and gestures always reach the content
 * underneath.
 *
 * ## Color mapping
 *
 * Both layers are drawn into the same window, in the same visual order as the
 * previous two-window implementation:
 *
 * - **Dimming layer** (drawn first): black with effective opacity `dimming`.
 *   0.0 -> no effect, 1.0 -> full black.
 * - **Warmth layer** (drawn on top): amber (#FFB300) with effective opacity
 *   `warmth * MAX_WARMTH_ALPHA`.
 *
 * The window-level alpha is kept just below the system maximum obscuring
 * opacity (0.8 on API 31+, with a small safety margin). The *combined*
 * presented opacity of both layers is additionally clamped by
 * [OverlayOpacityPolicy], because the layers compound inside the window
 * (dimming 0.5 + warmth 0.5 presents a net opacity of 0.75, and higher
 * combinations exceed the ceiling outright). Some OEM input stacks (realme /
 * ColorOS on Android 16) block touches when the presented combined opacity of
 * an overlay window is above the ceiling, even though the platform itself
 * only measures the window-level alpha — issue #8.
 */
@Singleton
class OverlayDisplayController
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : DisplayController {
        private val windowManager: WindowManager =
            context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        private val mainHandler = Handler(Looper.getMainLooper())

        /**
         * Window-level alpha, fixed at creation.
         *
         * On API 31+ this is clamped to the system maximum obscuring opacity
         * for touch pass-through, with a small safety margin below it so
         * window-level comparisons with `>=` and OEMs that measure the
         * presented content (rather than the window alpha) still pass touches
         * through. On earlier releases there is no such limit, so the window
         * stays fully opaque.
         */
        private val windowAlpha: Float =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val max =
                    context.getSystemService(InputManager::class.java)
                        ?.maximumObscuringOpacityForTouch
                ((max ?: DEFAULT_MAX_OBSCURING_OPACITY).coerceIn(0f, 1f)) * TOUCH_CEILING_MARGIN
            } else {
                1f
            }

        private var overlayView: OverlayView? = null

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

            // Resolve the presented opacities of both layers so their combined
            // opacity never exceeds the pass-through ceiling (issue #8), then
            // divide by the window alpha to get the in-window drawing alphas
            // (a layer drawn at x/windowAlpha renders at exactly x on screen).
            val (presentedDimming, presentedWarmth) =
                OverlayOpacityPolicy.clamp(
                    dimming = dimming,
                    warmth = warmth * MAX_WARMTH_ALPHA,
                    ceiling = windowAlpha,
                )

            val view = getOrCreateOverlayView()
            view.dimmingAlpha = (presentedDimming / windowAlpha).coerceIn(0f, 1f)
            view.warmthAlpha = (presentedWarmth / windowAlpha).coerceIn(0f, 1f)
            view.invalidate()
        }

        private fun clearOnMainThread() {
            overlayView?.let { removeViewSafely(it) }
            overlayView = null
        }

        // ── Internals ─────────────────────────────────────────────────────────

        private fun getOrCreateOverlayView(): OverlayView = overlayView ?: createOverlayView().also { overlayView = it }

        private fun createOverlayView(): OverlayView {
            val view = OverlayView(context)

            val layoutType =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                }

            val params =
                WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    layoutType,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                        or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSLUCENT,
                )

            params.gravity = Gravity.TOP or Gravity.START
            params.x = 0
            params.y = 0
            params.alpha = windowAlpha
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                params.layoutInDisplayCutoutMode =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                    } else {
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                    }
            }

            windowManager.addView(view, params)
            return view
        }

        private fun removeViewSafely(view: View) {
            try {
                windowManager.removeView(view)
            } catch (_: IllegalArgumentException) {
            }
        }

        companion object {
            private const val TAG = "OverlayDisplayCtrl"

            /**
             * Maximum effective opacity of the warmth layer. At warmth 1.0 the
             * amber tint is strong but still translucent, keeping the screen
             * readable. A purely additive overlay cannot shift color temperature
             * like the native path, so this cap approximates a comfortable
             * maximum warm tint.
             */
            private const val MAX_WARMTH_ALPHA = 0.5f

            /** Amber tint RGB channels (#FFB300) for the warmth layer. */
            private const val AMBER_RED = 0xFF
            private const val AMBER_GREEN = 0xB3
            private const val AMBER_BLUE = 0x00

            /** Android 12+ default maximum obscuring opacity for touch pass-through. */
            private const val DEFAULT_MAX_OBSCURING_OPACITY = 0.8f

            /**
             * Safety margin applied to the touch pass-through ceiling. Keeps
             * the overlay strictly below the limit to tolerate OEM input
             * stacks that measure the presented content (issue #8) instead of
             * the window-level alpha, or that compare with `>=`.
             */
            private const val TOUCH_CEILING_MARGIN = 0.95f
        }

        /**
         * Single full-screen view that draws the dimming (black) and warmth
         * (amber) layers in one pass: black first, amber on top.
         *
         * The alpha values stored here are already window-alpha compensated and
         * include the warmth strength cap.
         */
        private class OverlayView(context: Context) : View(context) {
            /** Black layer alpha, 0..1. */
            var dimmingAlpha: Float = 0f

            /** Amber layer alpha, 0..1. */
            var warmthAlpha: Float = 0f

            override fun onDraw(canvas: Canvas) {
                super.onDraw(canvas)

                if (dimmingAlpha > 0f) {
                    canvas.drawColor(
                        Color.argb((dimmingAlpha * 255).toInt().coerceIn(0, 255), 0, 0, 0),
                    )
                }
                if (warmthAlpha > 0f) {
                    canvas.drawColor(
                        Color.argb(
                            (warmthAlpha * 255).toInt().coerceIn(0, 255),
                            AMBER_RED,
                            AMBER_GREEN,
                            AMBER_BLUE,
                        ),
                    )
                }
            }
        }
    }
