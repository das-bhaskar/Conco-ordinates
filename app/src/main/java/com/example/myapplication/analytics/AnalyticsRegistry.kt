package com.example.myapplication.analytics

object AnalyticsRegistry {
    @Volatile
    private var provider: AnalyticsProvider = NoOpAnalyticsProvider

    fun setProvider(analyticsProvider: AnalyticsProvider) {
        provider = analyticsProvider
    }

    fun provider(): AnalyticsProvider = provider
}
