package com.example.myapplication.analytics

import android.content.Context
import com.example.myapplication.BuildConfig
import com.smartlook.android.core.api.Smartlook
import com.example.myapplication.telemetry.CrashReporter

class SmartlookProvider : AnalyticsProvider {
    private var initialized: Boolean = false

    override fun initialize(context: Context, projectKey: String) {
        if (projectKey.isBlank()) {
            CrashReporter.log("smartlook_init_skipped_blank_project_key")
            return
        }

        runCatching {
            Smartlook.instance.preferences.projectKey = projectKey
            CrashReporter.setKey("smartlook_project_key_present", true)
            CrashReporter.setKey("smartlook_project_key_length", projectKey.length)
            val testerId = BuildConfig.SMARTLOOK_TESTER_ID.trim()
            if (testerId.isNotEmpty()) {
                setTesterIdentifier(testerId)
                CrashReporter.setKey("smartlook_tester_id_present", true)
            } else {
                CrashReporter.setKey("smartlook_tester_id_present", false)
            }
            Smartlook.instance.start()
            Smartlook.instance.trackEvent("smartlook_sdk_started")
            initialized = true
            CrashReporter.log("smartlook_initialized")
        }.onFailure { error ->
            CrashReporter.recordNonFatal(error, "smartlook_init_failed")
        }
    }

    override fun trackNavigationEnter(source: String) {
        if (!initialized) return

        runCatching {
            Smartlook.instance.trackEvent("navigation_entered_$source")
        }.onFailure { error ->
            CrashReporter.recordNonFatal(error, "smartlook_track_failed")
        }
    }

    override fun trackScreenView(screenName: String) {
        if (!initialized) return

        runCatching {
            Smartlook.instance.trackNavigationEnter("screen_$screenName")
            Smartlook.instance.trackEvent("screen_view_$screenName")
        }.onFailure { error ->
            CrashReporter.recordNonFatal(error, "smartlook_track_screen_failed")
        }
    }

    private fun setTesterIdentifier(testerId: String) {
        runCatching {
            val user = Smartlook.instance.user
            val setter = user::class.java.methods.firstOrNull { method ->
                method.name == "setIdentifier" &&
                    method.parameterTypes.size == 1 &&
                    method.parameterTypes[0] == String::class.java
            } ?: return
            setter.invoke(user, testerId)
        }.onFailure { error ->
            CrashReporter.recordNonFatal(error, "smartlook_set_tester_id_failed")
        }
    }
}
