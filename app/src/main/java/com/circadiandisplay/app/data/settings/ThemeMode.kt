package com.circadiandisplay.app.data.settings

/**
 * How the app's own UI chooses between the light and dark colour schemes.
 *
 * This is **presentation only**. It has no effect on the display adjustments the
 * overlay and native controllers apply, and therefore never needs to trigger a
 * scheduler evaluation (see DECISIONS.md #010, trap 1).
 */
enum class ThemeMode {
    /** Follow the device's dark-mode setting. The default, including for existing installs. */
    SYSTEM,
    LIGHT,
    DARK,
    ;

    /**
     * Resolves this mode to a concrete choice.
     *
     * [SYSTEM] is resolved against the *system* setting, not a cached value, so a device-level
     * dark-mode switch is honoured live without the app restarting.
     */
    fun isDark(systemInDarkTheme: Boolean): Boolean =
        when (this) {
            SYSTEM -> systemInDarkTheme
            LIGHT -> false
            DARK -> true
        }

    companion object {
        /**
         * Used when the stored value is missing or unrecognised. A missing key must resolve to
         * [SYSTEM] so installs that predate the setting need no migration (DECISIONS.md #010).
         */
        val default = SYSTEM

        fun fromName(name: String?): ThemeMode = entries.firstOrNull { it.name == name } ?: default
    }
}
