package com.example.myapplication.logic

import com.example.myapplication.data.Building
import com.example.myapplication.data.indoor.BuildingEntrance
import com.example.myapplication.data.indoor.BuildingEntrances
import com.example.myapplication.ui.models.IndoorJourneyPhase
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil

/**
 * Pure logic for advancing the indoor journey state machine.
 *
 * All functions that need building data accept [allBuildings] as a required
 * parameter — no default value calling CampusRepo. This keeps functions pure
 * and testable without needing CampusRepo initialised with an Android Context.
 *
 * The caller (MapViewModel) is responsible for supplying the list from
 * CampusRepo.getAllBuildings(), which it already holds cached.
 */
object IndoorJourneyHandler {

    fun onDestinationSelected(
        destination:  SearchResult.IndoorRoomResult,
        userGps:      LatLng?,
        allBuildings: List<Building>               // no default — caller must provide
    ): IndoorJourneyPhase {
        if (userGps == null) return IndoorJourneyPhase.DetectingLocation

        val currentBuilding = allBuildings.firstOrNull { building ->
            PolyUtil.containsLocation(userGps, building.getGoogleOutline(), false)
        }

        return if (currentBuilding != null) {
            IndoorJourneyPhase.AskCurrentRoom(currentBuilding, destination)
        } else {
            val destEntrances = BuildingEntrances.forBuilding(destination.buildingCode)
            val bestEntry     = destEntrances.firstOrNull()
            if (bestEntry != null) {
                IndoorJourneyPhase.Outdoor(
                    origin      = userGps,
                    destination = bestEntry.gps,
                    destRoom    = destination
                )
            } else {
                IndoorJourneyPhase.Idle
            }
        }
    }

    fun onCurrentRoomSelected(
        phase:       IndoorJourneyPhase.AskCurrentRoom,
        startNodeId: String,
        startLabel:  String,
        startFloor:  Int = 1
    ): IndoorJourneyPhase {
        val currentBuildingCode = phase.currentBuilding.code
        val destBuildingCode    = phase.destination.buildingCode

        if (currentBuildingCode.lowercase() == destBuildingCode.lowercase()) {
            return IndoorJourneyPhase.IndoorToDestination(
                buildingCode = destBuildingCode,
                startFloor   = startFloor,
                startNodeId  = startNodeId,
                destination  = phase.destination
            )
        }

        val exits    = BuildingEntrances.forBuilding(currentBuildingCode)
        val exitNode = exits.firstOrNull() ?: return IndoorJourneyPhase.Idle

        return IndoorJourneyPhase.IndoorToExit(
            buildingCode = currentBuildingCode,
            startFloor   = startFloor,
            exitFloor    = exitNode.floor,
            startNodeId  = startNodeId,
            exitNodeId   = exitNode.nodeId,
            destination  = phase.destination
        )
    }

    fun onUserExited(
        phase:   IndoorJourneyPhase.IndoorToExit,
        userGps: LatLng
    ): IndoorJourneyPhase {
        val destEntrances = BuildingEntrances.forBuilding(phase.destination.buildingCode)
        val bestEntry = destEntrances.firstOrNull() ?: return IndoorJourneyPhase.Idle

        return IndoorJourneyPhase.Outdoor(
            origin      = userGps,
            destination = bestEntry.gps,
            destRoom    = phase.destination
        )
    }

    fun onNearDestinationBuilding(
        phase:        IndoorJourneyPhase.Outdoor,
        allBuildings: List<Building>               // no default — caller must provide
    ): IndoorJourneyPhase {
        val destBuilding = allBuildings.firstOrNull { it.code == phase.destRoom.buildingCode }
            ?: return IndoorJourneyPhase.Idle
        val entrances = BuildingEntrances.forBuilding(phase.destRoom.buildingCode)
        return IndoorJourneyPhase.AskEntryPoint(destBuilding, entrances, phase.destRoom)
    }

    fun onEntranceSelected(
        phase:    IndoorJourneyPhase.AskEntryPoint,
        entrance: BuildingEntrance
    ): IndoorJourneyPhase {
        return IndoorJourneyPhase.IndoorToDestination(
            buildingCode = phase.destination.buildingCode,
            startFloor   = entrance.floor,
            startNodeId  = entrance.nodeId,
            destination  = phase.destination
        )
    }

    fun isNearBuilding(
        userGps:      LatLng,
        buildingCode: String,
        allBuildings: List<Building>               // no default — caller must provide
    ): Boolean {
        val building = allBuildings.firstOrNull { it.code == buildingCode } ?: return false
        val center   = building.getCenter()
        return com.google.maps.android.SphericalUtil
            .computeDistanceBetween(userGps, center) < 60.0
    }
}
