package com.example.myapplication.data.poi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class POIRepositoryAdditionalTest {

    @Test
    fun `default radius remains five hundred meters`() {
        assertEquals(500, POIRepository.DEFAULT_RADIUS)
    }

    @Test
    fun `poi exception stores message and cause`() {
        val cause = IllegalStateException("network")
        val exception = POIException("request failed", cause)

        assertEquals("request failed", exception.message)
        assertSame(cause, exception.cause)
    }
}
