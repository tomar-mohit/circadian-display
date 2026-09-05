package com.circadiandisplay.app.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.circadiandisplay.app.ui.dashboard.DashboardScreen
import com.circadiandisplay.app.ui.dashboard.DashboardViewModel
import com.circadiandisplay.app.ui.editor.CurveEditorScreen
import com.circadiandisplay.app.ui.editor.CurveEditorViewModel
import com.circadiandisplay.app.ui.exclusions.ExclusionsScreen
import com.circadiandisplay.app.ui.exclusions.ExclusionsViewModel
import com.circadiandisplay.app.ui.preview.PreviewScreen
import com.circadiandisplay.app.ui.preview.PreviewViewModel
import com.circadiandisplay.app.ui.profiles.ProfilesScreen
import com.circadiandisplay.app.ui.profiles.ProfilesViewModel
import com.circadiandisplay.app.ui.settings.SettingsScreen
import com.circadiandisplay.app.ui.settings.SettingsViewModel

/**
 * Top-level navigation graph.
 *
 * Three primary destinations (Dashboard, Profiles, Settings) are reachable via
 * a bottom navigation bar. The Curve Editor and Preview are pushed on top of
 * those and hide the bottom bar.
 */
@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        // The inner screens own their own top app bars (and status-bar insets),
        // so the outer scaffold must not add another top inset.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (currentRoute in AppDestinations.TOP_LEVEL) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute == AppDestinations.DASHBOARD,
                        onClick = {
                            navController.navigate(AppDestinations.DASHBOARD) {
                                popUpTo(AppDestinations.DASHBOARD)
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Filled.Home, contentDescription = "Dashboard") },
                        label = { Text("Dashboard") },
                    )
                    NavigationBarItem(
                        selected = currentRoute == AppDestinations.PROFILES,
                        onClick = {
                            navController.navigate(AppDestinations.PROFILES) {
                                popUpTo(AppDestinations.DASHBOARD)
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Profiles") },
                        label = { Text("Profiles") },
                    )
                    NavigationBarItem(
                        selected = currentRoute == AppDestinations.SETTINGS,
                        onClick = {
                            navController.navigate(AppDestinations.SETTINGS) {
                                popUpTo(AppDestinations.DASHBOARD)
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Filled.Settings, contentDescription = "Settings") },
                        label = { Text("Settings") },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppDestinations.DASHBOARD,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(AppDestinations.DASHBOARD) {
                val viewModel: DashboardViewModel = hiltViewModel()
                DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToPreview = { profileId ->
                        navController.navigate(AppDestinations.preview(profileId))
                    },
                )
            }
            composable(AppDestinations.PROFILES) {
                val viewModel: ProfilesViewModel = hiltViewModel()
                ProfilesScreen(
                    viewModel = viewModel,
                    onOpenEditor = { profileId ->
                        navController.navigate(AppDestinations.curveEditor(profileId))
                    },
                )
            }
            composable(AppDestinations.SETTINGS) {
                val viewModel: SettingsViewModel = hiltViewModel()
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToExclusions = {
                        navController.navigate(AppDestinations.EXCLUSIONS)
                    },
                )
            }
            composable(AppDestinations.EXCLUSIONS) {
                val viewModel: ExclusionsViewModel = hiltViewModel()
                ExclusionsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                )
            }
            composable(
                route = AppDestinations.CURVE_EDITOR,
                arguments = listOf(navArgument("profileId") { type = NavType.LongType }),
            ) {
                val viewModel: CurveEditorViewModel = hiltViewModel()
                CurveEditorScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                )
            }
            composable(
                route = AppDestinations.PREVIEW,
                arguments = listOf(navArgument("profileId") { type = NavType.LongType }),
            ) {
                val viewModel: PreviewViewModel = hiltViewModel()
                PreviewScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                )
            }
        }
    }
}
