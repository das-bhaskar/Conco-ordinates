package com.example.myapplication.logic

import com.example.myapplication.data.poi.POICategory
import com.google.android.gms.maps.model.BitmapDescriptorFactory

fun poiMarkerHue(category: POICategory): Float = when (category) {
    POICategory.ALL -> BitmapDescriptorFactory.HUE_RED
    POICategory.CAFE -> BitmapDescriptorFactory.HUE_ORANGE
    POICategory.RESTAURANT -> BitmapDescriptorFactory.HUE_ROSE
    POICategory.PHARMACY -> BitmapDescriptorFactory.HUE_GREEN
    POICategory.GROCERY -> BitmapDescriptorFactory.HUE_YELLOW
    POICategory.GYM -> BitmapDescriptorFactory.HUE_VIOLET
    POICategory.ATM -> BitmapDescriptorFactory.HUE_AZURE
}
