package com.circadiandisplay.app.ui.dashboard

import com.circadiandisplay.core.curve.DisplayMode

/**
 * Immutable UI state for the Dashboard screen.
 *
 * Emitted via StateFlow from [DashboardViewModel].
 */
data class DashboardUiState(
    val isEnabled: Boolean = true,
    val activeProfileName: String? = null,
    val hasActiveProfile: Boolean = false,
    val currentWarmth: Float = 0f,
    val currentDimming: Float = 0f,
    val activeMode: DisplayMode = DisplayMode.OVERLAY,
    val nativeSupported: Boolean = false,
    val lastEvaluatedAt: Long = 0L,
    val currentTimeMinutes: Int = 0,
)

