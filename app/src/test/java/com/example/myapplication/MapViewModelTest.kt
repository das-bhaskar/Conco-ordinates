package com.example.myapplication.ui.viewmodel

import android.content.Context
import androidx.compose.runtime.MutableState
import com.example.myapplication.data.Building
import com.example.myapplication.data.Campus
import com.example.myapplication.data.CampusRepo
import com.example.myapplication.data.JsonLatLng
import com.example.myapplication.data.ShuttleRepo
import com.example.myapplication.logic.DefaultShuttleService
import com.example.myapplication.logic.MapInteractionHandler
import com.example.myapplication.logic.MockLocationProvider
import com.example.myapplication.logic.RouteData
import com.example.myapplication.logic.RouteProvider
import com.example.myapplication.logic.SearchResult
import com.example.myapplication.logic.SimpleMockRouteProvider
import com.example.myapplication.ui.models.BuildingUiState
import com.example.myapplication.ui.models.MapUIMode
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.spy
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class MainDispatcherRule(
    val testDispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

class MapViewModelTest {

    private lateinit var viewModel: MapViewModel
    private lateinit var mockProvider: MockLocationProvider
    private lateinit var mockRoute: RouteProvider

    private lateinit var testListPoints: ArrayList<LatLng>

    private lateinit var routeData: RouteData

    private lateinit var context: Context

    private lateinit var mockMapHandler: MapInteractionHandler


    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()


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
        buildings = emptyList(),
        outline = emptyList()
    )

    @Before
    fun setup() {

        // 2. Inject it into the Repo so it's not null anymore
        CampusRepo.setTestCampuses(listOf(testCampus))

        mockProvider = MockLocationProvider()
        mockRoute = mock<RouteProvider>()
        // shuttleService is now required – inject the real default.
        // ShuttleRepo returns empty lists until initialized, which is fine
        // for these non-shuttle tests.
        viewModel = spy(MapViewModel(mockProvider,mockRoute, DefaultShuttleService(ShuttleRepo))
        )

        testListPoints = ArrayList<LatLng>()
        testListPoints.addAll(
            arrayListOf(
                LatLng(45.497640000000004, -73.57866),
                LatLng(45.49754, -73.57845),
                LatLng(45.49736, -73.57812000000001),
                LatLng(45.49678, -73.57689),
                LatLng(45.496550000000006, -73.57645000000001),
                LatLng(45.49588000000001, -73.5771),
                LatLng(45.495020000000004, -73.57799),
                LatLng(45.4945, -73.57858),
                LatLng(45.49443, -73.57846),
                LatLng(45.49441, -73.57849)
            )
        )

        routeData = RouteData(testListPoints, "7 mins", "0.5 km")

        context = mock<Context>()
        mockMapHandler = mock<MapInteractionHandler>()
    }

    @Test
    fun `onCampusSelected updates currentCampus state`() {
        // Now this will not be null!
        viewModel.onCampusSelected("SGW")
        assertEquals("SGW", viewModel.currentCampus?.name)
    }

    @Test
    fun `refreshLocation processes provider update`() {
        val testLocation = LatLng(45.497, -73.579)
        mockProvider.mockedLocation = testLocation

        viewModel.refreshLocation()

        // Assert that the current campus matches the location we just fed it
        assertEquals("SGW", viewModel.currentCampus?.name)
    }

    @Test
    fun `swapLocation with no current location`() {
        doNothing().`when`(viewModel).calculateRoute()

        val startPointT = LatLng(51.5074, -0.1278)
        val endPointT = LatLng(48.8566, 2.3522)
        val buildingT = testBuilding
        val destinationNameT = "Hawaii"
        val startLocationNameT = "Georgia"
        val newState = BuildingUiState(
            startPoint = startPointT,
            endPoint = endPointT,
            building = buildingT,
            destinationName = destinationNameT,
            startLocationName = startLocationNameT,
        )

        val field = viewModel::class.java.getDeclaredField("uiBuildingState\$delegate")
        field.isAccessible = true

        @Suppress("UNCHECKED_CAST")
        val mutableState = field.get(viewModel) as MutableState<BuildingUiState>

        // Set the value
        mutableState.value = newState

        viewModel.swapLocations()

        assertEquals(startLocationNameT, viewModel.uiBuildingState.destinationName)
        assertEquals(destinationNameT, viewModel.uiBuildingState.startLocationName)
        assertEquals(endPointT, viewModel.uiBuildingState.startPoint)
        assertEquals(startPointT, viewModel.uiBuildingState.endPoint)
        assertEquals(null, viewModel.uiBuildingState.building)
        assertEquals(null, viewModel.highlightedBuildingName)
    }

    @Test
    fun `swapLocation with current location`() {
        doNothing().`when`(viewModel).calculateRoute()

        val startPointT = LatLng(51.5074, -0.1278)
        val endPointT = LatLng(48.8566, 2.3522)
        val buildingT = testBuilding
        val destinationNameT = "Hawaii"
        val startLocationNameT = "Georgia"
        val newState = BuildingUiState(
            startPoint = startPointT,
            endPoint = endPointT,
            building = buildingT,
            destinationName = destinationNameT,
            startLocationName = startLocationNameT,
            isStartCurrentLocation = true
        )

        val field = viewModel::class.java.getDeclaredField("uiBuildingState\$delegate")
        field.isAccessible = true

        @Suppress("UNCHECKED_CAST")
        val mutableState = field.get(viewModel) as MutableState<BuildingUiState>

        // Set the value
        mutableState.value = newState

        viewModel.swapLocations()

        assertEquals(startLocationNameT, viewModel.uiBuildingState.destinationName)
        assertEquals(destinationNameT, viewModel.uiBuildingState.startLocationName)
        assertEquals(endPointT, viewModel.uiBuildingState.startPoint)
        assertEquals(startPointT, viewModel.uiBuildingState.endPoint)
        assertEquals(testBuilding, viewModel.uiBuildingState.building)
        assertEquals(testBuilding.name, viewModel.highlightedBuildingName)
    }


    @Test
    fun `calculateRoute with no start point and no lastProcessedLocation`() {
        val newState = BuildingUiState(
            startPoint = null,
            routeDistance = ""
        )

        val field = viewModel::class.java.getDeclaredField("uiBuildingState\$delegate")
        field.isAccessible = true

        @Suppress("UNCHECKED_CAST")
        val mutableState = field.get(viewModel) as MutableState<BuildingUiState>

        // Set the value
        mutableState.value = newState

        viewModel.calculateRoute()
        assertEquals("", viewModel.uiBuildingState.routeDistance)
    }

    @Test
    fun `calculateRoute with no end point and no building`() {
        val newState = BuildingUiState(
            startPoint = LatLng(0.0, 0.0),
            endPoint = null,
            building = null,
            routeDistance = ""
        )

        val field = viewModel::class.java.getDeclaredField("uiBuildingState\$delegate")
        field.isAccessible = true

        @Suppress("UNCHECKED_CAST")
        val mutableState = field.get(viewModel) as MutableState<BuildingUiState>

        mutableState.value = newState

        viewModel.calculateRoute()
        assertEquals("", viewModel.uiBuildingState.routeDistance)
    }

    @Test
    fun `calculateRoute with no routeData`() = runTest {

        val newState = BuildingUiState(
            startPoint = LatLng(45.4973357596697, -73.57894993830904),
            endPoint = null,
            building = null,
            routeDistance = ""
        )

        val field = viewModel::class.java.getDeclaredField("uiBuildingState\$delegate")
        field.isAccessible = true

        @Suppress("UNCHECKED_CAST")
        val mutableState = field.get(viewModel) as MutableState<BuildingUiState>

        mutableState.value = newState

        viewModel.calculateRoute()

        assertEquals("", viewModel.uiBuildingState.routeDistance)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `calculateRoute with existent routeData`() = runTest {
        whenever(mockRoute.getRoute(anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(routeData)
        doReturn(null).whenever(viewModel).buildBounds(any())

        val newState = BuildingUiState(
            startPoint = LatLng(45.4973357596697, -73.57894993830904),
            endPoint = LatLng(45.494220940920705, -73.57817776994307),
            routeDuration = "few"
        )

        val field = viewModel::class.java.getDeclaredField("uiBuildingState\$delegate")
        field.isAccessible = true

        @Suppress("UNCHECKED_CAST")
        val mutableState = field.get(viewModel) as MutableState<BuildingUiState>

        mutableState.value = newState

        viewModel.calculateRoute()

        advanceUntilIdle()

        assertEquals("7 mins", viewModel.uiBuildingState.routeDuration)
        assertEquals("0.5 km", viewModel.uiBuildingState.routeDistance)
        assertEquals(testListPoints, viewModel.uiBuildingState.routePoints)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `calculateRoute with null as routeData`() = runTest {
        whenever(mockRoute.getRoute(anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(null)
        doReturn(null).whenever(viewModel).buildBounds(any())

        val newState = BuildingUiState(
            startPoint = LatLng(45.4973357596697, -73.57894993830904),
            endPoint = LatLng(45.494220940920705, -73.57817776994307),
            routeDuration = "few"
        )

        val field = viewModel::class.java.getDeclaredField("uiBuildingState\$delegate")
        field.isAccessible = true

        @Suppress("UNCHECKED_CAST")
        val mutableState = field.get(viewModel) as MutableState<BuildingUiState>

        mutableState.value = newState

        viewModel.calculateRoute()

        advanceUntilIdle()

        assertEquals("-- min", viewModel.uiBuildingState.routeDuration)
        assertEquals("-- m", viewModel.uiBuildingState.routeDistance)
        assertEquals(emptyList<LatLng>(), viewModel.uiBuildingState.routePoints)
    }

    @Test
    fun `test onSearchQueryChanged main string`() {
        viewModel.onSearchQueryChanged("Hall")
        assertEquals("Hall", viewModel.searchQuery)
    }

    @Test
    fun `test onSearchQueryChanged start string`() {
        viewModel.onSearchQueryChanged("Hall", "start")
        assertEquals("Hall", viewModel.uiBuildingState.startLocationName)
    }

    @Test
    fun `test onSearchQueryChanged dest string`() {
        viewModel.onSearchQueryChanged("Hall", "dest")
        assertEquals("Hall", viewModel.uiBuildingState.destinationName)
    }

    @Test
    fun `handleSearchResult with BuildingResult updates uiBuildingState start`() {
        doNothing().whenever(viewModel).setMapEventWithOffset(any())
        doNothing().whenever(viewModel).calculateRoute()

        val fieldString = viewModel::class.java.getDeclaredField("activeSearchField\$delegate")
        fieldString.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val mutableStateString = fieldString.get(viewModel) as MutableState<String>
        mutableStateString.value = "start"

        val newState = BuildingUiState(
            mode = MapUIMode.DIRECTIONS
        )

        val fieldBuilding = viewModel::class.java.getDeclaredField("uiBuildingState\$delegate")
        fieldBuilding.isAccessible = true

        @Suppress("UNCHECKED_CAST")
        val mutableStatedBuilding = fieldBuilding.get(viewModel) as MutableState<BuildingUiState>

        mutableStatedBuilding.value = newState


        val result = SearchResult.BuildingResult(testBuilding)

        viewModel.handleSearchResult(result, context)

        assertEquals("Hall Building", viewModel.uiBuildingState.startLocationName)
        assertEquals(LatLng(45.4975, -73.57849999999999), viewModel.uiBuildingState.startPoint)
    }

    @Test
    fun `handleSearchResult with BuildingResult updates uiBuildingState destination`() {
        doNothing().whenever(viewModel).setMapEventWithOffset(any())
        doNothing().whenever(viewModel).calculateRoute()

        val newState = BuildingUiState(
            mode = MapUIMode.DIRECTIONS
        )

        val fieldBuilding = viewModel::class.java.getDeclaredField("uiBuildingState\$delegate")
        fieldBuilding.isAccessible = true

        @Suppress("UNCHECKED_CAST")
        val mutableStatedBuilding = fieldBuilding.get(viewModel) as MutableState<BuildingUiState>

        mutableStatedBuilding.value = newState


        val result = SearchResult.BuildingResult(testBuilding)

        viewModel.handleSearchResult(result, context)

        assertEquals("Hall Building", viewModel.uiBuildingState.destinationName)
        assertEquals(LatLng(45.4975, -73.57849999999999), viewModel.uiBuildingState.endPoint)
        assertEquals(testBuilding, viewModel.uiBuildingState.building)
    }

    @Test
    fun `handleSearchResult with BuildingResult preview`() {
        doNothing().whenever(viewModel).setMapEventWithOffset(any())
        doNothing().whenever(viewModel).calculateRoute()

        val newState = BuildingUiState(
            mode = MapUIMode.PREVIEW,
            isVisible = false,
            building = null,
            endPoint = null
        )

        val fieldBuilding = viewModel::class.java.getDeclaredField("uiBuildingState\$delegate")
        fieldBuilding.isAccessible = true

        @Suppress("UNCHECKED_CAST")
        val mutableStatedBuilding = fieldBuilding.get(viewModel) as MutableState<BuildingUiState>

        mutableStatedBuilding.value = newState


        val result = SearchResult.BuildingResult(testBuilding)

        viewModel.handleSearchResult(result, context)

        assertEquals(true, viewModel.uiBuildingState.isVisible)
        assertEquals(testBuilding, viewModel.uiBuildingState.building)
    }

    @Test
    fun `handleSearchResult with Campus updates uiBuildingState `() {
        doNothing().whenever(viewModel).setMapEventWithOffset(any())
        doNothing().whenever(viewModel).calculateRoute()
        doNothing().whenever(viewModel).onCampusSelected(any())

        val newState = BuildingUiState(
            isVisible = true,
            building = testBuilding
        )

        val fieldBuilding = viewModel::class.java.getDeclaredField("uiBuildingState\$delegate")
        fieldBuilding.isAccessible = true

        @Suppress("UNCHECKED_CAST")
        val mutableStatedBuilding = fieldBuilding.get(viewModel) as MutableState<BuildingUiState>

        mutableStatedBuilding.value = newState


        val result = SearchResult.CampusResult(testCampus)

        viewModel.handleSearchResult(result, context)

        assertEquals(false, viewModel.uiBuildingState.isVisible)
        assertEquals(null, viewModel.uiBuildingState.building)
    }


    @Test
    fun `handleSearchResult with Home sets startPoint to home`() {
        val result = SearchResult.Home

        viewModel.handleSearchResult(result, context)

        assertEquals(
            LatLng(45.51723868665001, -73.627297124046),
            viewModel.uiBuildingState.startPoint
        )
    }

    @Test
    fun `handleSearchResult with GoogleResult does not change uiBuildingState`() {
        val result = SearchResult.GoogleResult("Google Name", "1234 Avenue", "123")

        viewModel.handleSearchResult(result, context)

        assertEquals(emptyList<SearchResult>(), viewModel.searchResults)
    }

    @Test
    fun `onDirectionsRequest check if it changes values`() {
        val newState = BuildingUiState(
            building = testBuilding,
            mode = MapUIMode.PREVIEW
        )

        val fieldBuilding = viewModel::class.java.getDeclaredField("uiBuildingState\$delegate")
        fieldBuilding.isAccessible = true

        @Suppress("UNCHECKED_CAST")
        val mutableStatedBuilding = fieldBuilding.get(viewModel) as MutableState<BuildingUiState>

        mutableStatedBuilding.value = newState
        viewModel.onDirectionsRequested()

        assertEquals(MapUIMode.DIRECTIONS, viewModel.uiBuildingState.mode)
        assertEquals("Hall Building", viewModel.uiBuildingState.destinationName)
    }

    @Test
    fun `onBackToPreview check if it changes values`() {
        val newState = BuildingUiState(
            mode = MapUIMode.DIRECTIONS
        )

        val fieldBuilding = viewModel::class.java.getDeclaredField("uiBuildingState\$delegate")
        fieldBuilding.isAccessible = true

        @Suppress("UNCHECKED_CAST")
        val mutableStatedBuilding = fieldBuilding.get(viewModel) as MutableState<BuildingUiState>

        mutableStatedBuilding.value = newState
        viewModel.onBackToPreview()

        assertEquals(MapUIMode.PREVIEW, viewModel.uiBuildingState.mode)
    }

    @Test
    fun `onStartQueryChanged check if it changes values`() {
        val newState = BuildingUiState(
            startLocationName = "N/A"
        )

        val fieldBuilding = viewModel::class.java.getDeclaredField("uiBuildingState\$delegate")
        fieldBuilding.isAccessible = true

        @Suppress("UNCHECKED_CAST")
        val mutableStatedBuilding = fieldBuilding.get(viewModel) as MutableState<BuildingUiState>

        mutableStatedBuilding.value = newState
        viewModel.onStartQueryChanged("Hall Building")

        assertEquals("Hall Building", viewModel.uiBuildingState.startLocationName)
    }

    @Test
    fun `onDestinationQueryChanged check if it changes values`() {
        val newState = BuildingUiState(
           destinationName = "N/A"
        )

        val fieldBuilding = viewModel::class.java.getDeclaredField("uiBuildingState\$delegate")
        fieldBuilding.isAccessible = true

        @Suppress("UNCHECKED_CAST")
        val mutableStatedBuilding = fieldBuilding.get(viewModel) as MutableState<BuildingUiState>

        mutableStatedBuilding.value = newState
        viewModel.onDestinationQueryChanged("Hall Building")

        assertEquals("Hall Building", viewModel.uiBuildingState.destinationName)
    }

    @Test
    fun `onTransportQueryChanged check if it changes values`() {
        doNothing().whenever(viewModel).calculateRoute()
        val newState = BuildingUiState(
            selectedTransportMode = "N/A"
        )

        val fieldBuilding = viewModel::class.java.getDeclaredField("uiBuildingState\$delegate")
        fieldBuilding.isAccessible = true

        @Suppress("UNCHECKED_CAST")
        val mutableStatedBuilding = fieldBuilding.get(viewModel) as MutableState<BuildingUiState>

        mutableStatedBuilding.value = newState
        viewModel.onTransportModeChanged("transit")

        assertEquals("transit", viewModel.uiBuildingState.selectedTransportMode)
    }


}