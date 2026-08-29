package com.circadiandisplay.app.ui.profiles

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.circadiandisplay.app.data.repository.CurveRepository
import com.circadiandisplay.app.scheduler.SchedulerWorker
import com.circadiandisplay.core.curve.CurveProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** One-shot navigation event emitted from [ProfilesViewModel]. */
sealed interface ProfilesEvent {
    data class OpenEditor(val profileId: Long) : ProfilesEvent
}

@HiltViewModel
class ProfilesViewModel @Inject constructor(
    private val application: Application,
    private val curveRepository: CurveRepository,
) : ViewModel() {

    val uiState: StateFlow<ProfilesUiState> =
        curveRepository.getAllProfiles()
            .map { ProfilesUiState(isLoading = false, profiles = it) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = ProfilesUiState(),
            )

    private val _events = MutableSharedFlow<ProfilesEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    /** Inserts a new empty profile and requests navigation to its editor. */
    fun createProfile(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val id = curveRepository.insertProfile(CurveProfile(name = trimmed))
            _events.emit(ProfilesEvent.OpenEditor(id))
        }
    }

    fun activateProfile(id: Long) {
        viewModelScope.launch {
            curveRepository.setActiveProfile(id)
            SchedulerWorker.triggerNow(application)
        }
    }

    fun deleteProfile(profile: CurveProfile) {
        viewModelScope.launch {
            curveRepository.deleteProfile(profile)
        }
    }
}
