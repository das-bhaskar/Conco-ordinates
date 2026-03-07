package com.example.myapplication.analytics

import android.content.Context

interface AnalyticsProvider {
    fun initialize(context: Context, projectKey: String)
    fun trackNavigationEnter(source: String)
    fun trackScreenView(screenName: String)
}
