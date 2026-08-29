package com.circadiandisplay.system.nativemode

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.circadiandisplay.core.curve.DisplayController
import com.circadiandisplay.core.curve.DisplayState
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Applies warmth via Android's native night display (color temperature) system.
 *
 * Uses a layered strategy per [Decision 008](DECISIONS.md):
 * 1. **Primary**: [android.hardware.display.ColorDisplayManager] hidden APIs via reflection.
 * 2. **Fallback**: Direct writes to [Settings.Secure] night display keys.
 * 3. **Graceful degradation**: [isSupported] returns `false` below API 29 or when
 *    `WRITE_SECURE_SETTINGS` is not granted.
 *
 * ## Requirements
 *
 * - API 29+ (Android 10+)
 * - `WRITE_SECURE_SETTINGS` granted via ADB:
 *   `adb shell pm grant com.circadiandisplay.app android.permission.WRITE_SECURE_SETTINGS`
 *
 * ## Color temperature mapping
 *
 * Warmth 0.0 → 6500K (neutral daylight — no visible warming).
 * Warmth 1.0 → 1000K (deep amber — maximum warming).
 *
 * ## Limitations
 *
 * - Dimming is **not** applied in Native Mode. The native night display system
 *   controls only color temperature, not screen brightness or opacity.
 * - Hidden API access via reflection may break on future Android versions if
 *   Google further restricts non-SDK interfaces.
 * - Some manufacturer ROMs override or disable the night display system entirely.
 */
@Singleton
class NativeDisplayController @Inject constructor(
    @ApplicationContext private val context: Context,
) : DisplayController {

    /**
     * Lazily resolved [android.hardware.display.ColorDisplayManager] instance.
     * `null` on API < 29.
     */
    private val colorDisplayManager: Any? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                @Suppress("DEPRECATION")
                context.getSystemService("color_display")
            } catch (e: Exception) {
                Log.w(TAG, "ColorDisplayManager not available: ${e.message}")
                null
            }
        } else {
            null
        }
    }

    // ── DisplayController contract ────────────────────────────────────────

    override fun isSupported(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            Log.d(TAG, "Not supported: API ${Build.VERSION.SDK_INT} < 29")
            return false
        }
        val granted = context.checkSelfPermission(PERMISSION_WRITE_SECURE_SETTINGS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) {
            Log.d(TAG, "Not supported: WRITE_SECURE_SETTINGS not granted")
        }
        return granted
    }

    override fun apply(state: DisplayState) {
        if (!isSupported()) return

        val warmth = state.warmth.coerceIn(0f, 1f)
        val colorTemp = mapWarmthToColorTemperature(warmth)

        Log.d(
            TAG,
            "Applying: warmth=${"%.2f".format(warmth)} → ${colorTemp}K",
        )

        // 1. Disable auto mode so manual settings persist
        setNightDisplayAutoMode(false)

        // 2. Set the desired color temperature
        setNightDisplayColorTemperature(colorTemp)

        // 3. Activate night display
        setNightDisplayActivated(true)
    }

    override fun clear() {
        if (!isSupported()) return
        Log.d(TAG, "Clearing — deactivating night display")
        setNightDisplayActivated(false)
    }

    // ── Color temperature mapping ─────────────────────────────────────────

    /**
     * Maps normalized warmth `[0.0 .. 1.0]` to color temperature in Kelvin.
     *
     * - 0.0 → 6500K (neutral daylight)
     * - 1.0 → 1000K (deep amber, maximum warmth)
     */
    private fun mapWarmthToColorTemperature(warmth: Float): Int {
        val minTemp = 1000  // Maximum warmth
        val maxTemp = 6500  // Neutral (no warming)
        return (maxTemp - (maxTemp - minTemp) * warmth).toInt()
    }

    // ── Night display controls (reflection → Settings.Secure fallback) ────

    private fun setNightDisplayAutoMode(enabled: Boolean) {
        val success = tryReflection(
            "setNightDisplayAutoMode",
            arrayOf(Int::class.java),
            arrayOf(if (enabled) 1 else AUTO_MODE_DISABLED),
        )
        if (!success) {
            Settings.Secure.putInt(
                context.contentResolver,
                KEY_NIGHT_DISPLAY_AUTO_MODE,
                if (enabled) 1 else 0,
            )
        }
    }

    private fun setNightDisplayActivated(activated: Boolean) {
        val success = tryReflection(
            "setNightDisplayActivated",
            arrayOf(Boolean::class.java),
            arrayOf(activated),
        )
        if (!success) {
            Settings.Secure.putInt(
                context.contentResolver,
                KEY_NIGHT_DISPLAY_ACTIVATED,
                if (activated) 1 else 0,
            )
        }
    }

    private fun setNightDisplayColorTemperature(colorTemp: Int) {
        val success = tryReflection(
            "setNightDisplayColorTemperature",
            arrayOf(Int::class.java),
            arrayOf(colorTemp),
        )
        if (!success) {
            Settings.Secure.putInt(
                context.contentResolver,
                KEY_NIGHT_DISPLAY_COLOR_TEMPERATURE,
                colorTemp,
            )
        }
    }

    /**
     * Attempts to invoke a hidden method on [colorDisplayManager] via reflection.
     *
     * @return `true` if the reflection call succeeded, `false` otherwise.
     */
    private fun tryReflection(
        methodName: String,
        paramTypes: Array<Class<*>>,
        args: Array<Any>,
    ): Boolean {
        if (colorDisplayManager == null) return false
        return try {
            val method = colorDisplayManager!!.javaClass.getMethod(methodName, *paramTypes)
            method.invoke(colorDisplayManager, *args)
            true
        } catch (e: NoSuchMethodException) {
            Log.w(TAG, "Hidden API unavailable: $methodName — falling back to Settings.Secure")
            false
        } catch (e: Exception) {
            Log.w(TAG, "Reflection call failed: $methodName", e)
            false
        }
    }

    companion object {
        private const val TAG = "NativeDisplayCtrl"

        /** The signature-level permission required for native night display control. */
        private const val PERMISSION_WRITE_SECURE_SETTINGS = "android.permission.WRITE_SECURE_SETTINGS"

        // Settings.Secure keys for night display (undocumented but stable across AOSP).
        private const val KEY_NIGHT_DISPLAY_ACTIVATED = "night_display_activated"
        private const val KEY_NIGHT_DISPLAY_AUTO_MODE = "night_display_auto_mode"
        private const val KEY_NIGHT_DISPLAY_COLOR_TEMPERATURE = "night_display_color_temperature"

        /** Value that disables auto mode (0 = disabled, 1 = enabled). */
        private const val AUTO_MODE_DISABLED = 0
    }
}
