package com.circadiandisplay.app.foreground

/**
 * Provides the package name of the app currently in the foreground.
 *
 * Used by the scheduler to decide whether an excluded app is open and the
 * display adjustments should be suspended.
 *
 * Implementations are Android-framework dependent (e.g. `UsageStatsManager`),
 * so the interface exists to keep the scheduler testable with fakes.
 */
interface ForegroundAppProvider {
    /**
     * The package name of the current foreground app, or `null` if it cannot
     * be determined (e.g. Usage Access not granted).
     */
    fun currentForegroundPackage(): String?

    /**
     * Whether the provider can actually determine the foreground app on this
     * device. When `false`, exclusions silently do not apply (fail-open).
     */
    fun isAvailable(): Boolean
}
