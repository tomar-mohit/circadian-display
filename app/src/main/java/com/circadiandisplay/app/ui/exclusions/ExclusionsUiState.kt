package com.circadiandisplay.app.ui.exclusions

import androidx.compose.ui.graphics.ImageBitmap

/**
 * A single installed, launchable app shown in the Exclusions list.
 *
 * @property packageName Android package name (authoritative identifier).
 * @property displayName Human-readable label shown in the list.
 * @property icon        App icon, or `null` if it could not be loaded.
 */
data class InstalledAppUi(
    val packageName: String,
    val displayName: String,
    val icon: ImageBitmap?,
)

/**
 * Immutable UI state for the Exclusions screen.
 *
 * @property isLoading          True until the installed-app list has loaded.
 * @property installedApps      All launchable apps, alphabetically sorted.
 * @property excludedPackages   Set of currently excluded package names.
 * @property usageAccessGranted Whether Usage Access is granted; when false the
 *                              scheduler cannot detect the foreground app and
 *                              exclusions silently do not apply.
 */
data class ExclusionsUiState(
    val isLoading: Boolean = true,
    val installedApps: List<InstalledAppUi> = emptyList(),
    val excludedPackages: Set<String> = emptySet(),
    val usageAccessGranted: Boolean = false,
)
