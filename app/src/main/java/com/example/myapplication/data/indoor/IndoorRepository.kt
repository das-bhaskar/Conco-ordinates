package com.example.myapplication.data.indoor

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Loads and caches [IndoorFloor] data from raw JSON resources.
 *
 * Parsing is delegated to [IndoorJsonParser] (SRP) — the repository
 * focuses solely on resource loading and in-memory caching.
 *
 * The cache uses [ConcurrentHashMap] to prevent data races when
 * [getFloor] is called concurrently from multiple Dispatchers.IO coroutines.
 *
 * Implements [IIndoorRepository] so that logic-layer classes and ViewModels
 * can depend on the interface rather than this Android-specific class,
 * enabling pure-JVM unit tests with a mock/fake implementation.
 */
class IndoorRepository(
    private val context: Context,
    private val parser:  IndoorJsonParser = IndoorJsonParser()
) : IIndoorRepository {

    private val cache = java.util.concurrent.ConcurrentHashMap<String, IndoorFloor>()

    override suspend fun getFloor(building: String, floor: Int): IndoorFloor? =
        withContext(Dispatchers.IO) {
            val key = "${building.lowercase()}_$floor"
            cache[key] ?: loadFromRaw(building, floor)?.also { cache[key] = it }
        }

    override fun clearCache() = cache.clear()

    // ── private ───────────────────────────────────────────────────────────────

    private fun loadFromRaw(building: String, floor: Int): IndoorFloor? {
        // Android resource names cannot contain '-', so negative floors use 'n' prefix.
        // e.g. floor -2 → "indoor_mb_floorn2"
        val floorSuffix = if (floor < 0) "n${-floor}" else "$floor"
        val resName = "indoor_${building.lowercase()}_floor$floorSuffix"
        val resId   = context.resources.getIdentifier(resName, "raw", context.packageName)
        if (resId == 0) return null
        return try {
            val text = context.resources.openRawResource(resId).bufferedReader().readText()
            parser.parse(JSONObject(text))
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
