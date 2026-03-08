package com.example.myapplication.ui.models

import com.example.myapplication.logic.SearchResult
import org.junit.Assert.*
import org.junit.Test

class SearchUiStateTest {

    @Test
    fun `initial state has correct default values`() {
        val state = SearchUiState()

        assertEquals("", state.query)
        assertTrue(state.results.isEmpty())
        assertFalse(state.isSearching)
        assertFalse(state.showSuggestions)
    }

    @Test
    fun `copy method updates specific fields while keeping others`() {
        val initialState = SearchUiState(query = "Hall", isSearching = true)

        // Update only the results
        val newState = initialState.copy(
            results = listOf(SearchResult.CurrentLocation),
            isSearching = false
        )

        assertEquals("Hall", newState.query) // Remains unchanged
        assertEquals(1, newState.results.size)
        assertFalse(newState.isSearching) // Updated
    }
}