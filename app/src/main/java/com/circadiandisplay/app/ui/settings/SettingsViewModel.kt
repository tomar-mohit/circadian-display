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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SettingsEvent {
    data class ShowToast(val message: String) : SettingsEvent
}

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val application: Application,
        private val appSettings: AppSettings,
        private val nativeController: NativeDisplayController,
    ) : ViewModel() {
        private val overlayPermissionGranted = MutableStateFlow(readOverlayPermission())
        private val batteryOptimizationIgnored = MutableStateFlow(readBatteryOptimization())
        private val _events = MutableSharedFlow<SettingsEvent>(extraBufferCapacity = 1)
        val events = _events.asSharedFlow()

        val uiState: StateFlow<SettingsUiState> =
            combine(
                appSettings.displayMode,
                overlayPermissionGranted,
                batteryOptimizationIgnored,
            ) { mode, overlay, battery ->
                SettingsUiState(
                    displayMode = mode,
                    nativeSupported = nativeController.isSupported(),
                    nativeApiSupported = nativeController.isApiSupported(),
                    nativePermissionGranted = nativeController.isWriteSecureSettingsGranted(),
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
            val overlay = readOverlayPermission()
            val battery = readBatteryOptimization()
            overlayPermissionGranted.value = overlay
            batteryOptimizationIgnored.value = battery

            // If native mode was selected previously but permission is revoked,
            // revert setting to OVERLAY to avoid a false state.
            if (!nativeController.isSupported() && uiState.value.displayMode == DisplayMode.NATIVE) {
                viewModelScope.launch {
                    appSettings.setDisplayMode(DisplayMode.OVERLAY)
                    SchedulerWorker.triggerNow(application)
                }
            }
        }

        fun setDisplayMode(mode: DisplayMode) {
            viewModelScope.launch {
                if (mode == DisplayMode.NATIVE && !nativeController.isSupported()) {
                    val message =
                        if (!nativeController.isApiSupported()) {
                            "Native mode requires Android 10+ (API 29+). " +
                                "Your device is not supported - falling back to Overlay."
                        } else {
                            "Native mode is unavailable: WRITE_SECURE_SETTINGS not granted via ADB. " +
                                "Falling back to Overlay."
                        }
                    _events.emit(SettingsEvent.ShowToast(message))
                    appSettings.setDisplayMode(DisplayMode.OVERLAY)
                    SchedulerWorker.triggerNow(application)
                    return@launch
                }

                appSettings.setDisplayMode(mode)
                // Immediately re-evaluate so the display updates without waiting
                // for the next periodic WorkManager cycle.
                SchedulerWorker.triggerNow(application)
            }
        }

        private fun readOverlayPermission(): Boolean = Settings.canDrawOverlays(application)

        private fun readBatteryOptimization(): Boolean {
            val powerManager = application.getSystemService(Context.POWER_SERVICE) as PowerManager
            return powerManager.isIgnoringBatteryOptimizations(application.packageName)
        }
    }
