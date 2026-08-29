package com.circadiandisplay.app.ui.settings

import com.circadiandisplay.core.curve.DisplayMode

/**
 * Immutable UI state for the Settings screen.
 *
 * Emitted via StateFlow from [SettingsViewModel].
 *
 * @property displayMode               The currently selected display mode.
 * @property nativeSupported           Whether Native Mode can function on this device
 *                                     (API 29+ and WRITE_SECURE_SETTINGS granted).
 * @property overlayPermissionGranted  Whether SYSTEM_ALERT_WINDOW is granted.
 * @property batteryOptimizationIgnored Whether the app is exempt from battery optimization.
 */
data class SettingsUiState(
    val displayMode: DisplayMode = DisplayMode.OVERLAY,
    val nativeSupported: Boolean = false,
    val overlayPermissionGranted: Boolean = false,
    val batteryOptimizationIgnored: Boolean = false,
)
