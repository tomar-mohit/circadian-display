package com.circadiandisplay.core.curve

/**
 * The mechanism by which screen adjustments are applied.
 *
 * [NATIVE]  — Uses Android [android.hardware.display.ColorDisplayManager] APIs (API 29+).
 *             Provides hardware-level color temperature shifting where supported.
 * [OVERLAY] — Uses a full-screen software overlay via [android.view.WindowManager].
 *             Works on all devices but requires [android.permission.SYSTEM_ALERT_WINDOW].
 */
enum class DisplayMode {
    NATIVE,
    OVERLAY,
}
