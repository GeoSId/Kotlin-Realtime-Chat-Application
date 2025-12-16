package com.lkps.ctApp.controllers.crash

import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.crashlytics.ktx.setCustomKeys
import com.google.firebase.ktx.Firebase

object CrashlyticsController {
    fun sendException(userId: String, errorCode: Int, exception: String, ex: Exception) {
        val crashlytics = Firebase.crashlytics
        crashlytics.setCustomKeys {
            key("str_key_custom_code", errorCode)
            key("int_key_custom_exception", exception)
        }
        crashlytics.setUserId(userId)
        crashlytics.recordException(ex)
    }
}