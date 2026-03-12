package com.example.myapplication.ui.models

data class NavigationState(
    val isAutoCenterEnabled: Boolean = false,
    val hasArrived: Boolean = false,
    val currentInstruction: String = "Follow the path",
    val currentBearing: Float = 0f
)