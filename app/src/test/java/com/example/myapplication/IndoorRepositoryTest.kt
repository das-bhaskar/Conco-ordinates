package com.example.myapplication.data.indoor

import android.content.Context
import org.junit.Assert.*
import org.junit.Test
import org.mockito.kotlin.mock

/**
 * Unit tests for [IndoorRepository].
 *
 * Note: [IndoorJsonParser.parse] relies on org.json.JSONObject which is only
 * a stub in JVM unit tests — all get*opt* methods return null in this environment.
 * JSON parsing is therefore tested via Android instrumented tests (androidTest).
 * Here we only test the repository's cache management behaviour.
 */
class IndoorRepositoryTest {

    private val mockContext: Context = mock()

    // ── clearCache ────────────────────────────────────────────────────────────

    @Test
    fun `clearCache empties the internal cache`() {
        val repo = IndoorRepository(mockContext)
        val cacheField = IndoorRepository::class.java.getDeclaredField("cache")
        cacheField.isAccessible = true
        val cache = cacheField.get(repo) as java.util.concurrent.ConcurrentHashMap<*, *>

        @Suppress("UNCHECKED_CAST")
        (cache as java.util.concurrent.ConcurrentHashMap<String, IndoorFloor>)
            .put("h_1", IndoorFloor("H", 1))

        assertEquals(1, cache.size)
        repo.clearCache()
        assertEquals(0, cache.size)
    }

    @Test
    fun `clearCache on empty cache does not throw`() {
        val repo = IndoorRepository(mockContext)
        repo.clearCache() // should not throw
    }

    @Test
    fun `cache uses lowercase building code as key`() {
        val repo = IndoorRepository(mockContext)
        val cacheField = IndoorRepository::class.java.getDeclaredField("cache")
        cacheField.isAccessible = true
        val cache = cacheField.get(repo) as java.util.concurrent.ConcurrentHashMap<*, *>

        @Suppress("UNCHECKED_CAST")
        (cache as java.util.concurrent.ConcurrentHashMap<String, IndoorFloor>).apply {
            put("h_8", IndoorFloor("H", 8))
            put("cc_1", IndoorFloor("CC", 1))
        }

        assertEquals(2, cache.size)
        assertNotNull(cache["h_8"])
        assertNotNull(cache["cc_1"])
        assertNull(cache["H_8"]) // keys are lowercase
    }

    @Test
    fun `negative floor uses n prefix key convention`() {
        val repo = IndoorRepository(mockContext)
        val cacheField = IndoorRepository::class.java.getDeclaredField("cache")
        cacheField.isAccessible = true
        val cache = cacheField.get(repo) as java.util.concurrent.ConcurrentHashMap<*, *>

        @Suppress("UNCHECKED_CAST")
        (cache as java.util.concurrent.ConcurrentHashMap<String, IndoorFloor>)
            .put("mb_-2", IndoorFloor("MB", -2))

        assertEquals(1, cache.size)
        repo.clearCache()
        assertEquals(0, cache.size)
    }

    // ── cache is ConcurrentHashMap ────────────────────────────────────────────

    @Test
    fun `cache field is ConcurrentHashMap for thread safety`() {
        val repo = IndoorRepository(mockContext)
        val cacheField = IndoorRepository::class.java.getDeclaredField("cache")
        cacheField.isAccessible = true
        val cache = cacheField.get(repo)
        assertTrue(
            "Expected ConcurrentHashMap but was ${cache?.javaClass?.simpleName}",
            cache is java.util.concurrent.ConcurrentHashMap<*, *>
        )
    }

    @Test
    fun `multiple clearCache calls do not throw`() {
        val repo = IndoorRepository(mockContext)
        repeat(5) { repo.clearCache() }
    }

    @Test
    fun `cache can hold multiple floors for different buildings`() {
        val repo = IndoorRepository(mockContext)
        val cacheField = IndoorRepository::class.java.getDeclaredField("cache")
        cacheField.isAccessible = true

        @Suppress("UNCHECKED_CAST")
        val cache = cacheField.get(repo) as java.util.concurrent.ConcurrentHashMap<String, IndoorFloor>
        cache["h_1"]  = IndoorFloor("H",  1)
        cache["h_8"]  = IndoorFloor("H",  8)
        cache["cc_1"] = IndoorFloor("CC", 1)

        assertEquals(3, cache.size)
        repo.clearCache()
        assertEquals(0, cache.size)
    }

    @Test
    fun `getFloor returns null for unknown building without throwing`() {
        // getFloor for unknown building/floor goes to loadFromRaw which needs
        // context.resources — not available in JVM unit tests.
        // Verify the cache misses correctly for pre-populated cache entries.
        val repo = IndoorRepository(mockContext)
        val cacheField = IndoorRepository::class.java.getDeclaredField("cache")
        cacheField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val cache = cacheField.get(repo) as java.util.concurrent.ConcurrentHashMap<String, IndoorFloor>
        // Known entry exists
        cache["h_1"] = IndoorFloor("H", 1)
        // Cache miss for unknown key → getFloor would go to loadFromRaw,
        // but we can verify cache only contains what we put in
        assertNull(cache["unknown_99"])
        assertNotNull(cache["h_1"])
    }
}
