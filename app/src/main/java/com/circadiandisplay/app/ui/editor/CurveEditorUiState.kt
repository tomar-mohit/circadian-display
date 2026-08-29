package com.circadiandisplay.app.ui.editor

import com.circadiandisplay.core.curve.CurvePoint

/**
 * A single editable point in the Curve Editor draft.
 *
 * Uses temporary negative [id]s for points that have not yet been persisted,
 * so multiple new points can be distinguished for selection before saving.
 */
data class CurvePointDraft(
    val id: Long = 0,
    val timeMinutes: Int,
    val warmth: Float,
    val dimming: Float,
) {
    fun toDomain(profileId: Long): CurvePoint = CurvePoint(
        id = if (id > 0) id else 0,
        profileId = profileId,
        timeMinutes = timeMinutes,
        warmth = warmth,
        dimming = dimming,
    )
}

/**
 * Immutable UI state for the Curve Editor screen.
 *
 * @property isLoading          True while the profile + points load from Room.
 * @property profileId          The profile being edited.
 * @property profileName        Draft name (persisted only on save).
 * @property points             Draft points, not yet persisted.
 * @property selectedPointId    Which point the detail panel edits, if any.
 * @property hasUnsavedChanges  Whether the draft differs from the persisted curve.
 * @property isSaving           True while a save is in flight.
 */
data class CurveEditorUiState(
    val isLoading: Boolean = true,
    val profileId: Long = 0,
    val profileName: String = "",
    val points: List<CurvePointDraft> = emptyList(),
    val selectedPointId: Long? = null,
    val hasUnsavedChanges: Boolean = false,
    val isSaving: Boolean = false,
)
