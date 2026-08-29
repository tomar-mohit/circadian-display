package com.circadiandisplay.app.ui.preview

import com.circadiandisplay.core.curve.DisplayState

/**
 * Immutable UI state for the Preview screen.
 *
 * @property isLoading         True while the profile + points load from Room.
 * @property profileName       Name of the profile being previewed.
 * @property timeMinutes       The simulated time being scrubbed (0–1439).
 * @property currentTimeMinutes The actual wall-clock time, shown for reference.
 * @property displayState      Interpolated warmth/dimming at [timeMinutes].
 */
data class PreviewUiState(
    val isLoading: Boolean = true,
    val profileName: String = "",
    val timeMinutes: Int = 0,
    val currentTimeMinutes: Int = 0,
    val displayState: DisplayState = DisplayState(0f, 0f),
)
