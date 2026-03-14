package com.example.myapplication.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock

class AnalyticsRegistryTest {

    @Before
    fun setUp() {
        // Reset the provider to a known state before each test
        AnalyticsRegistry.setProvider(NoOpAnalyticsProvider)
    }

    @Test
    fun `default provider should be NoOpAnalyticsProvider`() {
        assertSame(NoOpAnalyticsProvider, AnalyticsRegistry.provider())
    }

    @Test
    fun `setProvider should update the provider`() {
        val mockProvider: AnalyticsProvider = mock()
        
        AnalyticsRegistry.setProvider(mockProvider)
        
        assertSame(mockProvider, AnalyticsRegistry.provider())
    }

    @Test
    fun `multiple setProvider calls should keep the last one`() {
        val firstMock: AnalyticsProvider = mock()
        val secondMock: AnalyticsProvider = mock()
        
        AnalyticsRegistry.setProvider(firstMock)
        AnalyticsRegistry.setProvider(secondMock)
        
        assertSame(secondMock, AnalyticsRegistry.provider())
    }
}
