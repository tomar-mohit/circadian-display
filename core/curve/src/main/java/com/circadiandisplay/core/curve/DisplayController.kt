package com.circadiandisplay.core.curve

/**
 * Contract for applying screen adjustments to the physical display.
 *
 * Defined in `core/curve` so that `system/` modules can implement it
 * without creating a dependency on `core/scheduler` or any feature module.
 *
 * Implementations (in `system/` modules):
 * - `OverlayDisplayController` (system/overlay) -- full-screen software overlay.
 * - `NativeDisplayController` (system/native) -- Android `ColorDisplayManager`.
 *   See Decision 008 in DECISIONS.md for Native Mode API constraints.
 */
interface DisplayController {
    /**
     * Apply the given [DisplayState] to the physical screen.
     * Called by the scheduler on each evaluation cycle.
     */
    fun apply(state: DisplayState)

    /**
     * Remove all screen adjustments immediately.
     * Called when the app is disabled, the active profile is cleared,
     * or an excluded app enters the foreground.
     */
    fun clear()

    /**
     * Whether this controller can function on the current device.
     * For example, NativeDisplayController returns false below API 29.
     */
    fun isSupported(): Boolean
}
