package com.example.myapplication.telemetry

import com.google.firebase.crashlytics.FirebaseCrashlytics

object CrashReporter {
    private val crashlytics: FirebaseCrashlytics by lazy { FirebaseCrashlytics.getInstance() }

    fun log(message: String) {
        crashlytics.log(message)
    }

    fun setKey(key: String, value: String?) {
        crashlytics.setCustomKey(key, value ?: "null")
    }

    fun setKey(key: String, value: Boolean) {
        crashlytics.setCustomKey(key, value)
    }

    fun setKey(key: String, value: Int) {
        crashlytics.setCustomKey(key, value)
    }

    fun setKey(key: String, value: Long) {
        crashlytics.setCustomKey(key, value)
    }

    fun recordNonFatal(throwable: Throwable, reason: String) {
        crashlytics.log("non_fatal_reason=$reason")
        crashlytics.recordException(throwable)
    }
}
