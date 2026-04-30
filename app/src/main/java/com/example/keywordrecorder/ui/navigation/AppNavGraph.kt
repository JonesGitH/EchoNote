package com.example.keywordrecorder.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
import com.example.keywordrecorder.ui.theme.*

private sealed class Screen(val route: String, val label: String, val shortcut: String) {
    object Home      : Screen("home",      "HOME",      "H")
    object Recordings: Screen("recordings","RECORDINGS","R")
    object Settings  : Screen("settings",  "SETTINGS",  "S")
    object Detail    : Screen("detail/{id}","DETAIL",   "")
}

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val navBackStack by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStack?.destination

    val topLevelScreens = listOf(Screen.Home, Screen.Recordings, Screen.Settings)

    Scaffold(
        containerColor = TermBg,
        bottomBar = {
            TermNavBar(
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

@Composable
private fun TermNavBar(
    screens: List<Screen>,
    currentDestination: NavDestination?,
    onClick: (Screen) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(TermSurface)
    ) {
        HorizontalDivider(color = TermBorder, thickness = 1.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            screens.forEach { screen ->
                val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                val color = if (selected) TermCyan else TermTextDim
                val label = if (selected) "[${screen.label}]" else " ${screen.label} "

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { onClick(screen) }
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = label,
                        color = color,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                    if (selected) {
                        Spacer(modifier = Modifier.height(2.dp))
                        HorizontalDivider(
                            modifier = Modifier.width(40.dp),
                            color = TermCyan,
                            thickness = 1.dp
                        )
                    }
                }
            }
        }
    }
}
