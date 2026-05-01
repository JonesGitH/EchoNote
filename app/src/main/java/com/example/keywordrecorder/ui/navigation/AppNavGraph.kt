package com.example.keywordrecorder.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
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
import com.example.keywordrecorder.ui.theme.EchoAccent
import com.example.keywordrecorder.ui.theme.EchoBg
import com.example.keywordrecorder.ui.theme.EchoBorder
import com.example.keywordrecorder.ui.theme.EchoSurface
import com.example.keywordrecorder.ui.theme.EchoTextSecondary

private sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Home       : Screen("home",       "Notes",       Icons.Default.Home)
    object Recordings : Screen("recordings", "Recordings",  Icons.Default.List)
    object Settings   : Screen("settings",   "Settings",    Icons.Default.Settings)
    object Detail     : Screen("detail/{id}", "Detail",     Icons.Default.Home)
}

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val navBackStack by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStack?.destination

    val topLevelScreens = listOf(Screen.Home, Screen.Recordings, Screen.Settings)
    val showBottomBar = currentDestination?.route != Screen.Detail.route

    Scaffold(
        containerColor = EchoBg,
        bottomBar = {
            if (showBottomBar) {
                EchoNavBar(
                    screens = topLevelScreens,
                    currentDestination = currentDestination,
                    onClick = { screen ->
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
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
                RecordingDetailScreen(
                    recordingId = backStackEntry.arguments!!.getLong("id"),
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Settings.route) { SettingsScreen() }
        }
    }
}

@Composable
private fun EchoNavBar(
    screens: List<Screen>,
    currentDestination: NavDestination?,
    onClick: (Screen) -> Unit
) {
    NavigationBar(
        containerColor = EchoSurface,
        tonalElevation = 0.dp
    ) {
        screens.forEach { screen ->
            val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
            NavigationBarItem(
                selected = selected,
                onClick = { onClick(screen) },
                icon = {
                    Icon(
                        imageVector = screen.icon,
                        contentDescription = screen.label,
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = {
                    Text(screen.label, style = MaterialTheme.typography.labelSmall)
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = EchoAccent,
                    selectedTextColor = EchoAccent,
                    unselectedIconColor = EchoTextSecondary,
                    unselectedTextColor = EchoTextSecondary,
                    indicatorColor = EchoSurface
                )
            )
        }
    }
}
