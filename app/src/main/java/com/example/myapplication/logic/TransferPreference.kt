package com.example.myapplication.logic

/**
 * User's preferred method for changing floors in a building.
 *
 * Used by CrossFloorNavigator to filter and rank transfer node candidates.
 * ELEVATOR_ONLY is also the accessible mode — it guarantees no stairs or
 * escalators are included in the route.
 */
enum class TransferPreference(
    val label:    String,
    val icon:     String,
    /** Primary node types to try first, in priority order. */
    val primary:  List<String>,
    /** Fallback node types if no primary transfer pair is found. */
    val fallback: List<String>
) {
    ANY(
        label    = "Any (Shortest)",
        icon     = "🔀",
        primary  = listOf("ELEVATOR", "ESCALATOR", "STAIRCASE"),
        fallback = emptyList()
    ),
    ELEVATOR_ONLY(
        label    = "Elevator",
        icon     = "🛗",
        primary  = listOf("ELEVATOR"),
        fallback = emptyList()
    ),
    ESCALATOR(
        label    = "Escalator",
        icon     = "↗",
        primary  = listOf("ESCALATOR"),
        fallback = listOf("ELEVATOR")   // fall back if no escalator found
    ),
    STAIRS(
        label    = "Stairs",
        icon     = "🪜",
        primary  = listOf("STAIRCASE"),
        fallback = listOf("ELEVATOR")   // fall back if no staircase found
    );
}
