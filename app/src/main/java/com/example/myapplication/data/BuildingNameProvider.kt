package com.example.myapplication.data

/**
 * Provides a building's display name given its Concordia building code.
 *
 * Abstracted as an interface so that:
 * - [LocationResolver] depends on a contract, not a concrete class (DIP)
 * - Tests can inject a [FakeBuildingNameProvider] without loading JSON
 * - The single source of truth for building names stays in [CampusRepo],
 *   which already owns that data via campuses.json
 */
fun interface BuildingNameProvider {
    /** Returns the full building name for [code], or null if not found. */
    fun nameForCode(code: String): String?
}

/**
 * Production implementation — delegates to [CampusRepo].
 *
 * Changed from `object` to `class` (PR review) so it can be instantiated and
 * injected via the ViewModel factory, making state-dependent logic unit-testable.
 *
 * [CampusRepo] must already be initialised (via [CampusRepo.initialize])
 * before this provider is created. In practice that happens in Application.onCreate.
 */
class CampusBuildingNameProvider : BuildingNameProvider {
    override fun nameForCode(code: String): String? =
        CampusRepo.getAllCampuses()
            .flatMap { it.buildings }
            .firstOrNull { it.code.equals(code, ignoreCase = true) }
            ?.name
}
