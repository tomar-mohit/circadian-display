package com.circadiandisplay.app.ui

/**
 * Top-level navigation destinations.
 *
 * Three primary destinations (Dashboard, Profiles, Settings) are reachable via
 * a bottom navigation bar. The Curve Editor and Preview are pushed on top of
 * those and hide the bottom bar.
 */
object AppDestinations {
    const val DASHBOARD = "dashboard"
    const val PROFILES = "profiles"
    const val SETTINGS = "settings"
    const val EXCLUSIONS = "exclusions"
    const val CURVE_EDITOR = "curve_editor/{profileId}"
    const val PREVIEW = "preview/{profileId}"

    fun curveEditor(profileId: Long) = "curve_editor/$profileId"

    fun preview(profileId: Long) = "preview/$profileId"

    val TOP_LEVEL = setOf(DASHBOARD, PROFILES, SETTINGS)
}
