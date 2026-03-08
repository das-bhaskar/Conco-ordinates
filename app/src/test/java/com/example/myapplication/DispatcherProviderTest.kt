package com.example.myapplication.logic

import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Test

class DefaultDispatcherProviderTest {

    private val provider = DefaultDispatcherProvider()

    @Test
    fun `main returns Dispatchers Main`() {
        assertEquals(Dispatchers.Main, provider.main)
    }

    @Test
    fun `io returns Dispatchers IO`() {
        assertEquals(Dispatchers.IO, provider.io)
    }

    @Test
    fun `default returns Dispatchers Default`() {
        assertEquals(Dispatchers.Default, provider.default)
    }
}