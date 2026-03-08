package com.example.myapplication.data

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class BuildingNameProviderTest {

    private lateinit var provider: CampusBuildingNameProvider

    @Before
    fun setup() {
        // Ensure CampusRepo has data.
        // In a real project, you might need to mock CampusRepo if you refactor
        // it to be non-static, but for now, we ensure it's initialized.
        provider = CampusBuildingNameProvider()
    }



    @Test
    fun `nameForCode returns null for non-existent code`() {
        val result = provider.nameForCode("NON_EXISTENT_CODE")

        assertNull("Should return null if the code does not exist in any campus", result)
    }

    @Test
    fun `nameForCode handles empty string gracefully`() {
        val result = provider.nameForCode("")

        assertNull("Should return null for an empty string code", result)
    }
}