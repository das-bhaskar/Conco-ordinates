package com.example.myapplication.telemetry

interface AnalyticsProvider {
    fun initialize()
    fun trackNavigationEnter(name: String)
    fun trackNavigationExit(name: String)
}

