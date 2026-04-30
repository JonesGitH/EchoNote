package com.example.keywordrecorder.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.keywordrecorder.ui.detail.RecordingDetailScreen
import com.example.keywordrecorder.ui.home.HomeScreen
import com.example.keywordrecorder.ui.recordings.RecordingsScreen
import com.example.keywordrecorder.ui.settings.SettingsScreen

private sealed class Screen(val route: String, val label: String) {
    object Home : Screen("home", "Home")
    object Recordings : Screen("recordings", "Recordings")
    object Settings : Screen("settings", "Settings")
}

private val bottomScreens = listOf(Screen.Home, Screen.Recordings, Screen.Settings)

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    val showBottomBar = bottomScreens.any { screen ->
        currentDestination?.hierarchy?.any { it.route == screen.route } == true
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomScreens.forEach { screen ->
                        val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                when (screen) {
                                    Screen.Home -> Icon(Icons.Default.Home, contentDescription = null)
                                    Screen.Recordings -> Icon(Icons.Default.List, contentDescription = null)
                                    Screen.Settings -> Icon(Icons.Default.Settings, contentDescription = null)
                                }
                            },
                            label = { Text(screen.label) },
                        )
                    }
                }
            }
        }
    ) { _ ->
        NavHost(navController = navController, startDestination = Screen.Home.route) {
            composable(Screen.Home.route) { HomeScreen() }

            composable(Screen.Recordings.route) {
                RecordingsScreen(onRecordingClick = { id ->
                    navController.navigate("detail/$id")
                })
            }

            composable(
                route = "detail/{recordingId}",
                arguments = listOf(navArgument("recordingId") { type = NavType.LongType }),
            ) {
                RecordingDetailScreen(onBack = { navController.popBackStack() })
            }

            composable(Screen.Settings.route) { SettingsScreen() }
        }
    }
}
