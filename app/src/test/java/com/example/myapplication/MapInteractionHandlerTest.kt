package com.example.myapplication

import android.content.Context
import android.location.Location
import com.example.myapplication.data.Building
import com.example.myapplication.data.Campus
import com.example.myapplication.data.CampusDataWrapper
import com.example.myapplication.data.CampusRepo
import com.example.myapplication.data.JsonLatLng
import com.example.myapplication.logic.MapInteractionHandler
import com.example.myapplication.logic.SimpleMockRouteProvider
import com.example.myapplication.logic.TrueLocationProvider
import com.example.myapplication.telemetry.CrashReporter
import com.example.myapplication.ui.viewmodel.MapViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.OnSuccessListener
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.InputStream
import org.junit.Assert.assertEquals
import org.mockito.Mockito.doNothing


class MapInteractionHandlerTest {

    private val testBuilding = Building(
        name = "Hall Building",
        code = "H",
        wayID = 123L,
        address = "1455 De Maisonneuve",
        outline = listOf(
            JsonLatLng(45.497, -73.579),
            JsonLatLng(45.498, -73.579),
            JsonLatLng(45.498, -73.578),
            JsonLatLng(45.497, -73.578)
        )
    )

    private val testCampus = Campus(
        name = "SGW",
        center = JsonLatLng(45.497, -73.579),
        buildings = listOf(testBuilding),
        outline = emptyList()
    )

    private lateinit var mapViewModel: MapViewModel
    private lateinit var context: Context

    val buildingCaptor = argumentCaptor<Building>()
    val imgCaptor = argumentCaptor<String>()

    @Before
    fun setup() {
        mapViewModel = mock<MapViewModel>().also {
            whenever(it.currentCampus).thenReturn(testCampus)
            doNothing().whenever(it).handleMapTap(buildingCaptor.capture(), imgCaptor.capture())
        }
        context = mock<Context>()
    }

    @Test
    fun `test processClick inside of a building`(){
        MapInteractionHandler.processClick(LatLng(45.4975, -73.5785), mapViewModel, context)
        assertEquals(testBuilding, buildingCaptor.firstValue)
    }

    @Test
    fun `test processClick outside of a building`(){
        MapInteractionHandler.processClick(LatLng(45.49, -73.57), mapViewModel, context)
        assertEquals(null, buildingCaptor.firstValue)
    }

    @Test
    fun `test handleSearchSelection`(){
        MapInteractionHandler.handleSearchSelection(testBuilding, mapViewModel, context)
        assertEquals(testBuilding, buildingCaptor.firstValue)
    }
}