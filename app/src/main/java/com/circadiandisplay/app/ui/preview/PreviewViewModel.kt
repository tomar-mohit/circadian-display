package com.circadiandisplay.app.ui.preview

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.circadiandisplay.app.data.repository.CurveRepository
import com.circadiandisplay.app.util.currentTimeMinutes
import com.circadiandisplay.core.curve.CurveEngine
import com.circadiandisplay.core.curve.DisplayState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class PreviewViewModel
    @Inject
    constructor(
        private val curveRepository: CurveRepository,
        private val curveEngine: CurveEngine,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val profileId: Long = savedStateHandle.get<Long>("profileId") ?: -1L

        private val _timeMinutes = MutableStateFlow(currentTimeMinutes())
        private val timeMinutes: StateFlow<Int> = _timeMinutes.asStateFlow()

        private val profileAndPoints =
            flow {
                val profile = curveRepository.getProfileById(profileId)
                val points = curveRepository.getPointsByProfileIdOnce(profileId)
                emit(profile to points)
            }

        val uiState: StateFlow<PreviewUiState> =
            profileAndPoints.combine(timeMinutes) { (profile, points), time ->
                val state =
                    if (profile != null) {
                        curveEngine.calculateDisplayState(profile, points, time)
                    } else {
                        DisplayState(0f, 0f)
                    }
                PreviewUiState(
                    isLoading = false,
                    profileName = profile?.name.orEmpty(),
                    timeMinutes = time,
                    currentTimeMinutes = currentTimeMinutes(),
                    displayState = state,
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = PreviewUiState(),
            )

        fun setTime(minutes: Int) {
            _timeMinutes.value = minutes.coerceIn(0, 1439)
        }
    }
