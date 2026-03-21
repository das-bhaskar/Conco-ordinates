package com.example.myapplication.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.myapplication.logic.SearchResult
import androidx.compose.material.icons.filled.Home

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampusSearchBar(
    query: String,
    results: List<SearchResult>,
    onQueryChange: (String) -> Unit,
    onResultClick: (SearchResult) -> Unit,
    modifier: Modifier = Modifier
) {
    var active by remember { mutableStateOf(false) }

    SearchBar(
        query = query,
        onQueryChange = onQueryChange,
        onSearch = { active = false },
        active = active,
        onActiveChange = { active = it },
        placeholder = { Text("Search buildings or addresses...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        modifier = modifier.fillMaxWidth().padding(16.dp),
        colors = SearchBarDefaults.colors(containerColor = Color.White)
    ) {
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(results) { result ->
                val title = when(result) {
                    is SearchResult.BuildingResult -> result.building.name
                    is SearchResult.CampusResult -> result.campus.name
                    is SearchResult.GoogleResult -> result.title
                    is SearchResult.CurrentLocation -> "Your position"
                    is SearchResult.Home -> "Home"
                    is SearchResult.IndoorRoomResult -> result.label
                }

                val icon = when(result) {
                    is SearchResult.BuildingResult -> Icons.Default.Business
                    is SearchResult.CampusResult -> Icons.Default.LocationOn
                    is SearchResult.GoogleResult -> Icons.Default.LocationOn
                    is SearchResult.CurrentLocation -> Icons.Default.MyLocation
                    is SearchResult.Home -> Icons.Default.Home
                    is SearchResult.IndoorRoomResult -> Icons.Default.LocationOn
                }

                val tintColor = if (result is SearchResult.CurrentLocation || result is SearchResult.Home) {
                    Color(0xFF4285F4) // Google Blue
                } else {
                    Color(0xFF912338) // Concordia Maroon
                }

                ListItem(
                    headlineContent = { Text(title) },
                    supportingContent = {
                        if(result is SearchResult.GoogleResult) Text(result.address)
                    },
                    leadingContent = {
                        Icon(icon, contentDescription = null, tint = tintColor)
                    },
                    modifier = Modifier.clickable {
                        onResultClick(result)
                        active = false
                    }
                )
            }
        }
    }
}