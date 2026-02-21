package com.example.myapplication.ui.models

import com.example.myapplication.data.Building

data class BuildingUiState(
    val isVisible: Boolean = false,
    val building: Building? = null,
    val fullAddress: String? = null,
    val imageUrl: String? = null
)