package com.example.myapplication.logic

import com.example.myapplication.data.BuildingNameProvider
import com.example.myapplication.data.LocationResult
import com.example.myapplication.data.ParsedLocation

private const val CAMPUS_SGW    = "Sir George Williams"
private const val CAMPUS_LOYOLA = "Loyola"

/**
 * UseCase: resolves a raw Calendar location string into a [LocationResult].
 *
 * Building names are looked up via [BuildingNameProvider] — injected at
 * construction time — so this class never hardcodes campus data that already
 * lives in [CampusRepo] / campuses.json.
 *
 * Default production wiring: [CampusBuildingNameProvider] → [CampusRepo].
 * Tests pass a [FakeBuildingNameProvider] with a small fixture map.
 *
 * Parsing strategy can change (e.g. swap local regex for a remote lookup)
 * without touching any ViewModel or data class.
 */
class LocationResolver(
    private val buildingNames: BuildingNameProvider  // injected at call-site — no default
) {

    /**
     * Parses [rawLocation] into a [LocationResult].
     * Returns [LocationResult.Unknown] for null or blank input.
     */
    fun resolve(rawLocation: String?): LocationResult {
        val raw = rawLocation ?: return LocationResult.Unknown
        return parseLocation(raw)
    }

    /**
     * Convenience — returns a [ParsedLocation] only when a real room was
     * parsed, null otherwise.
     */
    fun parsedLocation(rawLocation: String?): ParsedLocation? =
        (resolve(rawLocation) as? LocationResult.Known)?.location

    // ── Parsing ───────────────────────────────────────────────────────────────

    private fun parseLocation(raw: String): LocationResult {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return LocationResult.Unknown

        val lower = trimmed.lowercase()

        // ── Online detection ──────────────────────────────────────────────────
        if (lower == "online" || lower == "remote" ||
            lower.startsWith("online") || lower.contains("webex") ||
            lower.contains("zoom")     || lower.contains("teams")) {
            return LocationResult.Online
        }

        // ── TBA / TBD detection ───────────────────────────────────────────────
        if (lower == "tba" || lower == "tbd" ||
            lower == "to be announced" || lower == "to be determined") {
            return LocationResult.TBA
        }

        // ── Short pattern: "MB S1.401 SGW"  or  "H 535 LOY" ─────────────────
        val short = Regex("""^([A-Z]{1,3})\s+([\w.]+)\s+(SGW|LOY|EV)$""").find(trimmed)
        if (short != null) {
            val bCode = short.groupValues[1]
            return LocationResult.Known(
                ParsedLocation(
                    buildingCode = bCode,
                    buildingName = buildingNames.nameForCode(bCode) ?: bCode,
                    roomCode     = short.groupValues[2],
                    campus       = if (short.groupValues[3] == "SGW") CAMPUS_SGW else CAMPUS_LOYOLA
                )
            )
        }

        // ── Long pattern: verbose string like "Hall Building Rm 862" ──────────
        val campus = when {
            trimmed.contains("Sir George", ignoreCase = true) -> CAMPUS_SGW
            trimmed.contains("SGW",        ignoreCase = true) -> CAMPUS_SGW
            trimmed.contains(CAMPUS_LOYOLA,ignoreCase = true) -> CAMPUS_LOYOLA
            trimmed.contains("LOY",        ignoreCase = true) -> CAMPUS_LOYOLA
            else                                               -> ""
        }

        val tokens = trimmed.uppercase().split(Regex("""\s+|-|,"""))
        val bCode  = tokens.firstOrNull { buildingNames.nameForCode(it) != null } ?: ""
        val room   = Regex("""[Rr]m\.?\s*(\w+)""").find(trimmed)?.groupValues?.get(1) ?: ""

        return if (bCode.isNotEmpty()) {
            LocationResult.Known(
                ParsedLocation(
                    buildingCode = bCode,
                    buildingName = buildingNames.nameForCode(bCode) ?: bCode,
                    roomCode     = room,
                    campus       = campus
                )
            )
        } else {
            LocationResult.Unknown
        }
    }
}
