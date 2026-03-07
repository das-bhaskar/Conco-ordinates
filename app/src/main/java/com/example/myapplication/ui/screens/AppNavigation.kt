package com.example.myapplication.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.ui.components.CalendarScreen
import com.example.myapplication.ui.components.CalendarActions
import com.example.myapplication.ui.components.UserAccountState
import com.example.myapplication.ui.theme.ConcordiaMaroon
import com.example.myapplication.ui.viewmodel.CalendarViewModel

// ── Route definitions ─────────────────────────────────────────────────────────
// Sealed class keeps all route strings in one place — no magic strings scattered
// across the codebase, and adding a new screen is a single-file change.
sealed class Screen(val route: String) {
    object Map      : Screen("map")
    object Calendar : Screen("calendar")
}

/**
 * Root scaffold using Jetpack Navigation Component.
 *
 * Replaces the hardcoded `when(selectedTab)` pattern per architectural review:
 * - NavController owns navigation state (not the Activity, not a remember{} int)
 * - Deep-linking and back-stack are handled automatically by the NavHost
 * - Adding a new top-level screen requires only a new [Screen] route + composable {}
 *
 * [onNavigateToMap] is the Coordinator callback: CalendarScreen emits a
 * "navigate to building" intent → NavController switches to Map route →
 * MapsActivity executes the building code command. No ViewModel knows about
 * the other domain.
 */
/**
 * Bundles cross-screen navigation callbacks into one stable object (PR review).
 *
 * Grouping them ensures the NavHost always fires both [onNavigateToMap]
 * AND the NavController switch together — preventing state desync.
 */
data class NavigationActions(
    val onNavigateToMap: (buildingCode: String) -> Unit,
    val onConnectClick:  () -> Unit,
    val onSignOutClick:  () -> Unit
)

@Composable
fun AppNavigation(
    calendarViewModel: CalendarViewModel,
    userEmail:         String,
    navigationActions: NavigationActions,
    mapContent: @Composable () -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val tabs = listOf(
        Triple(Screen.Map,      "Map",      Icons.Default.Map),
        Triple(Screen.Calendar, "Schedule", Icons.Default.CalendarMonth)
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                tabs.forEach { (screen, label, icon) ->
                    NavigationBarItem(
                        selected = currentDestination?.hierarchy
                            ?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                // Pop back to start so tapping a tab never
                                // builds up a deep back stack
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState    = true
                            }
                        },
                        icon   = { Icon(icon, contentDescription = label) },
                        label  = { Text(label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ConcordiaMaroon,
                            selectedTextColor = ConcordiaMaroon,
                            indicatorColor    = ConcordiaMaroon.copy(alpha = 0.12f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            NavHost(
                navController    = navController,
                startDestination = Screen.Map.route
            ) {
                composable(Screen.Map.route) {
                    mapContent()
                }
                composable(Screen.Calendar.route) {
                    val calId = calendarViewModel.selectedCalendarId
                    CalendarScreen(
                        calendarState  = calendarViewModel.calendarState,
                        weekStartMs    = calendarViewModel.currentWeekStartMs,
                        weekEvents     = calendarViewModel.weekEvents,
                        isLoading      = calendarViewModel.weekViewLoading,
                        accountState   = UserAccountState(
                            isSignedIn = calId != null,
                            userEmail  = userEmail
                        ),
                        calendarActions = CalendarActions(
                            onConnectClick   = navigationActions.onConnectClick,
                            onSignOutClick   = navigationActions.onSignOutClick,
                            onCalendarPicked = calendarViewModel::onCalendarSelected,
                            onPreviousWeek   = { calId?.let { calendarViewModel.goToPreviousWeek(it) } },
                            onNextWeek       = { calId?.let { calendarViewModel.goToNextWeek(it) } },
                            // NavigationActions guarantees Coordinator callback + NavController
                            // switch always fire together — prevents state desync (PR review).
                            onNavigateToEvent = { buildingCode ->
                                navigationActions.onNavigateToMap(buildingCode)
                                navController.navigate(Screen.Map.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState    = true
                                }
                            }
                        )
                    )
                }
            }
        }
    }
}
