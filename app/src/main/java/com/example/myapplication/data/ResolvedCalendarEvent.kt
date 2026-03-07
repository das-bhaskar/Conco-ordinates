package com.example.myapplication.data

/**
 * A [CalendarEvent] with its location already resolved by [LocationResolver].
 *
 * The ViewModel pre-resolves location before exposing events to the UI so
 * that:
 *  - UI composables never call parsing logic (SRP)
 *  - Parsing happens once, not on every recomposition (performance)
 *  - UI is fully stateless regarding business rules (testability)
 *
 * @param event          The original raw calendar event
 * @param locationResult The pre-resolved location — never null, defaults to Unknown
 */
data class ResolvedCalendarEvent(
    val event:          CalendarEvent,
    val locationResult: LocationResult
) {
    // Convenience delegates so call-sites can read event fields directly
    val id:          String  get() = event.id
    val title:       String  get() = event.title
    val location:    String? get() = event.location
    val startTimeMs: Long    get() = event.startTimeMs
    val endTimeMs:   Long    get() = event.endTimeMs
    val calendarId:  String  get() = event.calendarId

    /** Non-null only when a physical room was parsed. */
    val parsedLocation: ParsedLocation?
        get() = (locationResult as? LocationResult.Known)?.location

    /**
     * The building code to pass to [MapViewModel.navigateToBuildingCode], or null
     * if this event has no navigable location.
     *
     * Centralises the (locationResult as? Known)?.buildingCode fallback logic so
     * no UI composable needs to perform conditional casting (PR review: LOGIC LEAK).
     * Falls back to the raw location string for unrecognised rooms so navigation
     * can still attempt a search.
     */
    val destinationBuildingCode: String?
        get() = when (val r = locationResult) {
            is LocationResult.Known   -> r.location.buildingCode.ifBlank { location }
            is LocationResult.Unknown -> location?.takeIf { it.isNotBlank() }
            else                      -> null   // Online / TBA — no map navigation
        }
}
