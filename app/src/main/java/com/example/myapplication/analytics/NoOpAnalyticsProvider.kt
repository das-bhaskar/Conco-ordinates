package com.example.myapplication.analytics

import android.content.Context

object NoOpAnalyticsProvider : AnalyticsProvider {
    override fun initialize(context: Context, projectKey: String) = Unit
    override fun trackNavigationEnter(source: String) = Unit
}
