package com.circadiandisplay.app.ui.dashboard

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.circadiandisplay.app.data.repository.CurveRepository
import com.circadiandisplay.app.data.settings.AppSettings
import com.circadiandisplay.app.scheduler.SchedulerWorker
import com.circadiandisplay.app.util.currentTimeMinutes
import com.circadiandisplay.core.curve.CurveEngine
import com.circadiandisplay.core.curve.DisplayController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val application: Application,
    private val curveRepository: CurveRepository,
    private val appSettings: AppSettings,
    private val curveEngine: CurveEngine,
    private val displayController: DisplayController,
) : ViewModel() {

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<DashboardUiState> =
        combine(
            appSettings.isEnabled,
            appSettings.displayMode,
            curveRepository.observeActiveProfile(),
        ) { isEnabled, displayMode, profile ->
            Triple(isEnabled, displayMode, profile)
        }.flatMapLatest { (isEnabled, displayMode, profile) ->
        if (profile != null) {
                curveRepository.getPointsByProfileId(profile.id).map { points ->
                    val now = currentTimeMinutes()
                    val displayState = curveEngine.calculateDisplayState(profile, points, now)
                    DashboardUiState(
                        isEnabled = isEnabled,
                        activeProfileName = profile.name,
                        activeProfileId = profile.id,
                        hasActiveProfile = true,
                        currentWarmth = displayState.warmth,
                        currentDimming = displayState.dimming,
                        activeMode = displayMode,
                        nativeSupported = displayController.isSupported(),
                        currentTimeMinutes = now,
                    )
                }
        } else {
                flowOf(
                    DashboardUiState(
                        isEnabled = isEnabled,
                        activeProfileName = null,
                        hasActiveProfile = false,
                        activeMode = displayMode,
                        nativeSupported = displayController.isSupported(),
                        currentTimeMinutes = currentTimeMinutes(),
                    )
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DashboardUiState(),
        )

    fun toggleEnabled() {
        viewModelScope.launch {
            val newState = !uiState.value.isEnabled
            appSettings.setEnabled(newState)
            // Immediately re-evaluate so the display updates without waiting
            // for the next periodic WorkManager cycle
            SchedulerWorker.triggerNow(application)
        }
    }
}

