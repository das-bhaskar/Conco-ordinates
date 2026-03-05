package com.example.myapplication.logic

enum class TravelMode {
    PUB_TRANSIT,
    MOTORIZED,
    WALK,

    fun displayLabel(): String = when (this) {
        PUB_TRANSIT -> "Public transit"
        MOTORIZED -> "Motorized"
        WALK -> "Walk"
    }
}