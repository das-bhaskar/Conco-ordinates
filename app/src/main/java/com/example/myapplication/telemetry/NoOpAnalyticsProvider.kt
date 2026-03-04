package com.example.myapplication.telemetry

object NoOpAnalyticsProvider : AnalyticsProvider {
    override fun initialize() = Unit
    override fun trackNavigationEnter(name: String) = Unit
    override fun trackNavigationExit(name: String) = Unit
}

