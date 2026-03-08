package com.example.myapplication.logic

import android.content.Context
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock

class CalendarProviderTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var provider: GoogleCalendarProvider
    private val mockContext: Context = mock()

    // Test Dispatcher to keep everything on one thread
    private val testDispatchers = object : DispatcherProvider {
        override val main = kotlinx.coroutines.Dispatchers.Unconfined
        override val io = kotlinx.coroutines.Dispatchers.Unconfined
        override val default = kotlinx.coroutines.Dispatchers.Unconfined
    }

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        provider = GoogleCalendarProvider(
            context = mockContext,
            tokenProvider = { "fake-token" },
            dispatchers = testDispatchers,
            httpClient = OkHttpClient(),
            baseUrl = mockWebServer.url("/").toString() // Redirect to mock server
        )
    }

    @After
    fun teardown() {
        mockWebServer.shutdown()
    }




    @Test
    fun `getCalendars returns empty list on HTTP error`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(401)) // Unauthorized

        val result = provider.getCalendars()

        assertTrue("Should return empty list on 401", result.isEmpty())
    }



    @Test
    fun `test currentWeekMonday utility`() {
        // This utility is outside the class but in the same file; call it directly
        val monday = currentWeekMonday()
        assert(monday > 0)
    }
    @Test
    fun `getCalendars returns empty if token is null`() = runTest {
        val nullTokenProvider = GoogleCalendarProvider(
            context = mockContext,
            tokenProvider = { null }, // Return null here
            baseUrl = mockWebServer.url("/").toString()
        )

        val result = nullTokenProvider.getCalendars()
        assertTrue(result.isEmpty())
    }


}