package com.example.myapplication.data.indoor

import org.junit.Assert.assertEquals
import org.junit.Test

class IndoorBuildingConfigDimsTest {

    @Test
    fun `dimsFor returns expected dimensions for known building`() {
        val dims = IndoorBuildingConfig.dimsFor("H")
        assertEquals(70f, dims.widthM)
        assertEquals(120f, dims.heightM)
    }

    @Test
    fun `dimsFor is case insensitive`() {
        assertEquals(IndoorBuildingConfig.dimsFor("CC"), IndoorBuildingConfig.dimsFor("cc"))
        assertEquals(IndoorBuildingConfig.dimsFor("Mb"), IndoorBuildingConfig.dimsFor("MB"))
    }

    @Test
    fun `dimsFor returns default dimensions for unknown building`() {
        val dims = IndoorBuildingConfig.dimsFor("UNKNOWN")
        assertEquals(60f, dims.widthM)
        assertEquals(80f, dims.heightM)
    }
}
