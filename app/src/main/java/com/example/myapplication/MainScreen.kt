package com.example.myapplication

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.myapplication.ui.theme.ConcordiaMaroon
import com.example.myapplication.ui.viewmodel.MapViewModel
import com.example.myapplication.ui.viewmodel.ShuttleViewModel
import com.google.android.gms.location.FusedLocationProviderClient

private data class BottomNavItem(
    val label:         String,
    val selectedIcon:  ImageVector,
    val unselectedIcon: ImageVector
)

private val NAV_ITEMS = listOf(
    BottomNavItem("Profile",  Icons.Filled.Person,        Icons.Outlined.Person),
    BottomNavItem("Map",      Icons.Filled.LocationOn,    Icons.Outlined.LocationOn),
    BottomNavItem("Schedule", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth)
)

private const val TAB_PROFILE  = 0
private const val TAB_MAP      = 1
private const val TAB_SCHEDULE = 2

@Composable
fun MainScreen(
    fusedLocationClient: FusedLocationProviderClient,
    mapViewModel:        MapViewModel,
    shuttleViewModel:    ShuttleViewModel
) {
    var selectedTab by remember { mutableStateOf(TAB_MAP) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = ConcordiaMaroon,
                contentColor   = Color.White
            ) {
                NAV_ITEMS.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick  = { selectedTab = index },
                        icon = {
                            Icon(
                                imageVector        = if (selectedTab == index) item.selectedIcon
                                                     else item.unselectedIcon,
                                contentDescription = item.label
                            )
                        },
                        label  = { Text(item.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor   = Color.White,
                            selectedTextColor   = Color.White,
                            unselectedIconColor = Color.White.copy(alpha = 0.6f),
                            unselectedTextColor = Color.White.copy(alpha = 0.6f),
                            indicatorColor      = Color.White.copy(alpha = 0.2f)
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        when (selectedTab) {
            TAB_PROFILE -> ProfilePlaceholderScreen(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
            TAB_MAP -> MapsScreen(
                fusedLocationClient = fusedLocationClient,
                mapViewModel        = mapViewModel,
                shuttleViewModel    = shuttleViewModel,
                bottomPadding       = paddingValues
            )
            TAB_SCHEDULE -> SchedulePlaceholderScreen(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        }
    }
}

@Composable
private fun ProfilePlaceholderScreen(modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.Box(
        modifier         = modifier,
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Text(
            text  = "Profile — coming soon",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Gray
        )
    }
}

@Composable
private fun SchedulePlaceholderScreen(modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.Box(
        modifier         = modifier,
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Text(
            text  = "Schedule — coming soon",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Gray
        )
    }
}
