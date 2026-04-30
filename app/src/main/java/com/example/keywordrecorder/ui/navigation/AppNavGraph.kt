package com.example.keywordrecorder.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
    object Detail : Screen("detail/{id}", "Detail")
}

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val navBackStack by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStack?.destination

    val topLevelRoutes = listOf(Screen.Home, Screen.Recordings, Screen.Settings)

    Scaffold(
        bottomBar = {
            NavigationBar {
                topLevelRoutes.forEach { screen ->
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
                                Screen.Home -> Icon(Icons.Default.Home, contentDescription = screen.label)
                                Screen.Recordings -> Icon(Icons.Default.List, contentDescription = screen.label)
                                Screen.Settings -> Icon(Icons.Default.Settings, contentDescription = screen.label)
                                else -> {}
                            }
                        },
                        label = { Text(screen.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) { HomeScreen() }
            composable(Screen.Recordings.route) {
                RecordingsScreen(onOpenDetail = { id -> navController.navigate("detail/$id") })
            }
            composable(
                route = Screen.Detail.route,
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { backStackEntry ->
                RecordingDetailScreen(recordingId = backStackEntry.arguments!!.getLong("id"))
            }
            composable(Screen.Settings.route) { SettingsScreen() }
        }
    }
}
