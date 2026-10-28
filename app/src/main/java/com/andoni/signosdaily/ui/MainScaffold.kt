package com.andoni.signosdaily.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.andoni.signosdaily.ads.BannerAd
import com.andoni.signosdaily.data.UserData
import com.andoni.signosdaily.ui.screens.LibraryScreen
import com.andoni.signosdaily.ui.screens.ProgressScreen
import com.andoni.signosdaily.ui.screens.QuizScreen
import com.andoni.signosdaily.ui.screens.SettingsScreen
import com.andoni.signosdaily.ui.screens.TodayScreen

private data class Tab(val route: String, val label: String, val icon: ImageVector, val selectedIcon: ImageVector)

@Suppress("DEPRECATION")
private val tabs = listOf(
    Tab("today", "Hoy", Icons.Outlined.WbSunny, Icons.Filled.WbSunny),
    Tab("quiz", "Repaso", Icons.Outlined.Quiz, Icons.Filled.Quiz),
    Tab("library", "Biblioteca", Icons.Outlined.MenuBook, Icons.Filled.MenuBook),
    Tab("progress", "Progreso", Icons.Outlined.EmojiEvents, Icons.Filled.EmojiEvents),
    Tab("settings", "Ajustes", Icons.Outlined.Settings, Icons.Filled.Settings),
)

@Composable
fun MainScaffold(vm: AppViewModel, user: UserData, startTab: String = "today") {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route

    fun go(route: String) = nav.navigate(route) {
        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }

    Scaffold(
        bottomBar = {
            Column {
                BannerAd(Modifier.padding(bottom = 4.dp))
                NavigationBar {
                    tabs.forEach { tab ->
                        val selected = current == tab.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = { go(tab.route) },
                            icon = { Icon(if (selected) tab.selectedIcon else tab.icon, null) },
                            label = { Text(tab.label, maxLines = 1) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding)) {
            NavHost(nav, startDestination = tabs.firstOrNull { it.route == startTab }?.route ?: "today") {
                composable("today") { TodayScreen(vm, user, onStartQuiz = { go("quiz") }) }
                composable("quiz") { QuizScreen(vm, user) }
                composable("library") { LibraryScreen(vm, user) }
                composable("progress") { ProgressScreen(user) }
                composable("settings") { SettingsScreen(vm, user) }
            }
        }
    }
}
