package com.example.myapplication.ui.models

import com.example.myapplication.logic.SearchResult

data class SearchUiState(
    val query: String = "",
    val results: List<SearchResult> = emptyList(),
    val isSearching: Boolean = false,
    val showSuggestions: Boolean = false
)