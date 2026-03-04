package com.example.myapplication.telemetry

object AnalyticsManager {
    private var provider: AnalyticsProvider = NoOpAnalyticsProvider

    fun setProvider(provider: AnalyticsProvider) {
        this.provider = provider
    }

    fun initialize() {
        provider.initialize()
    }

    fun trackNavigationEnter(name: String) {
        provider.trackNavigationEnter(name)
    }

    fun trackNavigationExit(name: String) {
        provider.trackNavigationExit(name)
    }
}

