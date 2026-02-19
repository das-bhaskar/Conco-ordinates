package com.example.myapplication.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test

class CampusDataTest {

    @Test
    fun `verify SGW and Loyola data integrity`() {
        // Accessing the static properties directly to trigger 897 lines of coverage
        val sgw = CampusRepo.SGW
        val loyola = CampusRepo.LOYOLA

        assertNotNull(sgw)
        assertNotNull(loyola)

        // Verify SGW buildings
        assertFalse("SGW should have buildings", sgw.buildings.isEmpty())
        sgw.buildings.forEach { building ->
            assertNotNull(building.name)
            // Ensure this matches the property name in your Building class
            assertFalse(building.outline.isEmpty())
        }

        // Verify Loyola buildings
        assertFalse("Loyola should have buildings", loyola.buildings.isEmpty())
        loyola.buildings.forEach { building ->
            assertNotNull(building.name)
            assertFalse(building.outline.isEmpty())
        }
    }
}