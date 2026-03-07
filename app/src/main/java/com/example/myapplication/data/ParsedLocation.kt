package com.example.myapplication.data

/**
 * Domain model representing a parsed Concordia room location.
 *
 * Extracted from raw Calendar event location strings like "MB S1.401 SGW"
 * or "Sir George Williams Campus - Hall Building Rm 862".
 *
 * Pure data container — no parsing logic lives here.
 * Use [com.example.myapplication.logic.LocationResolver] to produce instances.
 */
data class ParsedLocation(
    val buildingCode: String,   // e.g. "H"
    val buildingName: String,   // e.g. "Henry F. Hall Building"
    val roomCode:     String,   // e.g. "820"
    val campus:       String    // e.g. "Sir George Williams"
) {
    /** Display string for the room, e.g. "H-820" */
    val roomDisplay: String get() = "$buildingCode-$roomCode"

    /** One-line summary, e.g. "Henry F. Hall Building · H-820 · Sir George Williams" */
    val shortSummary: String get() = "$buildingName · $roomDisplay · $campus"
}

/**
 * Exhaustive result of attempting to resolve an event's location string.
 *
 * The UI switches on this sealed class so every case is handled explicitly —
 * Online and TBA classes get appropriate UI treatment instead of silently
 * showing nothing.
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
