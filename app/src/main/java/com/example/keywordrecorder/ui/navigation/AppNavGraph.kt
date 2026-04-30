package com.example.keywordrecorder.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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

private val bottomNavRoutes = setOf("home", "recordings", "settings")

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            if (currentRoute in bottomNavRoutes) {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Rounded.Mic, contentDescription = "Home") },
                        label = { Text("Home") },
                        selected = currentRoute == "home",
                        onClick = {
                            navController.navigate("home") {
                                popUpTo("home") { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Rounded.Audiotrack, contentDescription = "Recordings") },
                        label = { Text("Recordings") },
                        selected = currentRoute == "recordings",
                        onClick = {
                            navController.navigate("recordings") {
                                popUpTo("home")
                                launchSingleTop = true
                            }
                        },
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Rounded.Tune, contentDescription = "Settings") },
                        label = { Text("Settings") },
                        selected = currentRoute == "settings",
                        onClick = {
                            navController.navigate("settings") {
                                popUpTo("home")
                                launchSingleTop = true
                            }
                        },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding),
        ) {
            composable("home") { HomeScreen() }
            composable("recordings") {
                RecordingsScreen(onOpenRecording = { id -> navController.navigate("detail/$id") })
            }
            composable(
                route = "detail/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { entry ->
                RecordingDetailScreen(
                    id = entry.arguments?.getLong("id") ?: -1,
                    onBack = { navController.popBackStack() },
                )
            }
            composable("settings") { SettingsScreen() }
        }
    }
}
