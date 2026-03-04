package com.example.myapplication

import android.app.Application
import com.example.myapplication.telemetry.AnalyticsManager
import com.example.myapplication.telemetry.SmartlookAnalyticsProvider

class AnalyticsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AnalyticsManager.setProvider(
            SmartlookAnalyticsProvider(
                projectKey = BuildConfig.SMARTLOOK_PROJECT_KEY,
                testerId = BuildConfig.SMARTLOOK_TESTER_ID
            )
        )
        AnalyticsManager.initialize()
    }
}
