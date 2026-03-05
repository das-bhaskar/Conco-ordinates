package com.example.myapplication.ui.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SearchUiStateTest {

    @Test
    fun `default state is empty and idle`() {
        val state = SearchUiState()
        assertEquals("", state.query)
        assertEquals(0, state.results.size)
        assertFalse(state.isSearching)
        assertFalse(state.showSuggestions)
    }
}
