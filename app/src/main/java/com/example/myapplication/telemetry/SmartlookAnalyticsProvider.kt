package com.example.myapplication.telemetry

import android.util.Log
import com.smartlook.android.core.api.Smartlook

class SmartlookAnalyticsProvider(
    private val projectKey: String,
    private val testerId: String
) : AnalyticsProvider {

    private var started = false

    override fun initialize() {
        if (projectKey.isBlank()) {
            Log.w("Smartlook", "SMARTLOOK_PROJECT_KEY is empty, Smartlook is not started.")
            return
        }

        val smartlook = Smartlook.instance
        smartlook.preferences.projectKey = projectKey
        smartlook.start()
        if (testerId.isNotBlank()) {
            smartlook.user.identifier = testerId
        }
        started = true
    }

    override fun trackNavigationEnter(name: String) {
        if (started) {
            Smartlook.instance.trackNavigationEnter(name)
        }
    }

    override fun trackNavigationExit(name: String) {
        if (started) {
            Smartlook.instance.trackNavigationExit(name)
        }
    }
}

