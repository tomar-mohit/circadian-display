package com.circadiandisplay.app.ui.settings

import android.app.Application
import android.content.Context
import android.os.PowerManager
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.circadiandisplay.app.data.settings.AppSettings
import com.circadiandisplay.app.scheduler.SchedulerWorker
import com.circadiandisplay.core.curve.DisplayMode
import com.circadiandisplay.system.nativemode.NativeDisplayController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val application: Application,
    private val appSettings: AppSettings,
    private val nativeController: NativeDisplayController,
) : ViewModel() {

    private val overlayPermissionGranted = MutableStateFlow(readOverlayPermission())
    private val batteryOptimizationIgnored = MutableStateFlow(readBatteryOptimization())

    val uiState: StateFlow<SettingsUiState> = combine(
        appSettings.displayMode,
        overlayPermissionGranted,
        batteryOptimizationIgnored,
    ) { mode, overlay, battery ->
        SettingsUiState(
            displayMode = mode,
            nativeSupported = nativeController.isSupported(),
            overlayPermissionGranted = overlay,
            batteryOptimizationIgnored = battery,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(),
    )

    /**
     * Re-reads system permission state. Called by the UI whenever the screen
     * resumes (e.g., after the user returns from the system settings screen)
     * so the status rows stay in sync with reality.
     */
    fun refreshPermissions() {
        overlayPermissionGranted.value = readOverlayPermission()
        batteryOptimizationIgnored.value = readBatteryOptimization()
    }

    fun setDisplayMode(mode: DisplayMode) {
        viewModelScope.launch {
            appSettings.setDisplayMode(mode)
            // Immediately re-evaluate so the display updates without waiting
            // for the next periodic WorkManager cycle.
            SchedulerWorker.triggerNow(application)
        }
    }

    private fun readOverlayPermission(): Boolean =
        Settings.canDrawOverlays(application)

    private fun readBatteryOptimization(): Boolean {
        val powerManager = application.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(application.packageName)
    }
}
