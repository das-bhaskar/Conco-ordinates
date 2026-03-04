package com.example.myapplication

import android.text.SpannableString
import com.example.myapplication.data.Building
import com.example.myapplication.data.Campus
import com.example.myapplication.data.CampusRepo
import com.example.myapplication.data.JsonLatLng
import com.example.myapplication.logic.HybridSearchProvider
import com.example.myapplication.logic.SearchResult
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.libraries.places.api.net.PlacesClient
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsResponse
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.mockito.kotlin.*


@OptIn(ExperimentalCoroutinesApi::class)
class HybridSearchProviderTest {

    private lateinit var placesClient: PlacesClient
    private lateinit var campusRepo: CampusRepo
    private lateinit var provider: HybridSearchProvider

    private val googleName = "Google"
    private val googleAddress = "123 Avenue"
    private val googlePlaceID = "1234"

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

    @Before
    fun setup() {
        placesClient = mock()
        campusRepo = mock()
        provider = HybridSearchProvider(placesClient)

        whenever(campusRepo.getAllCampuses()).thenReturn(listOf(testCampus))


        val primaryText: SpannableString = mock { on { toString() } doReturn googleName }
        val secondaryText: SpannableString = mock { on { toString() } doReturn googleAddress }
        val prediction: AutocompletePrediction = mock {
            on { getPrimaryText(null) } doReturn primaryText
            on { getSecondaryText(null) } doReturn secondaryText
            on { placeId } doReturn googlePlaceID
        }

        val response: FindAutocompletePredictionsResponse = mock {
            on { autocompletePredictions } doReturn listOf(prediction)
        }
        val task: Task<FindAutocompletePredictionsResponse> = Tasks.forResult(response)
        whenever(placesClient.findAutocompletePredictions(any())).thenReturn(task)
    }

    @Test
    fun `search returns GoogleResult`() = runTest {

        val results = provider.search("Whatever", campusRepo)

        val googleResult = results.filterIsInstance<SearchResult.GoogleResult>().firstOrNull()

        assert(googleResult != null)
        assert(googleResult?.title == googleName)
        assert(googleResult?.address == googleAddress)
        assert(googleResult?.placeId == googlePlaceID)
    }

    @Test
    fun `search returns BuildingResults by name`() = runTest {

        val results = provider.search("Hall Building", campusRepo)

        val buildingResult = results.filterIsInstance<SearchResult.BuildingResult>().firstOrNull()

        assert(buildingResult != null)
        assert(buildingResult?.building?.name == testBuilding.name)
        assert(buildingResult?.building?.address == testBuilding.address)
        assert(buildingResult?.building?.wayID == testBuilding.wayID)
    }

    @Test
    fun `search returns BuildingResults by address`() = runTest {

        val results = provider.search("1455", campusRepo)

        val buildingResult = results.filterIsInstance<SearchResult.BuildingResult>().firstOrNull()

        assert(buildingResult != null)
        assert(buildingResult?.building?.name == testBuilding.name)
        assert(buildingResult?.building?.address == testBuilding.address)
        assert(buildingResult?.building?.wayID == testBuilding.wayID)
    }

    @Test
    fun `search returns BuildingResults by code`() = runTest {

        val results = provider.search("H", campusRepo)

        val buildingResult = results.filterIsInstance<SearchResult.BuildingResult>().firstOrNull()

        assert(buildingResult != null)
        assert(buildingResult?.building?.name == testBuilding.name)
        assert(buildingResult?.building?.address == testBuilding.address)
        assert(buildingResult?.building?.wayID == testBuilding.wayID)
    }

    @Test
    fun `search returns CampusResults by name`() = runTest {

        val results = provider.search("SGW", campusRepo)

        val campusResult = results.filterIsInstance<SearchResult.CampusResult>().firstOrNull()

        assert(campusResult != null)
        assert(campusResult?.campus?.name == testCampus.name)
        assert(campusResult?.campus?.center == testCampus.center)
    }

    @Test
    fun `search with blank query returns current location and home`() = runTest {
        val results = provider.search("", campusRepo)
        assertEquals(listOf(SearchResult.CurrentLocation, SearchResult.Home), results)
    }


}