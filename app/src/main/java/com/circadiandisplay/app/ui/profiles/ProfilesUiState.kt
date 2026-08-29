package com.circadiandisplay.app.ui.profiles

import com.circadiandisplay.core.curve.CurveProfile

/**
 * Immutable UI state for the Profiles screen.
 *
 * @property isLoading True until the first database emission arrives.
 * @property profiles  All profiles, most recently created first.
 */
data class ProfilesUiState(
    val isLoading: Boolean = true,
    val profiles: List<CurveProfile> = emptyList(),
)
