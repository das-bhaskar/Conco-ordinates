package com.example.myapplication.data

/**
 * Domain model representing a parsed Concordia room location.
 *
 * Extracted from raw Calendar event location strings like "MB S1.401 SGW"
 * or "Sir George Williams Campus - Hall Building Rm 862".
 *
 * Lives in the data layer so the UI never has to split strings itself.
 */
data class ParsedLocation(
    val buildingCode: String,   // e.g. "H"
    val buildingName: String,   // e.g. "Henry F. Hall"
    val roomCode: String,       // e.g. "820"
    val campus: String          // e.g. "Sir George Williams"
) {
    /** Display string for the room, e.g. "H-820" */
    val roomDisplay: String get() = "$buildingCode-$roomCode"

    /** One-line summary, e.g. "Henry F. Hall · H-820 · SGW" */
    val shortSummary: String get() = "$buildingName · $roomDisplay · $campus"
}

/**
 * Exhaustive result of attempting to resolve an event's location string.
 *
 * The UI uses this instead of a nullable [ParsedLocation] so every case
 * is handled explicitly — Online and TBA classes get appropriate UI
 * treatment instead of silently showing nothing.
 */
sealed class LocationResult {
    /** A recognised Concordia room — navigation is possible. */
    data class Known(val location: ParsedLocation) : LocationResult()

    /** Location string indicates a remote/online class. */
    object Online : LocationResult()

    /** Location string is "TBA" or "TBD" — not yet announced. */
    object TBA : LocationResult()

    /** Blank or unrecognisable location string. */
    object Unknown : LocationResult()
}

// ── Building code → full name map ─────────────────────────────────────────

internal val buildingNames = mapOf(
    "H"  to "Henry F. Hall",
    "MB" to "John Molson Building",
    "EV" to "Engineering & Visual Arts",
    "FG" to "Le Faubourg",
    "GM" to "Guy-De Maisonneuve",
    "GN" to "Grey Nuns",
    "AD" to "Administration",
    "CL" to "CL Building",
    "CC" to "Comm. Studies",
    "SP" to "Science Pavilion",
    "LB" to "McConnell Library",
    "HC" to "Hingston Hall",
    "RF" to "Recreation & Athletics",
    "PY" to "Psychology",
    "SC" to "Science College",
    "SB" to "Science Building",
    "LS" to "Learning Square",
    "VE" to "Visual Arts"
)

/**
 * Parses a raw Concordia location string into a [LocationResult].
 *
 * Handles four cases:
 * - Online / remote keywords  → [LocationResult.Online]
 * - TBA / TBD                 → [LocationResult.TBA]
 * - Recognised room format    → [LocationResult.Known]
 * - Blank / unrecognisable    → [LocationResult.Unknown]
 */
fun parseLocation(raw: String): LocationResult {
    val trimmed = raw.trim()
    if (trimmed.isBlank()) return LocationResult.Unknown

    // ── Online detection ──────────────────────────────────────────────────
    val lower = trimmed.lowercase()
    if (lower == "online" || lower == "remote" ||
        lower.startsWith("online") || lower.contains("webex") ||
        lower.contains("zoom") || lower.contains("teams")) {
        return LocationResult.Online
    }

    // ── TBA / TBD detection ───────────────────────────────────────────────
    if (lower == "tba" || lower == "tbd" ||
        lower == "to be announced" || lower == "to be determined") {
        return LocationResult.TBA
    }

    // ── Short pattern: "MB S1.401 SGW" or "H 535 LOY" ────────────────────
    val short = Regex("""^([A-Z]{1,3})\s+([\w.]+)\s+(SGW|LOY|EV)$""").find(trimmed)
    if (short != null) {
        val bCode = short.groupValues[1]
        return LocationResult.Known(
            ParsedLocation(
                buildingCode = bCode,
                buildingName = buildingNames[bCode] ?: bCode,
                roomCode     = short.groupValues[2],
                campus       = if (short.groupValues[3] == "SGW") "Sir George Williams" else "Loyola"
            )
        )
    }

    // ── Long pattern: verbose string like "Hall Building Rm 862" ──────────
    val campus = when {
        trimmed.contains("Sir George", ignoreCase = true) -> "Sir George Williams"
        trimmed.contains("SGW",        ignoreCase = true) -> "Sir George Williams"
        trimmed.contains("Loyola",     ignoreCase = true) -> "Loyola"
        trimmed.contains("LOY",        ignoreCase = true) -> "Loyola"
        else -> ""
    }
    val tokens = trimmed.uppercase().split(Regex("""\s+|-|,"""))
    val bCode  = tokens.firstOrNull { buildingNames.containsKey(it) } ?: ""
    val room   = Regex("""[Rr]m\.?\s*(\w+)""").find(trimmed)?.groupValues?.get(1) ?: ""

    return if (bCode.isNotEmpty()) {
        LocationResult.Known(
            ParsedLocation(
                buildingCode = bCode,
                buildingName = buildingNames[bCode] ?: bCode,
                roomCode     = room,
                campus       = campus
            )
        )
    } else {
        LocationResult.Unknown
    }
}
