package com.circadiandisplay.app.ui

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.circadiandisplay.app.ui.dashboard.DashboardScreen
import com.circadiandisplay.app.ui.dashboard.DashboardViewModel
import com.circadiandisplay.app.ui.settings.SettingsScreen
import com.circadiandisplay.app.ui.settings.SettingsViewModel

/**
 * Top-level navigation graph.
 *
 * Currently two destinations: Dashboard (home) and Settings. Profiles and the
 * Curve Editor will be added as their own destinations in a later phase.
 */
object AppDestinations {
    const val DASHBOARD = "dashboard"
    const val SETTINGS = "settings"
}

@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = AppDestinations.DASHBOARD,
    ) {
        composable(AppDestinations.DASHBOARD) {
            val viewModel: DashboardViewModel = hiltViewModel()
            DashboardScreen(
                viewModel = viewModel,
                onNavigateToSettings = { navController.navigate(AppDestinations.SETTINGS) },
            )
        }
        composable(AppDestinations.SETTINGS) {
            val viewModel: SettingsViewModel = hiltViewModel()
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
            )
        }
    }
}
