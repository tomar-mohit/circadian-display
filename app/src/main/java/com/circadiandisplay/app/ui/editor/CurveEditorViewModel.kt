package com.circadiandisplay.app.ui.editor

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.circadiandisplay.app.data.repository.CurveRepository
import com.circadiandisplay.app.scheduler.SchedulerWorker
import com.circadiandisplay.core.curve.CurveProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** One-shot message emitted from [CurveEditorViewModel] (e.g., snackbar text). */
sealed interface EditorEvent {
    data object Saved : EditorEvent

    data class Error(val message: String) : EditorEvent
}

@HiltViewModel
class CurveEditorViewModel
    @Inject
    constructor(
        private val application: Application,
        private val curveRepository: CurveRepository,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val profileId: Long = savedStateHandle.get<Long>("profileId") ?: -1L
        private var originalProfile: CurveProfile? = null
        private var nextTempId = -1L

        private val _uiState = MutableStateFlow(CurveEditorUiState(profileId = profileId))
        val uiState: StateFlow<CurveEditorUiState> = _uiState.asStateFlow()

        private val _events = MutableSharedFlow<EditorEvent>(extraBufferCapacity = 1)
        val events = _events.asSharedFlow()

        init {
            viewModelScope.launch {
                val profile = curveRepository.getProfileById(profileId)
                val points = curveRepository.getPointsByProfileIdOnce(profileId)
                originalProfile = profile
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        profileName = profile?.name.orEmpty(),
                        points =
                            points.map { p ->
                                CurvePointDraft(p.id, p.timeMinutes, p.warmth, p.dimming)
                            },
                        selectedPointId = points.maxByOrNull { p -> p.timeMinutes }?.id,
                    )
                }
            }
        }

        fun setProfileName(name: String) {
            _uiState.update { it.copy(profileName = name, hasUnsavedChanges = true) }
        }

        fun selectPoint(id: Long?) {
            _uiState.update { it.copy(selectedPointId = id) }
        }

        fun addPoint(timeMinutes: Int) {
            val id = nextTempId--
            _uiState.update { state ->
                val point =
                    CurvePointDraft(
                        id = id,
                        timeMinutes = timeMinutes,
                        warmth = 0.5f,
                        dimming = 0.2f,
                    )
                state.copy(
                    points = (state.points + point).sortedBy { it.timeMinutes },
                    selectedPointId = id,
                    hasUnsavedChanges = true,
                )
            }
        }

        fun updatePoint(
            id: Long,
            timeMinutes: Int,
            warmth: Float,
            dimming: Float,
        ) {
            val clampedTime = timeMinutes.coerceIn(0, 1439)
            val clampedWarmth = warmth.coerceIn(0f, 1f)
            val clampedDimming = dimming.coerceIn(0f, 1f)
            _uiState.update { state ->
                state.copy(
                    points =
                        state.points.map {
                            if (it.id == id) {
                                it.copy(
                                    timeMinutes = clampedTime,
                                    warmth = clampedWarmth,
                                    dimming = clampedDimming,
                                )
                            } else {
                                it
                            }
                        }.sortedBy { it.timeMinutes },
                    hasUnsavedChanges = true,
                )
            }
        }

        fun movePointTime(
            id: Long,
            timeMinutes: Int,
        ) {
            val clampedTime = timeMinutes.coerceIn(0, 1439)
            _uiState.update { state ->
                state.copy(
                    points =
                        state.points.map {
                            if (it.id == id) it.copy(timeMinutes = clampedTime) else it
                        }.sortedBy { it.timeMinutes },
                    hasUnsavedChanges = true,
                )
            }
        }

        fun deletePoint(id: Long) {
            _uiState.update { state ->
                val remaining = state.points.filter { it.id != id }
                state.copy(
                    points = remaining,
                    selectedPointId =
                        if (state.selectedPointId == id) {
                            remaining.maxByOrNull { it.timeMinutes }?.id
                        } else {
                            state.selectedPointId
                        },
                    hasUnsavedChanges = true,
                )
            }
        }

        fun save() {
            val state = _uiState.value
            val name = state.profileName.trim()
            if (name.isEmpty()) {
                viewModelScope.launch {
                    _events.emit(EditorEvent.Error("Profile name must not be blank"))
                }
                return
            }
            if (state.isSaving) return

            viewModelScope.launch {
                _uiState.update { it.copy(isSaving = true) }
                val profile = originalProfile ?: CurveProfile(id = profileId, name = name)
                val updated = profile.copy(name = name)
                curveRepository.updateProfile(updated)
                curveRepository.replacePoints(profileId, state.points.map { it.toDomain(profileId) })
                originalProfile = updated
                _uiState.update {
                    it.copy(isSaving = false, hasUnsavedChanges = false)
                }
                // Re-evaluate immediately so the display reflects the new curve.
                SchedulerWorker.triggerNow(application)
                _events.emit(EditorEvent.Saved)
            }
        }
    }
