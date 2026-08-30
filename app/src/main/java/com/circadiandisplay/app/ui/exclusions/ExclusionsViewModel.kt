package com.circadiandisplay.app.ui.exclusions

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.circadiandisplay.app.data.repository.ExclusionRepository
import com.circadiandisplay.app.foreground.ForegroundAppProvider
import com.circadiandisplay.app.scheduler.SchedulerWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ExclusionsViewModel @Inject constructor(
    private val application: Application,
    private val exclusionRepository: ExclusionRepository,
    private val foregroundAppProvider: ForegroundAppProvider,
) : ViewModel() {

    private val installedApps = MutableStateFlow<List<InstalledAppUi>>(emptyList())
    private val isLoadingApps = MutableStateFlow(true)
    private val usageAccessGranted = MutableStateFlow(foregroundAppProvider.isAvailable())

    val uiState: StateFlow<ExclusionsUiState> = combine(
        installedApps,
        exclusionRepository.observeExcludedPackages(),
        isLoadingApps,
        usageAccessGranted,
    ) { apps, excluded, loading, usageAccess ->
        ExclusionsUiState(
            isLoading = loading,
            installedApps = apps,
            excludedPackages = excluded,
            usageAccessGranted = usageAccess,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ExclusionsUiState(),
    )

    init {
        loadInstalledApps()
    }

    /**
     * Re-reads the Usage Access permission state. Called when the screen
     * resumes so the banner stays in sync after returning from system settings.
     */
    fun refreshUsageAccess() {
        usageAccessGranted.value = foregroundAppProvider.isAvailable()
    }

    fun toggleExclusion(app: InstalledAppUi) {
        viewModelScope.launch {
            if (exclusionRepository.isExcluded(app.packageName)) {
                exclusionRepository.removeExclusion(app.packageName)
            } else {
                exclusionRepository.addExclusion(app.packageName, app.displayName)
            }
            // Re-evaluate immediately so the display updates without waiting
            // for the next periodic WorkManager cycle.
            SchedulerWorker.triggerNow(application)
        }
    }

    private fun loadInstalledApps() {
        viewModelScope.launch {
            isLoadingApps.value = true
            val apps = withContext(Dispatchers.IO) { queryLaunchableApps(application) }
            installedApps.value = apps
            isLoadingApps.value = false
        }
    }
}

/**
 * Loads every launchable app (ACTION_MAIN + CATEGORY_LAUNCHER) with its label
 * and icon, deduplicated by package and sorted alphabetically.
 *
 * Runs off the main thread. The system app itself is excluded from the list
 * since suspending the overlay over our own UI would be pointless.
 */
private fun queryLaunchableApps(context: Context): List<InstalledAppUi> {
    val packageManager = context.packageManager
    val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

    return runCatching {
        packageManager.queryIntentActivities(launcherIntent, 0)
    }.getOrDefault(emptyList())
        .mapNotNull { resolveInfo ->
            val packageName = resolveInfo.activityInfo?.packageName ?: return@mapNotNull null
            if (packageName == context.packageName) return@mapNotNull null
            val label = resolveInfo.loadLabel(packageManager)?.toString() ?: packageName
            InstalledAppUi(
                packageName = packageName,
                displayName = label,
                icon = loadAppIcon(packageManager, packageName),
            )
        }
        .distinctBy { it.packageName }
        .sortedBy { it.displayName.lowercase() }
}

private fun loadAppIcon(packageManager: android.content.pm.PackageManager, packageName: String): ImageBitmap? =
    runCatching {
        packageManager.getApplicationIcon(packageName).toImageBitmap(ICON_SIZE_PX)
    }.getOrNull()

private fun Drawable.toImageBitmap(sizePx: Int): ImageBitmap {
    val source = when (this) {
        is BitmapDrawable -> bitmap
        else -> {
            val width = if (intrinsicWidth > 0) intrinsicWidth else sizePx
            val height = if (intrinsicHeight > 0) intrinsicHeight else sizePx
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bitmap ->
                val canvas = Canvas(bitmap)
                setBounds(0, 0, bitmap.width, bitmap.height)
                draw(canvas)
            }
        }
    }

    val scaled = if (source.width > sizePx || source.height > sizePx) {
        val scale = minOf(sizePx.toFloat() / source.width, sizePx.toFloat() / source.height)
        Bitmap.createScaledBitmap(
            source,
            (source.width * scale).toInt().coerceAtLeast(1),
            (source.height * scale).toInt().coerceAtLeast(1),
            true,
        )
    } else {
        source
    }

    return scaled.asImageBitmap()
}

private const val ICON_SIZE_PX = 96
