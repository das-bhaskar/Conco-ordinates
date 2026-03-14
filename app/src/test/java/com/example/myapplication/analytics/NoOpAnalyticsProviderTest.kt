package com.example.myapplication.analytics

import android.content.Context
import org.junit.Test
import org.mockito.kotlin.mock

class NoOpAnalyticsProviderTest {

    @Test
    fun `initialize should not throw exceptions`() {
        val mockContext: Context = mock()
        NoOpAnalyticsProvider.initialize(mockContext, "some_key")
        NoOpAnalyticsProvider.initialize(mockContext, "")
    }

    @Test
    fun `trackNavigationEnter should not throw exceptions`() {
        NoOpAnalyticsProvider.trackNavigationEnter("some_source")
        NoOpAnalyticsProvider.trackNavigationEnter("")
    }

    @Test
    fun `trackScreenView should not throw exceptions`() {
        NoOpAnalyticsProvider.trackScreenView("some_screen")
        NoOpAnalyticsProvider.trackScreenView("")
    }
}
