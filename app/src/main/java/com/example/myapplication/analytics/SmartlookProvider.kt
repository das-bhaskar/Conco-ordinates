package com.example.myapplication.analytics

import android.content.Context
import android.util.Log
import com.example.myapplication.BuildConfig
import com.example.myapplication.telemetry.CrashReporter
import com.smartlook.android.core.api.Smartlook

class SmartlookProvider : AnalyticsProvider {
    private companion object {
        const val TAG = "Smartlook"
        const val DO_NOT_RECORD_TESTER_ID = "do not record"
    }

    private var initialized: Boolean = false

    override fun initialize(context: Context, projectKey: String) {
        if (projectKey.isBlank()) {
            CrashReporter.log("smartlook_init_skipped_blank_project_key")
            return
        }

        val testerId = BuildConfig.SMARTLOOK_TESTER_ID.trim()
        if (testerId == DO_NOT_RECORD_TESTER_ID) {
            Log.i(TAG, "SMARTLOOK_TESTER_ID is 'do not record', Smartlook will not be started.")
            CrashReporter.log("smartlook_init_skipped_do_not_record")
            return
        }

        runCatching {
            val smartlook = Smartlook.instance

            smartlook.preferences.projectKey = projectKey
            CrashReporter.setKey("smartlook_project_key_present", true)
            CrashReporter.setKey("smartlook_project_key_length", projectKey.length)

            if (testerId.isNotEmpty()) {
                setTesterIdentifier(testerId)
                CrashReporter.setKey("smartlook_tester_id_present", true)
            } else {
                CrashReporter.setKey("smartlook_tester_id_present", false)
            }

            smartlook.start()
            smartlook.trackEvent("smartlook_sdk_started")
            initialized = true
            CrashReporter.log("smartlook_initialized")
        }.onFailure { error ->
            CrashReporter.recordNonFatal(error, "smartlook_init_failed")
        }
    }

    override fun trackNavigationEnter(source: String) {
        trackSafely("smartlook_track_failed") {
            Smartlook.instance.trackEvent("navigation_entered_$source")
        }
    }

    override fun trackScreenView(screenName: String) {
        trackSafely("smartlook_track_screen_failed") {
            Smartlook.instance.trackNavigationEnter("screen_$screenName")
            Smartlook.instance.trackEvent("screen_view_$screenName")
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

    private inline fun trackSafely(failureKey: String, block: () -> Unit) {
        if (!initialized) return

        runCatching(block).onFailure { error ->
            CrashReporter.recordNonFatal(error, failureKey)
        }
    }
}
