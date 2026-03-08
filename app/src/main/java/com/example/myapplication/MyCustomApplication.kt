package com.example.myapplication

import android.app.Application
import com.example.myapplication.analytics.AnalyticsRegistry
import com.example.myapplication.analytics.SmartlookProvider

class MyCustomApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        val analyticsProvider = SmartlookProvider()
        AnalyticsRegistry.setProvider(analyticsProvider)
        analyticsProvider.initialize(this, BuildConfig.SMARTLOOK_PROJECT_KEY)
    }
}
