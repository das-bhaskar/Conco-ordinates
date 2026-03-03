package com.example.myapplication

import android.app.Application
import android.util.Log
import com.smartlook.android.core.api.Smartlook

class MyCustomApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.SMARTLOOK_PROJECT_KEY.isBlank()) {
            Log.w("Smartlook", "SMARTLOOK_PROJECT_KEY is empty, Smartlook is not started.")
            return
        }

        val smartlook = Smartlook.instance
        smartlook.preferences.projectKey = BuildConfig.SMARTLOOK_PROJECT_KEY
        smartlook.start()
    }
}
